package io.github.arthurpereiraa.monitor.dominio;

import java.util.Objects;

/** Um achado da análise de risco. */
public record Alerta(Nivel nivel, String codigo, String mensagem) {

    public enum Nivel {
        ALTO,
        MEDIO,
        INFO
    }

    public Alerta {
        Objects.requireNonNull(nivel);
        Objects.requireNonNull(codigo);
        Objects.requireNonNull(mensagem);
    }
}
