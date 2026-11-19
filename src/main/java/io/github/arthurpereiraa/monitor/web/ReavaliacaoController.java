package io.github.arthurpereiraa.monitor.web;

import io.github.arthurpereiraa.monitor.lote.ReavaliacaoLoteService;
import io.github.arthurpereiraa.monitor.lote.ResumoReavaliacao;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Dispara sob demanda o mesmo job que roda toda noite. */
@RestController
@RequestMapping("/api/reavaliacoes")
class ReavaliacaoController {

    private final ReavaliacaoLoteService lote;

    ReavaliacaoController(ReavaliacaoLoteService lote) {
        this.lote = lote;
    }

    @PostMapping
    ResumoReavaliacao reavaliarTodos() {
        return lote.executar();
    }
}
