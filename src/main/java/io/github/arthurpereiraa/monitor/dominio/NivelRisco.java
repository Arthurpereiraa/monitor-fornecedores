package io.github.arthurpereiraa.monitor.dominio;

/** Nível de risco geral de um fornecedor, do mais grave para o mais leve. */
public enum NivelRisco {
    ALTO("Alto"),
    MEDIO("Médio"),
    BAIXO("Baixo");

    private final String descricao;

    NivelRisco(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }

    /** Verdadeiro se este nível é mais grave que o outro (ALTO é pior que MEDIO, que é pior que BAIXO). */
    public boolean piorQue(NivelRisco outro) {
        return outro != null && ordinal() < outro.ordinal();
    }
}
