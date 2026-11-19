package io.github.arthurpereiraa.monitor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.arthurpereiraa.monitor.dominio.DadosEmpresa;
import io.github.arthurpereiraa.monitor.dominio.EnderecoCep;
import io.github.arthurpereiraa.monitor.integracao.ConsultaEmpresaGateway;
import io.github.arthurpereiraa.monitor.integracao.ResultadoConsulta;
import io.github.arthurpereiraa.monitor.integracao.ResultadoConsulta.FonteDados;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Sobe a aplicação inteira (Spring, JPA, Flyway e banco H2) e percorre o fluxo pela API HTTP.
 * Só as APIs públicas são simuladas, para o teste não depender de internet.
 */
@SpringBootTest
class FluxoCompletoTest {

    @Autowired
    WebApplicationContext contexto;

    @MockitoBean
    ConsultaEmpresaGateway gateway;

    MockMvc mvc;

    static DadosEmpresa empresa(String situacao) {
        return new DadosEmpresa("11222333000181", "EMPRESA EXEMPLO LTDA", "EXEMPLO", situacao, LocalDate.of(2005, 3, 10),
                LocalDate.of(2005, 3, 10), "Sociedade Empresária Limitada", "DEMAIS", new BigDecimal("500000"), false, "SAO PAULO",
                "SP", "01310100", "contato@exemplo.com.br", null, List.of("FULANO DE TAL"));
    }

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        when(gateway.buscarCep(anyString())).thenReturn(Optional.of(new EnderecoCep("São Paulo", "SP")));
    }

    @Test
    @DisplayName("cadastrar → listar → reavaliar (risco piora) → histórico → remover")
    void fluxo() throws Exception {
        when(gateway.buscarEmpresa(any())).thenReturn(new ResultadoConsulta(empresa("ATIVA"), FonteDados.BRASILAPI));

        mvc.perform(post("/api/fornecedores").contentType(MediaType.APPLICATION_JSON).content("{\"cnpj\": \"11.222.333/0001-81\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nivelRisco").value("BAIXO"))
                .andExpect(jsonPath("$.ultimaAvaliacao.recomendacoes.length()").value(2));

        mvc.perform(post("/api/fornecedores").contentType(MediaType.APPLICATION_JSON).content("{\"cnpj\": \"11222333000181\"}"))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/fornecedores").param("nivel", "BAIXO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.itens[0].cnpj").value("11.222.333/0001-81"));

        // A empresa foi baixada na Receita: a reavaliação precisa detectar a piora.
        when(gateway.buscarEmpresa(any())).thenReturn(new ResultadoConsulta(empresa("BAIXADA"), FonteDados.MINHA_RECEITA));

        mvc.perform(post("/api/fornecedores/11222333000181/reavaliacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nivel").value("ALTO"))
                .andExpect(jsonPath("$.nivelAnterior").value("BAIXO"))
                .andExpect(jsonPath("$.mudou").value(true))
                .andExpect(jsonPath("$.piorou").value(true))
                .andExpect(jsonPath("$.alertas[0].codigo").value("situacao_irregular"));

        mvc.perform(get("/api/fornecedores/11222333000181"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nivelRisco").value("ALTO"))
                .andExpect(jsonPath("$.situacao").value("BAIXADA"))
                .andExpect(jsonPath("$.fonte").value("Minha Receita"));

        mvc.perform(get("/api/fornecedores/11222333000181/avaliacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nivel").value("ALTO"))
                .andExpect(jsonPath("$[1].nivel").value("BAIXO"));

        mvc.perform(delete("/api/fornecedores/11222333000181")).andExpect(status().isNoContent());
        mvc.perform(get("/api/fornecedores/11222333000181")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CNPJ inválido responde 400 sem consultar as APIs")
    void cnpjInvalido() throws Exception {
        mvc.perform(post("/api/fornecedores").contentType(MediaType.APPLICATION_JSON).content("{\"cnpj\": \"11.222.333/0001-82\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("CNPJ inválido"));
    }
}
