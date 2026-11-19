package io.github.arthurpereiraa.monitor.integracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.github.arthurpereiraa.monitor.dominio.Cnpj;
import io.github.arthurpereiraa.monitor.dominio.DadosEmpresa;
import io.github.arthurpereiraa.monitor.dominio.EnderecoCep;
import io.github.arthurpereiraa.monitor.integracao.ResultadoConsulta.FonteDados;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ApisPublicasGatewayTest {

    static final Cnpj CNPJ = Cnpj.de("11.222.333/0001-81");

    static final String EMPRESA_JSON = """
            {
              "cnpj": "11222333000181",
              "razao_social": "EMPRESA EXEMPLO LTDA",
              "nome_fantasia": "",
              "descricao_situacao_cadastral": "ATIVA",
              "data_situacao_cadastral": "2005-03-10",
              "data_inicio_atividade": "2005-03-10",
              "natureza_juridica": "Sociedade Empresária Limitada",
              "porte": "DEMAIS",
              "capital_social": 500000.0,
              "opcao_pelo_mei": false,
              "municipio": "SAO PAULO",
              "uf": "SP",
              "cep": "01310100",
              "email": "contato@exemplo.com.br",
              "ddd_telefone_1": "1133334444",
              "qsa": [{"nome_socio": "FULANO DE TAL", "qualificacao_socio": "Sócio-Administrador"}],
              "campo_que_nao_existe_no_record": 123
            }
            """;

    MockRestServiceServer servidor;
    ApisPublicasGateway gateway;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        gateway = new ApisPublicasGateway(builder,
                new ApisProperties("https://brasil.test/api/", "https://mr.test", "https://viacep.test/ws", Duration.ofSeconds(5)));
    }

    @Test
    @DisplayName("converte a resposta da BrasilAPI para o domínio")
    void brasilApi() {
        servidor.expect(requestTo("https://brasil.test/api/cnpj/v1/11222333000181"))
                .andRespond(withSuccess(EMPRESA_JSON, MediaType.APPLICATION_JSON));

        ResultadoConsulta resultado = gateway.buscarEmpresa(CNPJ);
        DadosEmpresa dados = resultado.dados();

        assertEquals(FonteDados.BRASILAPI, resultado.fonte());
        assertEquals("EMPRESA EXEMPLO LTDA", dados.razaoSocial());
        assertEquals(null, dados.nomeFantasia(), "texto vazio vira null");
        assertEquals(LocalDate.of(2005, 3, 10), dados.dataInicioAtividade());
        assertEquals(0, new BigDecimal("500000").compareTo(dados.capitalSocial()));
        assertEquals("01310100", dados.cep());
        assertEquals(java.util.List.of("FULANO DE TAL"), dados.socios());
        servidor.verify();
    }

    @Test
    @DisplayName("usa a Minha Receita quando a BrasilAPI limita consultas (429)")
    void reservaQuandoLimitado() {
        servidor.expect(requestTo("https://brasil.test/api/cnpj/v1/11222333000181")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        servidor.expect(requestTo("https://mr.test/11222333000181")).andRespond(withSuccess(EMPRESA_JSON, MediaType.APPLICATION_JSON));

        ResultadoConsulta resultado = gateway.buscarEmpresa(CNPJ);

        assertEquals(FonteDados.MINHA_RECEITA, resultado.fonte());
        assertEquals("EMPRESA EXEMPLO LTDA", resultado.dados().razaoSocial());
        servidor.verify();
    }

    @Test
    @DisplayName("404 da BrasilAPI é definitivo: não consulta a reserva")
    void naoEncontradoNaoUsaReserva() {
        servidor.expect(requestTo("https://brasil.test/api/cnpj/v1/11222333000181")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(EmpresaNaoEncontradaException.class, () -> gateway.buscarEmpresa(CNPJ));
        servidor.verify();
    }

    @Test
    @DisplayName("quando as duas fontes falham, lança FonteIndisponivelException")
    void ambasIndisponiveis() {
        servidor.expect(requestTo("https://brasil.test/api/cnpj/v1/11222333000181")).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        servidor.expect(requestTo("https://mr.test/11222333000181")).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        FonteIndisponivelException erro = assertThrows(FonteIndisponivelException.class, () -> gateway.buscarEmpresa(CNPJ));
        assertTrue(erro.getMessage().contains("não responderam"));
        servidor.verify();
    }

    @Test
    @DisplayName("CEP: BrasilAPI primeiro, ViaCEP como reserva, vazio se as duas falharem")
    void cep() {
        servidor.expect(requestTo("https://brasil.test/api/cep/v2/01310100"))
                .andRespond(withSuccess("{\"cep\":\"01310100\",\"city\":\"São Paulo\",\"state\":\"SP\"}", MediaType.APPLICATION_JSON));
        assertEquals(Optional.of(new EnderecoCep("São Paulo", "SP")), gateway.buscarCep("01310-100"));

        servidor.reset();
        servidor.expect(requestTo("https://brasil.test/api/cep/v2/13010000")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        servidor.expect(requestTo("https://viacep.test/ws/13010000/json/"))
                .andRespond(withSuccess("{\"cep\":\"13010-000\",\"localidade\":\"Campinas\",\"uf\":\"SP\"}", MediaType.APPLICATION_JSON));
        assertEquals(Optional.of(new EnderecoCep("Campinas", "SP")), gateway.buscarCep("13010000"));

        servidor.reset();
        servidor.expect(requestTo("https://brasil.test/api/cep/v2/99999999")).andRespond(withStatus(HttpStatus.NOT_FOUND));
        servidor.expect(requestTo("https://viacep.test/ws/99999999/json/"))
                .andRespond(withSuccess("{\"erro\": true}", MediaType.APPLICATION_JSON));
        assertEquals(Optional.empty(), gateway.buscarCep("99999999"));
        servidor.verify();
    }

    @Test
    @DisplayName("CEP com tamanho errado nem chega a consultar")
    void cepInvalido() {
        assertEquals(Optional.empty(), gateway.buscarCep("123"));
        assertEquals(Optional.empty(), gateway.buscarCep(null));
        servidor.verify();
    }
}
