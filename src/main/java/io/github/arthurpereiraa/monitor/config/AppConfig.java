package io.github.arthurpereiraa.monitor.config;

import io.github.arthurpereiraa.monitor.integracao.ApisProperties;
import io.github.arthurpereiraa.monitor.integracao.ApisPublicasGateway;
import io.github.arthurpereiraa.monitor.integracao.ConsultaEmpresaGateway;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AppConfig {

    /** Relógio injetável: os testes usam um relógio fixo para as regras que dependem da data. */
    @Bean
    Clock clock() {
        return Clock.system(ZoneId.of("America/Sao_Paulo"));
    }

    @Bean
    ConsultaEmpresaGateway consultaEmpresaGateway(ApisProperties props) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(props.timeout());
        requestFactory.setReadTimeout(props.timeout());
        return new ApisPublicasGateway(RestClient.builder().requestFactory(requestFactory), props);
    }
}
