package io.github.arthurpereiraa.monitor.servico;

import io.github.arthurpereiraa.monitor.dominio.Cnpj;

public class FornecedorJaCadastradoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FornecedorJaCadastradoException(Cnpj cnpj) {
        super("Fornecedor " + cnpj.formatado() + " já está cadastrado. Use a reavaliação para atualizar o risco.");
    }
}
