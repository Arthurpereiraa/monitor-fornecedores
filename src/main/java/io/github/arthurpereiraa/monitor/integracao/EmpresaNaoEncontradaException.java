package io.github.arthurpereiraa.monitor.integracao;

import io.github.arthurpereiraa.monitor.dominio.Cnpj;

public class EmpresaNaoEncontradaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EmpresaNaoEncontradaException(Cnpj cnpj) {
        super("CNPJ " + cnpj.formatado() + " não encontrado na Receita Federal.");
    }
}
