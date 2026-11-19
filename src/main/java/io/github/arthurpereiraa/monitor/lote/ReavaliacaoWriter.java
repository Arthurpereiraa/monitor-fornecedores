package io.github.arthurpereiraa.monitor.lote;

import io.github.arthurpereiraa.monitor.servico.FornecedorService;
import io.github.arthurpereiraa.monitor.servico.NovaAvaliacao;
import java.util.ArrayList;
import java.util.List;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * Etapa "gravar" do job: registra cada avaliação no histórico e anota quem piorou no contexto da execução do step
 * (que o Spring Batch persiste junto com os metadados do job).
 */
@Component
class ReavaliacaoWriter implements ItemWriter<NovaAvaliacao>, StepExecutionListener {

    static final String CHAVE_PIORARAM = "pioraram";

    private final FornecedorService service;
    private StepExecution execucao;
    private final List<String> pioraram = new ArrayList<>();

    ReavaliacaoWriter(FornecedorService service) {
        this.service = service;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        this.execucao = stepExecution;
        this.pioraram.clear();
    }

    @Override
    public void write(Chunk<? extends NovaAvaliacao> itens) {
        for (NovaAvaliacao nova : itens) {
            service.registrarReavaliacao(nova)
                    .filter(avaliacao -> avaliacao.piorou())
                    .ifPresent(avaliacao -> pioraram.add(nova.cnpj().formatado()));
        }
        if (execucao != null) {
            execucao.getExecutionContext().putString(CHAVE_PIORARAM, String.join(",", pioraram));
        }
    }
}
