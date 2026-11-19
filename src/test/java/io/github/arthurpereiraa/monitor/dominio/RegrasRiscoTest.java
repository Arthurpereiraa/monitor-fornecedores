package io.github.arthurpereiraa.monitor.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RegrasRiscoTest {

    static final LocalDate HOJE = LocalDate.of(2026, 11, 19);

    /** Empresa regular: antiga, ativa, com contato, sócios e endereço consistente. */
    static DadosEmpresa empresa() {
        return new DadosEmpresa("11222333000181", "EMPRESA EXEMPLO LTDA", "EXEMPLO", "ATIVA",
                LocalDate.of(2010, 1, 1), LocalDate.of(2010, 1, 1), "Sociedade Empresária Limitada", "DEMAIS",
                new BigDecimal("100000"), false, "SAO PAULO", "SP", "01310100", "contato@exemplo.com.br", null,
                List.of("FULANO DE TAL"));
    }

    static DadosEmpresa com(String situacao, LocalDate inicio, LocalDate mudanca, Boolean mei, BigDecimal capital,
            String email, List<String> socios) {
        DadosEmpresa e = empresa();
        return new DadosEmpresa(e.cnpj(), e.razaoSocial(), e.nomeFantasia(), situacao, mudanca, inicio, e.naturezaJuridica(),
                e.porte(), capital, mei, e.municipio(), e.uf(), e.cep(), email, null, socios);
    }

    static List<String> codigos(AnaliseRisco analise) {
        return analise.alertas().stream().map(Alerta::codigo).toList();
    }

    @Test
    @DisplayName("empresa regular tem risco baixo, sem alertas e com as 2 recomendações padrão")
    void regular() {
        AnaliseRisco analise = RegrasRisco.analisar(empresa(), new EnderecoCep("São Paulo", "SP"), HOJE);
        assertEquals(NivelRisco.BAIXO, analise.nivel());
        assertTrue(analise.alertas().isEmpty());
        assertEquals(RegrasRisco.RECOMENDACOES_PADRAO, analise.recomendacoes());
        assertEquals(Integer.valueOf(202), analise.idadeMeses());
    }

    @Test
    @DisplayName("situação diferente de ATIVA é risco alto")
    void situacaoIrregular() {
        AnaliseRisco analise = RegrasRisco.analisar(com("BAIXADA", LocalDate.of(2010, 1, 1), LocalDate.of(2010, 1, 1), false,
                new BigDecimal("100000"), "a@b.com", List.of("X")), null, HOJE);
        assertEquals(NivelRisco.ALTO, analise.nivel());
        assertEquals("situacao_irregular", analise.alertas().get(0).codigo());
        assertTrue(analise.recomendacoes().get(0).startsWith("Não formalizar"));
    }

    @Test
    @DisplayName("idade: menos de 6 meses é alto, menos de 2 anos é médio")
    void idade() {
        LocalDate recente = LocalDate.of(2026, 8, 1);
        assertTrue(codigos(RegrasRisco.analisar(com("ATIVA", recente, recente, false, null, "a@b.com", List.of("X")), null, HOJE))
                .contains("empresa_muito_nova"));
        LocalDate umAno = LocalDate.of(2025, 6, 1);
        assertTrue(codigos(RegrasRisco.analisar(com("ATIVA", umAno, umAno, false, null, "a@b.com", List.of("X")), null, HOJE))
                .contains("empresa_nova"));
    }

    @Test
    @DisplayName("mudança de situação nos últimos 90 dias gera alerta médio")
    void situacaoMudou() {
        AnaliseRisco analise = RegrasRisco.analisar(com("ATIVA", LocalDate.of(2010, 1, 1), LocalDate.of(2026, 10, 1), false,
                new BigDecimal("100000"), "a@b.com", List.of("X")), null, HOJE);
        assertEquals(List.of("situacao_mudou"), codigos(analise));
        assertEquals(NivelRisco.MEDIO, analise.nivel());
    }

    @Test
    @DisplayName("MEI e capital baixo para porte DEMAIS")
    void meiECapital() {
        LocalDate antiga = LocalDate.of(2010, 1, 1);
        assertTrue(codigos(RegrasRisco.analisar(com("ATIVA", antiga, antiga, true, new BigDecimal("100000"), "a@b.com", List.of()),
                null, HOJE)).contains("mei"));
        assertTrue(codigos(RegrasRisco.analisar(com("ATIVA", antiga, antiga, false, new BigDecimal("5000"), "a@b.com", List.of("X")),
                null, HOJE)).contains("capital_baixo"));
    }

    @Test
    @DisplayName("CEP de outra cidade gera alerta; a comparação ignora acentos e maiúsculas")
    void endereco() {
        assertFalse(codigos(RegrasRisco.analisar(empresa(), new EnderecoCep("São Paulo", "SP"), HOJE)).contains("endereco_inconsistente"));
        AnaliseRisco analise = RegrasRisco.analisar(empresa(), new EnderecoCep("Campinas", "SP"), HOJE);
        assertEquals(List.of("endereco_inconsistente"), codigos(analise));
        assertTrue(analise.alertas().get(0).mensagem().contains("Campinas/SP"));
    }

    @Test
    @DisplayName("alertas informativos (sem contato, sem sócios) não elevam o risco")
    void informativos() {
        LocalDate antiga = LocalDate.of(2010, 1, 1);
        AnaliseRisco analise = RegrasRisco.analisar(com("ATIVA", antiga, antiga, false, new BigDecimal("100000"), null, List.of()),
                null, HOJE);
        assertEquals(List.of("sem_contato", "sem_socios"), codigos(analise));
        assertEquals(NivelRisco.BAIXO, analise.nivel());
    }

    @Test
    @DisplayName("alertas vêm ordenados por gravidade e as recomendações são no máximo 4")
    void ordemERecomendacoes() {
        LocalDate recente = LocalDate.of(2026, 9, 1);
        AnaliseRisco analise = RegrasRisco.analisar(com("INAPTA", recente, recente, true, null, null, List.of()),
                new EnderecoCep("Campinas", "SP"), HOJE);
        List<Alerta.Nivel> niveis = analise.alertas().stream().map(Alerta::nivel).toList();
        assertEquals(Alerta.Nivel.ALTO, niveis.get(0));
        assertEquals(Alerta.Nivel.INFO, niveis.get(niveis.size() - 1));
        assertEquals(4, analise.recomendacoes().size());
    }

    @Test
    @DisplayName("idade por extenso e comparação de níveis")
    void auxiliares() {
        assertEquals("0 meses", RegrasRisco.idadePorExtenso(0));
        assertEquals("1 ano e 1 mês", RegrasRisco.idadePorExtenso(13));
        assertEquals("2 anos", RegrasRisco.idadePorExtenso(24));
        assertTrue(NivelRisco.ALTO.piorQue(NivelRisco.MEDIO));
        assertFalse(NivelRisco.BAIXO.piorQue(NivelRisco.MEDIO));
    }
}
