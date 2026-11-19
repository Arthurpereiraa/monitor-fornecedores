package io.github.arthurpereiraa.monitor.web;

import io.github.arthurpereiraa.monitor.dominio.NivelRisco;
import io.github.arthurpereiraa.monitor.servico.AvaliacaoView;
import io.github.arthurpereiraa.monitor.servico.FornecedorService;
import io.github.arthurpereiraa.monitor.servico.FornecedorView;
import io.github.arthurpereiraa.monitor.servico.PaginaView;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fornecedores")
public class FornecedorController {

    private final FornecedorService service;

    public FornecedorController(FornecedorService service) {
        this.service = service;
    }

    /** Cadastra um fornecedor no monitoramento e faz a primeira avaliação de risco. */
    @PostMapping
    public ResponseEntity<FornecedorView> cadastrar(@Valid @RequestBody CadastroRequest request) {
        FornecedorView criado = service.cadastrar(request.cnpj());
        String id = criado.cnpj().replaceAll("[^0-9A-Z]", "");
        return ResponseEntity.created(URI.create("/api/fornecedores/" + id)).body(criado);
    }

    /** Lista os fornecedores, opcionalmente filtrando pelo nível de risco (ALTO, MEDIO ou BAIXO). */
    @GetMapping
    public PaginaView<FornecedorView> listar(
            @RequestParam(required = false) NivelRisco nivel,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return service.listar(nivel, pagina, tamanho);
    }

    @GetMapping("/{cnpj}")
    public FornecedorView buscar(@PathVariable String cnpj) {
        return service.buscar(cnpj);
    }

    /** Histórico de avaliações, da mais recente para a mais antiga. */
    @GetMapping("/{cnpj}/avaliacoes")
    public List<AvaliacaoView> historico(@PathVariable String cnpj) {
        return service.historico(cnpj);
    }

    /** Consulta os dados de novo e registra uma nova avaliação. */
    @PostMapping("/{cnpj}/reavaliacoes")
    public AvaliacaoView reavaliar(@PathVariable String cnpj) {
        return service.reavaliar(cnpj);
    }

    @DeleteMapping("/{cnpj}")
    public ResponseEntity<Void> remover(@PathVariable String cnpj) {
        service.remover(cnpj);
        return ResponseEntity.noContent().build();
    }
}
