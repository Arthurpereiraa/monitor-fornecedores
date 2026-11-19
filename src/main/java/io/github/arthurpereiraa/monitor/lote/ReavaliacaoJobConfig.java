package io.github.arthurpereiraa.monitor.lote;

import io.github.arthurpereiraa.monitor.integracao.EmpresaNaoEncontradaException;
import io.github.arthurpereiraa.monitor.integracao.FonteIndisponivelException;
import io.github.arthurpereiraa.monitor.persistencia.FornecedorRepository;
import io.github.arthurpereiraa.monitor.servico.NovaAvaliacao;
import java.util.Map;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.data.RepositoryItemReader;
import org.springframework.batch.infrastructure.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Job de reavaliação em lote: lê todos os CNPJs monitorados (paginado), consulta e recalcula o risco de cada um e grava
 * uma nova avaliação no histórico. Processa em blocos (chunks) de {@value #TAMANHO_BLOCO}: cada bloco é uma transação.
 */
@Configuration
@EnableScheduling
class ReavaliacaoJobConfig {

    static final String NOME_JOB = "reavaliacaoFornecedores";
    static final int TAMANHO_BLOCO = 10;

    @Bean
    @StepScope
    RepositoryItemReader<String> leitorCnpjs(FornecedorRepository fornecedores) {
        return new RepositoryItemReaderBuilder<String>()
                .name("leitorCnpjs")
                .repository(fornecedores)
                .methodName("listarCnpjs")
                .pageSize(TAMANHO_BLOCO)
                .sorts(Map.of("id", Sort.Direction.ASC))
                .build();
    }

    @Bean
    Step reavaliarFornecedores(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            RepositoryItemReader<String> leitorCnpjs, ReavaliacaoProcessor processor, ReavaliacaoWriter writer,
            ReavaliacaoProperties props) {
        return new StepBuilder("reavaliarFornecedores", jobRepository)
                .<String, NovaAvaliacao>chunk(TAMANHO_BLOCO)
                .transactionManager(transactionManager)
                .reader(leitorCnpjs)
                .processor(processor)
                .writer(writer)
                .listener(writer)
                // Uma API fora do ar ou um CNPJ que sumiu não derruba o job inteiro: o fornecedor é pulado e
                // contado. Só se as falhas passarem do limite o job termina com erro.
                .faultTolerant()
                .skip(FonteIndisponivelException.class)
                .skip(EmpresaNaoEncontradaException.class)
                .skipLimit(props.limiteFalhas())
                .build();
    }

    @Bean
    Job reavaliacaoJob(JobRepository jobRepository, Step reavaliarFornecedores) {
        return new JobBuilder(NOME_JOB, jobRepository).start(reavaliarFornecedores).build();
    }
}
