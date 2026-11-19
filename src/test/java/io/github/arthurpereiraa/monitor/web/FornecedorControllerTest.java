package io.github.arthurpereiraa.monitor.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.arthurpereiraa.monitor.dominio.Cnpj;
import io.github.arthurpereiraa.monitor.dominio.NivelRisco;
import io.github.arthurpereiraa.monitor.integracao.FonteIndisponivelException;
import io.github.arthurpereiraa.monitor.servico.FornecedorJaCadastradoException;
import io.github.arthurpereiraa.monitor.servico.FornecedorNaoEncontradoException;
import io.github.arthurpereiraa.monitor.servico.FornecedorService;
import io.github.arthurpereiraa.monitor.servico.FornecedorView;
import io.github.arthurpereiraa.monitor.servico.PaginaView;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Testa o contrato HTTP (status, cabeçalhos e corpo) com o serviço simulado. */
class FornecedorControllerTest {

    static final Cnpj CNPJ = Cnpj.de("11222333000181");

    FornecedorService service;
    MockMvc mvc;

    static FornecedorView fornecedor() {
        Instant agora = Instant.parse("2026-11-19T12:00:00Z");
        return new FornecedorView("11.222.333/0001-81", "EMPRESA EXEMPLO LTDA", null, "ATIVA", "SAO PAULO", "SP", NivelRisco.BAIXO,
                "BrasilAPI", agora, agora, null);
    }

    @BeforeEach
    void preparar() {
        service = mock(FornecedorService.class);
        mvc = MockMvcBuilders.standaloneSetup(new FornecedorController(service)).setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test
    @DisplayName("POST cadastra e responde 201 com Location")
    void cadastrar() throws Exception {
        when(service.cadastrar("11.222.333/0001-81")).thenReturn(fornecedor());

        mvc.perform(post("/api/fornecedores").contentType(MediaType.APPLICATION_JSON).content("{\"cnpj\": \"11.222.333/0001-81\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/fornecedores/11222333000181"))
                .andExpect(jsonPath("$.cnpj").value("11.222.333/0001-81"))
                .andExpect(jsonPath("$.nivelRisco").value("BAIXO"));
    }

    @Test
    @DisplayName("POST sem CNPJ responde 400 em Problem Details")
    void cadastrarSemCnpj() throws Exception {
        mvc.perform(post("/api/fornecedores").contentType(MediaType.APPLICATION_JSON).content("{\"cnpj\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"))
                .andExpect(jsonPath("$.detail").value(containsString("informe o CNPJ")));
    }

    @Test
    @DisplayName("POST de CNPJ já cadastrado responde 409")
    void duplicado() throws Exception {
        when(service.cadastrar(any())).thenThrow(new FornecedorJaCadastradoException(CNPJ));

        mvc.perform(post("/api/fornecedores").contentType(MediaType.APPLICATION_JSON).content("{\"cnpj\": \"11222333000181\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("fontes fora do ar respondem 503")
    void fonteIndisponivel() throws Exception {
        when(service.cadastrar(any())).thenThrow(new FonteIndisponivelException(new RuntimeException("429")));

        mvc.perform(post("/api/fornecedores").contentType(MediaType.APPLICATION_JSON).content("{\"cnpj\": \"11222333000181\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value(containsString("não responderam")));
    }

    @Test
    @DisplayName("GET de fornecedor não cadastrado responde 404")
    void naoEncontrado() throws Exception {
        when(service.buscar("11222333000181")).thenThrow(new FornecedorNaoEncontradoException(CNPJ));

        mvc.perform(get("/api/fornecedores/11222333000181"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(containsString("não está cadastrado")));
    }

    @Test
    @DisplayName("GET lista filtra pelo nível de risco e rejeita nível inexistente")
    void listar() throws Exception {
        when(service.listar(eq(NivelRisco.ALTO), anyInt(), anyInt())).thenReturn(new PaginaView<>(List.of(fornecedor()), 0, 20, 1, 1));

        mvc.perform(get("/api/fornecedores").param("nivel", "ALTO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.itens[0].razaoSocial").value("EMPRESA EXEMPLO LTDA"));

        mvc.perform(get("/api/fornecedores").param("nivel", "ALTISSIMO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parâmetro inválido"));
    }

    @Test
    @DisplayName("DELETE responde 204")
    void remover() throws Exception {
        mvc.perform(delete("/api/fornecedores/11222333000181")).andExpect(status().isNoContent());
        verify(service).remover("11222333000181");
    }
}
