package io.github.arthurpereiraa.monitor.integracao;

import io.github.arthurpereiraa.monitor.dominio.Cnpj;
import io.github.arthurpereiraa.monitor.dominio.EnderecoCep;
import java.util.Optional;

/** Porta de saída para as fontes de dados públicas. O serviço não sabe de qual API os dados vieram. */
public interface ConsultaEmpresaGateway {

    /**
     * @throws EmpresaNaoEncontradaException se a Receita não tem o CNPJ
     * @throws FonteIndisponivelException se nenhuma fonte respondeu
     */
    ResultadoConsulta buscarEmpresa(Cnpj cnpj);

    /** CEP serve só para cruzar o endereço: se nenhuma fonte responder, devolve vazio. */
    Optional<EnderecoCep> buscarCep(String cep);
}
