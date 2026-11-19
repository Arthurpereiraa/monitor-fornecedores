package io.github.arthurpereiraa.monitor.web;

import jakarta.validation.constraints.NotBlank;

public record CadastroRequest(@NotBlank(message = "informe o CNPJ") String cnpj) {}
