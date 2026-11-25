package io.github.arthurpereiraa.monitor.seguranca;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração de segurança.
 *
 * @param segredoJwt chave que assina os tokens (HS256). Em produção, vem da variável JWT_SEGREDO.
 * @param validadeToken por quanto tempo um token vale
 * @param senhaAdmin senha do usuário "admin" (leitura e escrita)
 * @param senhaAnalista senha do usuário "analista" (só leitura)
 */
@ConfigurationProperties(prefix = "app.seguranca")
public record SegurancaProperties(String segredoJwt, Duration validadeToken, String senhaAdmin, String senhaAnalista) {

    public SegurancaProperties {
        if (segredoJwt == null || segredoJwt.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("app.seguranca.segredo-jwt precisa ter pelo menos 32 caracteres");
        }
        validadeToken = validadeToken == null ? Duration.ofHours(1) : validadeToken;
        if (senhaAdmin == null || senhaAdmin.isBlank() || senhaAnalista == null || senhaAnalista.isBlank()) {
            throw new IllegalArgumentException("Defina app.seguranca.senha-admin e app.seguranca.senha-analista");
        }
    }
}
