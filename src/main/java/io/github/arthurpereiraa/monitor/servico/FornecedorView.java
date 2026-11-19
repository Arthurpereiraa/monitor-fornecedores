package io.github.arthurpereiraa.monitor.servico;

import io.github.arthurpereiraa.monitor.dominio.Cnpj;
import io.github.arthurpereiraa.monitor.dominio.NivelRisco;
import io.github.arthurpereiraa.monitor.persistencia.FornecedorEntity;
import java.time.Instant;

/** Fornecedor como a API devolve. {@code ultimaAvaliacao} só vem preenchida na consulta individual. */
public record FornecedorView(
        String cnpj,
        String razaoSocial,
        String nomeFantasia,
        String situacao,
        String municipio,
        String uf,
        NivelRisco nivelRisco,
        String fonte,
        Instant cadastradoEm,
        Instant verificadoEm,
        AvaliacaoView ultimaAvaliacao) {

    static FornecedorView de(FornecedorEntity f, AvaliacaoView ultima) {
        return new FornecedorView(Cnpj.de(f.getCnpj()).formatado(), f.getRazaoSocial(), f.getNomeFantasia(), f.getSituacao(),
                f.getMunicipio(), f.getUf(), f.getNivelRisco(), f.getFonte(), f.getCadastradoEm(), f.getVerificadoEm(), ultima);
    }
}
