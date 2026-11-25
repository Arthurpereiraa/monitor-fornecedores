package io.github.arthurpereiraa.monitor.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadados da documentação OpenAPI, exibida no Swagger UI em /swagger-ui.html. Declara o token JWT, para o botão
 * Authorize do Swagger funcionar.
 */
@Configuration
class OpenApiConfig {

    static final String ESQUEMA_JWT = "bearer-jwt";

    @Bean
    OpenAPI documentacao(@Value("${app.versao:dev}") String versao) {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Gere o token em POST /api/auth/token")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT))
                .info(new Info()
                        .title("Monitor de Fornecedores")
                        .version(versao)
                        .description("API que monitora o risco de fornecedores pelo CNPJ, com dados públicos da Receita Federal, "
                                + "histórico de avaliações e reavaliação noturna em lote.")
                        .license(new License().name("MIT")
                                .url("https://github.com/Arthurpereiraa/monitor-fornecedores/blob/main/LICENSE")));
    }
}
