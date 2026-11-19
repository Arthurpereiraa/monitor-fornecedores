package io.github.arthurpereiraa.monitor.integracao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.arthurpereiraa.monitor.dominio.Cnpj;
import io.github.arthurpereiraa.monitor.dominio.DadosEmpresa;
import io.github.arthurpereiraa.monitor.dominio.EnderecoCep;
import io.github.arthurpereiraa.monitor.integracao.ResultadoConsulta.FonteDados;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Consulta as APIs públicas com fonte reserva:
 * CNPJ na BrasilAPI e, se ela falhar (limite de consultas, instabilidade, rede), na Minha Receita;
 * CEP na BrasilAPI e, se falhar, no ViaCEP.
 * "Não encontrado" (404) é resposta definitiva e não aciona a reserva.
 */
public class ApisPublicasGateway implements ConsultaEmpresaGateway {

    private static final Logger log = LoggerFactory.getLogger(ApisPublicasGateway.class);

    private final RestClient http;
    private final ApisProperties props;

    public ApisPublicasGateway(RestClient.Builder builder, ApisProperties props) {
        this.http = builder.defaultHeader("User-Agent", "monitor-fornecedores").build();
        this.props = props;
    }

    @Override
    public ResultadoConsulta buscarEmpresa(Cnpj cnpj) {
        try {
            return new ResultadoConsulta(consultarCnpj(props.brasilapiUrl() + "/cnpj/v1/{cnpj}", cnpj), FonteDados.BRASILAPI);
        } catch (RestClientException principal) {
            if (naoEncontrado(principal)) {
                throw new EmpresaNaoEncontradaException(cnpj);
            }
            log.warn("BrasilAPI indisponível para {} ({}); tentando a Minha Receita", cnpj, principal.getMessage());
            try {
                return new ResultadoConsulta(consultarCnpj(props.minhaReceitaUrl() + "/{cnpj}", cnpj), FonteDados.MINHA_RECEITA);
            } catch (RestClientException reserva) {
                if (naoEncontrado(reserva)) {
                    throw new EmpresaNaoEncontradaException(cnpj);
                }
                throw new FonteIndisponivelException(reserva);
            }
        }
    }

    @Override
    public Optional<EnderecoCep> buscarCep(String cep) {
        String digitos = cep == null ? "" : cep.replaceAll("\\D", "");
        if (digitos.length() != 8) {
            return Optional.empty();
        }
        try {
            RespostaCepBrasilApi r = http.get().uri(props.brasilapiUrl() + "/cep/v2/{cep}", digitos)
                    .accept(MediaType.APPLICATION_JSON).retrieve().body(RespostaCepBrasilApi.class);
            if (r != null && r.cidade() != null) {
                return Optional.of(new EnderecoCep(r.cidade(), r.uf()));
            }
        } catch (RestClientException e) {
            log.debug("CEP {} indisponível na BrasilAPI: {}", digitos, e.getMessage());
        }
        try {
            RespostaViaCep r = http.get().uri(props.viacepUrl() + "/{cep}/json/", digitos)
                    .accept(MediaType.APPLICATION_JSON).retrieve().body(RespostaViaCep.class);
            if (r != null && !r.comErro() && r.localidade() != null) {
                return Optional.of(new EnderecoCep(r.localidade(), r.uf()));
            }
        } catch (RestClientException e) {
            log.debug("CEP {} indisponível no ViaCEP: {}", digitos, e.getMessage());
        }
        return Optional.empty();
    }

    private DadosEmpresa consultarCnpj(String url, Cnpj cnpj) {
        RespostaCnpjApi resposta = http.get().uri(url, cnpj.valor()).accept(MediaType.APPLICATION_JSON).retrieve()
                .body(RespostaCnpjApi.class);
        if (resposta == null) {
            throw new RestClientException("Resposta vazia de " + url);
        }
        return resposta.paraDominio();
    }

    private static boolean naoEncontrado(RestClientException erro) {
        return erro instanceof RestClientResponseException r && r.getStatusCode().value() == 404;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespostaCepBrasilApi(@JsonProperty("city") String cidade, @JsonProperty("state") String uf) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespostaViaCep(@JsonProperty("localidade") String localidade, @JsonProperty("uf") String uf,
            @JsonProperty("erro") Object erro) {

        boolean comErro() {
            return erro != null && !"false".equalsIgnoreCase(String.valueOf(erro));
        }
    }
}
