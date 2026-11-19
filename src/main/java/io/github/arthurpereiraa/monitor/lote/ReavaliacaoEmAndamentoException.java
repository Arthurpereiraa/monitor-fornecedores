package io.github.arthurpereiraa.monitor.lote;

public class ReavaliacaoEmAndamentoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ReavaliacaoEmAndamentoException() {
        super("Já existe uma reavaliação em lote em andamento. Aguarde ela terminar.");
    }
}
