package io.github.arthurpereiraa.monitor.lote;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Dispara o job de reavaliação: sozinho pelo agendamento (cron) ou sob demanda pela API. */
@Service
public class ReavaliacaoLoteService {

    private static final Logger log = LoggerFactory.getLogger(ReavaliacaoLoteService.class);

    private final JobOperator jobOperator;
    private final Job reavaliacaoJob;
    private final AtomicBoolean emExecucao = new AtomicBoolean(false);

    public ReavaliacaoLoteService(JobOperator jobOperator, Job reavaliacaoJob) {
        this.jobOperator = jobOperator;
        this.reavaliacaoJob = reavaliacaoJob;
    }

    /** Roda o job e devolve o resumo. Não deixa duas execuções rodarem ao mesmo tempo. */
    public ResumoReavaliacao executar() {
        if (!emExecucao.compareAndSet(false, true)) {
            throw new ReavaliacaoEmAndamentoException();
        }
        try {
            // Cada execução precisa de parâmetros únicos para o Spring Batch criar uma nova instância do job.
            JobParameters parametros = new JobParametersBuilder()
                    .addString("execucao", UUID.randomUUID().toString())
                    .toJobParameters();
            JobExecution execucao = jobOperator.start(reavaliacaoJob, parametros);
            ResumoReavaliacao resumo = ResumoReavaliacao.de(execucao);
            log.info("Reavaliação em lote #{} terminou com {}: {} lidos, {} reavaliados, {} ignorados, {} pioraram",
                    resumo.execucaoId(), resumo.status(), resumo.lidos(), resumo.reavaliados(), resumo.ignorados(),
                    resumo.pioraram().size());
            return resumo;
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível executar a reavaliação em lote", e);
        } finally {
            emExecucao.set(false);
        }
    }

    @Scheduled(cron = "${app.reavaliacao.cron:0 0 2 * * *}", zone = "America/Sao_Paulo")
    void executarAgendado() {
        try {
            executar();
        } catch (RuntimeException e) {
            log.error("Falha na reavaliação agendada", e);
        }
    }
}
