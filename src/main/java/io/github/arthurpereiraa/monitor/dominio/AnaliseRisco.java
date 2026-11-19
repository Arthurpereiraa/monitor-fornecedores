package io.github.arthurpereiraa.monitor.dominio;

import java.util.List;

/** Resultado da análise: nível geral, alertas em ordem de gravidade e recomendações. */
public record AnaliseRisco(NivelRisco nivel, List<Alerta> alertas, Integer idadeMeses, List<String> recomendacoes) {

    public AnaliseRisco {
        alertas = List.copyOf(alertas);
        recomendacoes = List.copyOf(recomendacoes);
    }
}
