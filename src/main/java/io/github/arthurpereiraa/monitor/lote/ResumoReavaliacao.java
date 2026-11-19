package io.github.arthurpereiraa.monitor.lote;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.batch.core.job.JobExecution;

/**
 * Resumo de uma execução do job de reavaliação.
 *
 * @param execucaoId id da execução nos metadados do Spring Batch
 * @param status status final (COMPLETED, FAILED...)
 * @param lidos fornecedores lidos
 * @param reavaliados avaliações gravadas
 * @param ignorados fornecedores pulados por falha na consulta
 * @param pioraram CNPJs (formatados) cujo risco piorou nesta execução
 */
public record ResumoReavaliacao(Long execucaoId, String status, long lidos, long reavaliados, long ignorados,
        List<String> pioraram) {

    public ResumoReavaliacao {
        pioraram = List.copyOf(pioraram);
    }

    static ResumoReavaliacao de(JobExecution execucao) {
        long lidos = 0;
        long gravados = 0;
        long pulados = 0;
        List<String> pioraram = new ArrayList<>();
        for (var step : execucao.getStepExecutions()) {
            lidos += step.getReadCount();
            gravados += step.getWriteCount();
            pulados += step.getSkipCount();
            String anotados = step.getExecutionContext().getString(ReavaliacaoWriter.CHAVE_PIORARAM, "");
            if (!anotados.isBlank()) {
                pioraram.addAll(Arrays.asList(anotados.split(",")));
            }
        }
        return new ResumoReavaliacao(execucao.getId(), String.valueOf(execucao.getStatus()), lidos, gravados, pulados, pioraram);
    }
}
