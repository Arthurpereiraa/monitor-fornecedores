package io.github.arthurpereiraa.monitor.seguranca;

import java.time.Instant;

/** Resposta do login: o token, como usá-lo e até quando vale. */
public record TokenEmitido(String token, String tipo, Instant expiraEm, String permissoes) {}
