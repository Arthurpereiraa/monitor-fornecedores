package io.github.arthurpereiraa.monitor.integracao;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Endereços das APIs públicas (configuráveis para testes ou espelhos) e o timeout de cada chamada. */
@ConfigurationProperties(prefix = "app.apis")
public record ApisProperties(String brasilapiUrl, String minhaReceitaUrl, String viacepUrl, Duration timeout) {

    public ApisProperties {
        brasilapiUrl = semBarraFinal(brasilapiUrl, "https://brasilapi.com.br/api");
        minhaReceitaUrl = semBarraFinal(minhaReceitaUrl, "https://minhareceita.org");
        viacepUrl = semBarraFinal(viacepUrl, "https://viacep.com.br/ws");
        timeout = timeout == null ? Duration.ofSeconds(15) : timeout;
    }

    private static String semBarraFinal(String url, String padrao) {
        String valor = url == null || url.isBlank() ? padrao : url;
        return valor.endsWith("/") ? valor.substring(0, valor.length() - 1) : valor;
    }
}
