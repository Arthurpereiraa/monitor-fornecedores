package io.github.arthurpereiraa.monitor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.arthurpereiraa.monitor.dominio.DadosEmpresa;
import io.github.arthurpereiraa.monitor.dominio.EnderecoCep;
import io.github.arthurpereiraa.monitor.integracao.ConsultaEmpresaGateway;
import io.github.arthurpereiraa.monitor.integracao.ResultadoConsulta;
import io.github.arthurpereiraa.monitor.integracao.ResultadoConsulta.FonteDados;
import io.github.arthurpereiraa.monitor.persistencia.AvaliacaoRepository;
import io.github.arthurpereiraa.monitor.persistencia.FornecedorRepository;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Sobe a aplicação com os filtros do Spring Security e confere quem pode fazer o quê. */
@SpringBootTest
class SegurancaTest {

    static final String CNPJ = "99888777000100";

    @Autowired
    WebApplicationContext contexto;

    @Autowired
    FornecedorRepository fornecedores;

    @Autowired
    AvaliacaoRepository avaliacoes;

    @MockitoBean
    ConsultaEmpresaGateway gateway;

    MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        DadosEmpresa dados = new DadosEmpresa(CNPJ, "EMPRESA SEGURA LTDA", null, "ATIVA", LocalDate.of(2005, 3, 10),
                LocalDate.of(2005, 3, 10), "Sociedade Empresária Limitada", "DEMAIS", new BigDecimal("500000"), false, "SAO PAULO",
                "SP", "01310100", "contato@exemplo.com.br", null, List.of("FULANO DE TAL"));
        when(gateway.buscarEmpresa(any())).thenReturn(new ResultadoConsulta(dados, FonteDados.BRASILAPI));
        when(gateway.buscarCep(anyString())).thenReturn(Optional.of(new EnderecoCep("São Paulo", "SP")));
    }

    @AfterEach
    void limpar() {
        avaliacoes.deleteAll();
        fornecedores.deleteAll();
    }

    String token(String usuario, String senha) throws Exception {
        String corpo = mvc.perform(post("/api/auth/token").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\": \"" + usuario + "\", \"senha\": \"" + senha + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(corpo, "$.token");
    }

    @Test
    @DisplayName("sem token, a API responde 401")
    void semToken() throws Exception {
        mvc.perform(get("/api/fornecedores")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/reavaliacoes")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("token inválido ou senha errada respondem 401")
    void credenciaisInvalidas() throws Exception {
        mvc.perform(get("/api/fornecedores").header(HttpHeaders.AUTHORIZATION, "Bearer token.falso.qualquer"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/auth/token").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\": \"admin\", \"senha\": \"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Não autenticado"));
    }

    @Test
    @DisplayName("analista só lê: GET 200, alterações 403")
    void analistaSoLe() throws Exception {
        String token = token("analista", "analista123");

        mvc.perform(get("/api/fornecedores").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isOk());
        mvc.perform(post("/api/fornecedores").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cnpj\": \"" + CNPJ + "\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/reavaliacoes").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("admin lê e altera")
    void adminAltera() throws Exception {
        String token = token("admin", "admin123");

        mvc.perform(post("/api/fornecedores").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cnpj\": \"" + CNPJ + "\"}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/fornecedores/" + CNPJ).header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isOk());
        mvc.perform(delete("/api/fornecedores/" + CNPJ).header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("health e documentação continuam públicos, e a documentação declara o JWT")
    void rotasPublicas() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes['bearer-jwt'].scheme").value("bearer"));
    }
}
