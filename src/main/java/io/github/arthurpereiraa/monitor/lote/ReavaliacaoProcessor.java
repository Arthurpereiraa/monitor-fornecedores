package io.github.arthurpereiraa.monitor.lote;

import io.github.arthurpereiraa.monitor.servico.FornecedorService;
import io.github.arthurpereiraa.monitor.servico.NovaAvaliacao;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * Etapa "processar" do job: para cada CNPJ lido, consulta as APIs e calcula o risco. Não grava nada; quem grava é o
 * {@link ReavaliacaoWriter}. Se a consulta falhar, a exceção sobe e o Spring Batch pula esse fornecedor (skip).
 */
@Component
class ReavaliacaoProcessor implements ItemProcessor<String, NovaAvaliacao> {

    private final FornecedorService service;
    private final ReavaliacaoProperties props;

    ReavaliacaoProcessor(FornecedorService service, ReavaliacaoProperties props) {
        this.service = service;
        this.props = props;
    }

    @Override
    public NovaAvaliacao process(String cnpj) throws Exception {
        if (!props.intervalo().isZero()) {
            Thread.sleep(props.intervalo()); // evita estourar o limite de consultas das APIs públicas
        }
        return service.consultar(cnpj);
    }
}
