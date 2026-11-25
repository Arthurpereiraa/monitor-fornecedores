package io.github.arthurpereiraa.monitor.web;

import io.github.arthurpereiraa.monitor.seguranca.TokenEmitido;
import io.github.arthurpereiraa.monitor.seguranca.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Login que devolve o token JWT usado nas demais rotas")
class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokens;

    AuthController(AuthenticationManager authenticationManager, TokenService tokens) {
        this.authenticationManager = authenticationManager;
        this.tokens = tokens;
    }

    @Operation(summary = "Gera um token JWT", description = "Usuários de demonstração: admin (leitura e escrita) e analista (só leitura). "
            + "Use o token no botão Authorize ou no cabeçalho Authorization: Bearer <token>.")
    @SecurityRequirements // esta rota não exige token
    @PostMapping("/token")
    TokenEmitido token(@Valid @RequestBody LoginRequest login) {
        Authentication autenticado = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(login.usuario(), login.senha()));
        return tokens.emitir(autenticado);
    }
}
