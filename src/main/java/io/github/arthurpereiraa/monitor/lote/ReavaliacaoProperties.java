package io.github.arthurpereiraa.monitor.lote;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do job de reavaliação em lote.
 *
 * @param cron quando o job roda sozinho (padrão: todo dia às 2h, horário de Brasília)
 * @param intervalo pausa entre um fornecedor e outro, para respeitar o limite das APIs públicas
 * @param limiteFalhas quantos fornecedores podem falhar (API fora, CNPJ sumiu) antes de o job desistir
 */
@ConfigurationProperties(prefix = "app.reavaliacao")
public record ReavaliacaoProperties(String cron, Duration intervalo, Integer limiteFalhas) {

    public ReavaliacaoProperties {
        cron = cron == null || cron.isBlank() ? "0 0 2 * * *" : cron;
        intervalo = intervalo == null || intervalo.isNegative() ? Duration.ofMillis(300) : intervalo;
        limiteFalhas = limiteFalhas == null || limiteFalhas < 0 ? 20 : limiteFalhas;
    }
}
