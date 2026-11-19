package io.github.arthurpereiraa.monitor.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Dados públicos de uma empresa na Receita Federal, já independentes da API de onde vieram.
 * Campos ausentes na fonte ficam {@code null}.
 */
public record DadosEmpresa(
        String cnpj,
        String razaoSocial,
        String nomeFantasia,
        String situacaoCadastral,
        LocalDate dataSituacaoCadastral,
        LocalDate dataInicioAtividade,
        String naturezaJuridica,
        String porte,
        BigDecimal capitalSocial,
        Boolean optanteMei,
        String municipio,
        String uf,
        String cep,
        String email,
        String telefone,
        List<String> socios) {

    public DadosEmpresa {
        socios = socios == null ? List.of() : List.copyOf(socios);
    }
}
