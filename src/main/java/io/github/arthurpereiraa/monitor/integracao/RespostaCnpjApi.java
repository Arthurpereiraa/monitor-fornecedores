package io.github.arthurpereiraa.monitor.integracao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.arthurpereiraa.monitor.dominio.DadosEmpresa;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;

/**
 * Formato JSON da BrasilAPI (/cnpj/v1) e da Minha Receita, que usam os mesmos nomes de campo.
 * Campos numéricos e datas chegam como {@code Object}/{@code String} e são convertidos com tolerância.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record RespostaCnpjApi(
        @JsonProperty("cnpj") String cnpj,
        @JsonProperty("razao_social") String razaoSocial,
        @JsonProperty("nome_fantasia") String nomeFantasia,
        @JsonProperty("descricao_situacao_cadastral") String situacaoCadastral,
        @JsonProperty("data_situacao_cadastral") String dataSituacaoCadastral,
        @JsonProperty("data_inicio_atividade") String dataInicioAtividade,
        @JsonProperty("natureza_juridica") String naturezaJuridica,
        @JsonProperty("porte") String porte,
        @JsonProperty("capital_social") Object capitalSocial,
        @JsonProperty("opcao_pelo_mei") Boolean opcaoPeloMei,
        @JsonProperty("municipio") String municipio,
        @JsonProperty("uf") String uf,
        @JsonProperty("cep") Object cep,
        @JsonProperty("email") String email,
        @JsonProperty("ddd_telefone_1") String telefone,
        @JsonProperty("qsa") List<Socio> qsa) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Socio(@JsonProperty("nome_socio") String nome) {}

    DadosEmpresa paraDominio() {
        List<String> socios = qsa == null ? List.of() : qsa.stream().map(Socio::nome).filter(Objects::nonNull).toList();
        return new DadosEmpresa(cnpj, razaoSocial, vazioComoNulo(nomeFantasia), situacaoCadastral, data(dataSituacaoCadastral),
                data(dataInicioAtividade), naturezaJuridica, porte, decimal(capitalSocial), opcaoPeloMei, municipio, uf,
                cep == null ? null : String.valueOf(cep).replaceAll("\\D", ""), vazioComoNulo(email), vazioComoNulo(telefone),
                socios);
    }

    private static LocalDate data(String texto) {
        if (texto == null || texto.length() < 10) {
            return null;
        }
        try {
            return LocalDate.parse(texto.substring(0, 10));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static BigDecimal decimal(Object valor) {
        if (valor == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(valor).replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String vazioComoNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto;
    }
}
