package io.github.arthurpereiraa.monitor.servico;

import io.github.arthurpereiraa.monitor.dominio.Cnpj;

public class FornecedorNaoEncontradoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FornecedorNaoEncontradoException(Cnpj cnpj) {
        super("Fornecedor " + cnpj.formatado() + " não está cadastrado no monitoramento.");
    }
}
