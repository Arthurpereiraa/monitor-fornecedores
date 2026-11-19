package io.github.arthurpereiraa.monitor.lote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import io.github.arthurpereiraa.monitor.dominio.Cnpj;
import io.github.arthurpereiraa.monitor.dominio.DadosEmpresa;
import io.github.arthurpereiraa.monitor.dominio.EnderecoCep;
import io.github.arthurpereiraa.monitor.integracao.ConsultaEmpresaGateway;
import io.github.arthurpereiraa.monitor.integracao.FonteIndisponivelException;
import io.github.arthurpereiraa.monitor.integracao.ResultadoConsulta;
import io.github.arthurpereiraa.monitor.integracao.ResultadoConsulta.FonteDados;
import io.github.arthurpereiraa.monitor.persistencia.AvaliacaoRepository;
import io.github.arthurpereiraa.monitor.persistencia.FornecedorRepository;
import io.github.arthurpereiraa.monitor.servico.FornecedorService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Roda o job de verdade (Spring Batch + JPA + H2), só com as APIs públicas simuladas. */
@SpringBootTest
class ReavaliacaoLoteTest {

    static final String VAI_PIORAR = "33444555000181";
    static final String API_FORA = "55666777000181";
    static final String SEM_MUDANCA = "77888999000181";

    @Autowired
    ReavaliacaoLoteService lote;

    @Autowired
    FornecedorService fornecedores;

    @Autowired
    FornecedorRepository fornecedorRepository;

    @Autowired
    AvaliacaoRepository avaliacaoRepository;

    @MockitoBean
    ConsultaEmpresaGateway gateway;

    static ResultadoConsulta empresa(String cnpj, String situacao) {
        DadosEmpresa dados = new DadosEmpresa(cnpj, "EMPRESA " + cnpj, null, situacao, LocalDate.of(2005, 3, 10),
                LocalDate.of(2005, 3, 10), "Sociedade Empresária Limitada", "DEMAIS", new BigDecimal("500000"), false, "SAO PAULO",
                "SP", "01310100", "contato@exemplo.com.br", null, List.of("FULANO DE TAL"));
        return new ResultadoConsulta(dados, FonteDados.BRASILAPI);
    }

    @BeforeEach
    @AfterEach
    void limparBanco() {
        avaliacaoRepository.deleteAll();
        fornecedorRepository.deleteAll();
    }

    @Test
    @DisplayName("reavalia todos, pula quem a API não respondeu e aponta quem piorou")
    void reavaliaTodos() {
        when(gateway.buscarCep(anyString())).thenReturn(Optional.of(new EnderecoCep("São Paulo", "SP")));
        for (String cnpj : List.of(VAI_PIORAR, API_FORA, SEM_MUDANCA)) {
            when(gateway.buscarEmpresa(Cnpj.de(cnpj))).thenReturn(empresa(cnpj, "ATIVA"));
            fornecedores.cadastrar(cnpj);
        }

        // Na noite seguinte: uma empresa foi baixada e a API caiu para outra.
        when(gateway.buscarEmpresa(Cnpj.de(VAI_PIORAR))).thenReturn(empresa(VAI_PIORAR, "BAIXADA"));
        when(gateway.buscarEmpresa(Cnpj.de(API_FORA))).thenThrow(new FonteIndisponivelException(new RuntimeException("503")));

        ResumoReavaliacao resumo = lote.executar();

        assertEquals("COMPLETED", resumo.status(), "falha de uma API não derruba o job");
        assertEquals(3, resumo.lidos());
        assertEquals(2, resumo.reavaliados());
        assertEquals(1, resumo.ignorados());
        assertEquals(List.of(Cnpj.de(VAI_PIORAR).formatado()), resumo.pioraram());

        assertEquals(2, fornecedores.historico(VAI_PIORAR).size());
        assertEquals("ALTO", fornecedores.buscar(VAI_PIORAR).nivelRisco().name());
        assertEquals(1, fornecedores.historico(API_FORA).size(), "quem foi pulado mantém só a avaliação anterior");
        assertEquals(2, fornecedores.historico(SEM_MUDANCA).size());
    }

    @Test
    @DisplayName("sem fornecedores cadastrados, o job termina sem fazer nada")
    void semFornecedores() {
        ResumoReavaliacao resumo = lote.executar();

        assertEquals("COMPLETED", resumo.status());
        assertEquals(0, resumo.lidos());
        assertEquals(List.of(), resumo.pioraram());
    }
}
