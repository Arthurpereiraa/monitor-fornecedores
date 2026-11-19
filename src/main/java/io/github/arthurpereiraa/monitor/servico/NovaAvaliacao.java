package io.github.arthurpereiraa.monitor.servico;

import io.github.arthurpereiraa.monitor.dominio.AnaliseRisco;
import io.github.arthurpereiraa.monitor.dominio.Cnpj;
import io.github.arthurpereiraa.monitor.integracao.ResultadoConsulta;
import java.util.Objects;

/** Resultado de uma consulta às APIs com o risco já calculado, ainda não gravado no banco. */
public record NovaAvaliacao(Cnpj cnpj, ResultadoConsulta resultado, AnaliseRisco analise) {

    public NovaAvaliacao {
        Objects.requireNonNull(cnpj, "cnpj");
        Objects.requireNonNull(resultado, "resultado");
        Objects.requireNonNull(analise, "analise");
    }
}
