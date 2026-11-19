package io.github.arthurpereiraa.monitor.servico;

import io.github.arthurpereiraa.monitor.dominio.Alerta;
import io.github.arthurpereiraa.monitor.dominio.NivelRisco;
import io.github.arthurpereiraa.monitor.dominio.RegrasRisco;
import io.github.arthurpereiraa.monitor.persistencia.AvaliacaoEntity;
import java.time.Instant;
import java.util.List;

/**
 * Uma avaliação de risco. {@code mudou} indica que o nível é diferente do da avaliação anterior,
 * e {@code piorou}, que ficou mais grave.
 */
public record AvaliacaoView(
        NivelRisco nivel,
        NivelRisco nivelAnterior,
        boolean mudou,
        boolean piorou,
        String fonte,
        Instant avaliadoEm,
        List<Alerta> alertas,
        List<String> recomendacoes) {

    static AvaliacaoView de(AvaliacaoEntity a) {
        List<Alerta> alertas = a.getAlertas();
        boolean mudou = a.getNivelAnterior() != null && a.getNivelAnterior() != a.getNivelRisco();
        return new AvaliacaoView(a.getNivelRisco(), a.getNivelAnterior(), mudou, a.getNivelRisco().piorQue(a.getNivelAnterior()),
                a.getFonte(), a.getAvaliadoEm(), alertas, RegrasRisco.recomendacoes(alertas));
    }
}
