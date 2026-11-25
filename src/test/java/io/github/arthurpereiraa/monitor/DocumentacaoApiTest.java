package io.github.arthurpereiraa.monitor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.arthurpereiraa.monitor.integracao.ConsultaEmpresaGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Garante que a documentação OpenAPI é gerada e descreve as rotas da API. */
@SpringBootTest
class DocumentacaoApiTest {

    @Autowired
    WebApplicationContext contexto;

    @MockitoBean
    ConsultaEmpresaGateway gateway;

    MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    @Test
    @DisplayName("/v3/api-docs descreve a API com título, versão e rotas")
    void documentacao() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Monitor de Fornecedores"))
                .andExpect(jsonPath("$.paths['/api/fornecedores'].post.summary").value("Cadastra um fornecedor"))
                .andExpect(jsonPath("$.paths['/api/reavaliacoes'].post").exists());
    }
}
