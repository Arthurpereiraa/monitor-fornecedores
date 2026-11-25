package io.github.arthurpereiraa.monitor.seguranca;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * API sem sessão, protegida por token JWT. O token é emitido em POST /api/auth/token e enviado nas demais chamadas no
 * cabeçalho {@code Authorization: Bearer <token>}.
 *
 * <p>Permissões (viram o claim "scope" do token): {@code leitura} libera os GET; {@code escrita} libera cadastrar,
 * reavaliar, remover e rodar o job em lote.
 */
@Configuration
class SegurancaConfig {

    static final String LEITURA = "leitura";
    static final String ESCRITA = "escrita";

    @Bean
    SecurityFilterChain filtrosDeSeguranca(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()) // API sem cookies de sessão: CSRF não se aplica
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(regras -> regras
                        .requestMatchers(HttpMethod.POST, "/api/auth/token").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/**").hasAuthority("SCOPE_" + LEITURA)
                        .requestMatchers("/api/**").hasAuthority("SCOPE_" + ESCRITA)
                        .anyRequest().authenticated())
                .oauth2ResourceServer(recurso -> recurso.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder(SegurancaProperties props) {
        return NimbusJwtDecoder.withSecretKey(chave(props)).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    JwtEncoder jwtEncoder(SegurancaProperties props) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave(props)));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder(); // bcrypt por padrão
    }

    /** Dois usuários fixos para demonstração; num sistema real viriam do banco ou de um provedor de identidade. */
    @Bean
    UserDetailsService usuarios(SegurancaProperties props, PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("admin").password(encoder.encode(props.senhaAdmin())).authorities(LEITURA, ESCRITA).build(),
                User.withUsername("analista").password(encoder.encode(props.senhaAnalista())).authorities(LEITURA).build());
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService usuarios, PasswordEncoder encoder) {
        DaoAuthenticationProvider provedor = new DaoAuthenticationProvider(usuarios);
        provedor.setPasswordEncoder(encoder);
        return new ProviderManager(provedor);
    }

    private static SecretKey chave(SegurancaProperties props) {
        return new SecretKeySpec(props.segredoJwt().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
