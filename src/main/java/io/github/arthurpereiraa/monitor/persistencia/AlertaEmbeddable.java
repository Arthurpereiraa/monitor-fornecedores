package io.github.arthurpereiraa.monitor.persistencia;

import io.github.arthurpereiraa.monitor.dominio.Alerta;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/** Um alerta guardado junto da avaliação em que apareceu (tabela avaliacao_alerta). */
@Embeddable
public class AlertaEmbeddable {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Alerta.Nivel nivel;

    @Column(nullable = false, length = 40)
    private String codigo;

    @Column(nullable = false, length = 500)
    private String mensagem;

    protected AlertaEmbeddable() {
        // exigido pelo JPA
    }

    public static AlertaEmbeddable de(Alerta alerta) {
        AlertaEmbeddable e = new AlertaEmbeddable();
        e.nivel = alerta.nivel();
        e.codigo = alerta.codigo();
        e.mensagem = alerta.mensagem();
        return e;
    }

    public Alerta paraDominio() {
        return new Alerta(nivel, codigo, mensagem);
    }
}
