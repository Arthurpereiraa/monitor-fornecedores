package io.github.arthurpereiraa.monitor.seguranca;

import java.time.Clock;
import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Emite o token JWT de um usuário já autenticado, com as permissões dele no claim "scope". */
@Service
public class TokenService {

    static final String EMISSOR = "monitor-fornecedores";

    private final JwtEncoder encoder;
    private final SegurancaProperties props;
    private final Clock clock;

    public TokenService(JwtEncoder encoder, SegurancaProperties props, Clock clock) {
        this.encoder = encoder;
        this.props = props;
        this.clock = clock;
    }

    public TokenEmitido emitir(Authentication autenticado) {
        Instant agora = clock.instant();
        Instant expiraEm = agora.plus(props.validadeToken());
        String escopo = autenticado.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .collect(Collectors.joining(" "));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(EMISSOR)
                .subject(autenticado.getName())
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .claim("scope", escopo)
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new TokenEmitido(token, "Bearer", expiraEm, escopo);
    }
}
