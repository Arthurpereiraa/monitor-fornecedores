package io.github.arthurpereiraa.monitor.web;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank(message = "informe o usuário") String usuario,
        @NotBlank(message = "informe a senha") String senha) {}
