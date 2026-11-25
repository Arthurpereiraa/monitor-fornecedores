package io.github.arthurpereiraa.monitor.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Metadados da documentação OpenAPI, exibida no Swagger UI em /swagger-ui.html. */
@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI documentacao(@Value("${app.versao:dev}") String versao) {
        return new OpenAPI().info(new Info()
                .title("Monitor de Fornecedores")
                .version(versao)
                .description("API que monitora o risco de fornecedores pelo CNPJ, com dados públicos da Receita Federal, "
                        + "histórico de avaliações e reavaliação noturna em lote.")
                .license(new License().name("MIT").url("https://github.com/Arthurpereiraa/monitor-fornecedores/blob/main/LICENSE")));
    }
}
