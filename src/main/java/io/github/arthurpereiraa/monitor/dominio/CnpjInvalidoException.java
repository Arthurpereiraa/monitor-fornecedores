package io.github.arthurpereiraa.monitor.dominio;

public class CnpjInvalidoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CnpjInvalidoException(String valor) {
        super("CNPJ inválido: \"" + valor + "\". Confira os dígitos verificadores.");
    }
}
