package io.github.arthurpereiraa.monitor.web;

import io.github.arthurpereiraa.monitor.lote.ReavaliacaoLoteService;
import io.github.arthurpereiraa.monitor.lote.ResumoReavaliacao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Dispara sob demanda o mesmo job que roda toda noite. */
@RestController
@RequestMapping("/api/reavaliacoes")
@Tag(name = "Reavaliação em lote", description = "Job do Spring Batch que reavalia todos os fornecedores")
class ReavaliacaoController {

    private final ReavaliacaoLoteService lote;

    ReavaliacaoController(ReavaliacaoLoteService lote) {
        this.lote = lote;
    }

    @Operation(summary = "Reavalia todos os fornecedores agora", description = "Roda o mesmo job agendado para toda noite e devolve o resumo: lidos, reavaliados, ignorados e quem piorou. Responde 409 se já houver uma execução em andamento.")
    @PostMapping
    ResumoReavaliacao reavaliarTodos() {
        return lote.executar();
    }
}
