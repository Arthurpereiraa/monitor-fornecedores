package io.github.arthurpereiraa.monitor.integracao;

public class FonteIndisponivelException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FonteIndisponivelException(Throwable causa) {
        super("As APIs públicas de CNPJ não responderam agora (limite de consultas ou instabilidade). Tente novamente em alguns minutos.",
                causa);
    }
}
