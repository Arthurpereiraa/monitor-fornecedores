package io.github.arthurpereiraa.monitor.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.arthurpereiraa.monitor.dominio.Cnpj;
import io.github.arthurpereiraa.monitor.dominio.CnpjInvalidoException;
import io.github.arthurpereiraa.monitor.dominio.DadosEmpresa;
import io.github.arthurpereiraa.monitor.dominio.EnderecoCep;
import io.github.arthurpereiraa.monitor.dominio.NivelRisco;
import io.github.arthurpereiraa.monitor.integracao.ConsultaEmpresaGateway;
import io.github.arthurpereiraa.monitor.integracao.ResultadoConsulta;
import io.github.arthurpereiraa.monitor.integracao.ResultadoConsulta.FonteDados;
import io.github.arthurpereiraa.monitor.persistencia.AvaliacaoEntity;
import io.github.arthurpereiraa.monitor.persistencia.AvaliacaoRepository;
import io.github.arthurpereiraa.monitor.persistencia.FornecedorEntity;
import io.github.arthurpereiraa.monitor.persistencia.FornecedorRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FornecedorServiceTest {

    static final String CNPJ = "11.222.333/0001-81";
    static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-11-19T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    FornecedorRepository fornecedores;

    @Mock
    AvaliacaoRepository avaliacoes;

    @Mock
    ConsultaEmpresaGateway gateway;

    FornecedorService service;

    static DadosEmpresa empresa(String situacao) {
        return new DadosEmpresa("11222333000181", "EMPRESA EXEMPLO LTDA", null, situacao, LocalDate.of(2005, 3, 10),
                LocalDate.of(2005, 3, 10), "Sociedade Empresária Limitada", "DEMAIS", new BigDecimal("500000"), false, "SAO PAULO",
                "SP", "01310100", "contato@exemplo.com.br", null, List.of("FULANO DE TAL"));
    }

    @BeforeEach
    void preparar() {
        service = new FornecedorService(fornecedores, avaliacoes, gateway, RELOGIO);
        lenient().when(fornecedores.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(avaliacoes.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(gateway.buscarCep("01310100")).thenReturn(Optional.of(new EnderecoCep("São Paulo", "SP")));
    }

    @Test
    @DisplayName("cadastrar consulta os dados, calcula o risco e grava fornecedor e primeira avaliação")
    void cadastrar() {
        when(gateway.buscarEmpresa(Cnpj.de(CNPJ))).thenReturn(new ResultadoConsulta(empresa("ATIVA"), FonteDados.BRASILAPI));

        FornecedorView view = service.cadastrar("11222333000181");

        assertEquals(CNPJ, view.cnpj());
        assertEquals(NivelRisco.BAIXO, view.nivelRisco());
        assertEquals("BrasilAPI", view.fonte());
        assertEquals(Instant.parse("2026-11-19T12:00:00Z"), view.cadastradoEm());
        assertNull(view.ultimaAvaliacao().nivelAnterior());
        assertFalse(view.ultimaAvaliacao().mudou());
        assertEquals(2, view.ultimaAvaliacao().recomendacoes().size());
        verify(fornecedores).save(any(FornecedorEntity.class));
        verify(avaliacoes).save(any(AvaliacaoEntity.class));
    }

    @Test
    @DisplayName("não cadastra o mesmo CNPJ duas vezes nem consulta a API à toa")
    void duplicado() {
        when(fornecedores.existsByCnpj("11222333000181")).thenReturn(true);

        assertThrows(FornecedorJaCadastradoException.class, () -> service.cadastrar(CNPJ));
        verify(gateway, never()).buscarEmpresa(any());
    }

    @Test
    @DisplayName("CNPJ inválido falha antes de qualquer consulta")
    void cnpjInvalido() {
        assertThrows(CnpjInvalidoException.class, () -> service.cadastrar("11.222.333/0001-82"));
        verify(gateway, never()).buscarEmpresa(any());
    }

    @Test
    @DisplayName("reavaliar detecta quando o risco piora e guarda o nível anterior")
    void reavaliarDetectaMudanca() {
        FornecedorEntity existente = new FornecedorEntity("11222333000181", Instant.parse("2026-01-01T00:00:00Z"));
        existente.setNivelRisco(NivelRisco.BAIXO);
        when(fornecedores.findByCnpj("11222333000181")).thenReturn(Optional.of(existente));
        when(gateway.buscarEmpresa(Cnpj.de(CNPJ))).thenReturn(new ResultadoConsulta(empresa("BAIXADA"), FonteDados.MINHA_RECEITA));

        AvaliacaoView avaliacao = service.reavaliar(CNPJ);

        assertEquals(NivelRisco.ALTO, avaliacao.nivel());
        assertEquals(NivelRisco.BAIXO, avaliacao.nivelAnterior());
        assertTrue(avaliacao.mudou());
        assertTrue(avaliacao.piorou());
        assertEquals("Minha Receita", avaliacao.fonte());
        assertEquals("situacao_irregular", avaliacao.alertas().get(0).codigo());
        assertEquals(NivelRisco.ALTO, existente.getNivelRisco(), "o fornecedor passa a refletir a última avaliação");
        assertEquals("BAIXADA", existente.getSituacao());
    }

    @Test
    @DisplayName("reavaliar sem mudança não marca mudou")
    void reavaliarSemMudanca() {
        FornecedorEntity existente = new FornecedorEntity("11222333000181", Instant.parse("2026-01-01T00:00:00Z"));
        existente.setNivelRisco(NivelRisco.BAIXO);
        when(fornecedores.findByCnpj("11222333000181")).thenReturn(Optional.of(existente));
        when(gateway.buscarEmpresa(Cnpj.de(CNPJ))).thenReturn(new ResultadoConsulta(empresa("ATIVA"), FonteDados.BRASILAPI));

        AvaliacaoView avaliacao = service.reavaliar(CNPJ);

        assertFalse(avaliacao.mudou());
        assertFalse(avaliacao.piorou());
    }

    @Test
    @DisplayName("buscar fornecedor não cadastrado lança FornecedorNaoEncontradoException")
    void naoEncontrado() {
        when(fornecedores.findByCnpj("11222333000181")).thenReturn(Optional.empty());

        assertThrows(FornecedorNaoEncontradoException.class, () -> service.buscar(CNPJ));
    }
}
