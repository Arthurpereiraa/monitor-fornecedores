package io.github.arthurpereiraa.monitor.web;

import io.github.arthurpereiraa.monitor.dominio.NivelRisco;
import io.github.arthurpereiraa.monitor.servico.AvaliacaoView;
import io.github.arthurpereiraa.monitor.servico.FornecedorService;
import io.github.arthurpereiraa.monitor.servico.FornecedorView;
import io.github.arthurpereiraa.monitor.servico.PaginaView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Fornecedores", description = "Cadastro, consulta e avaliação de risco de fornecedores")
public class FornecedorController {

    private final FornecedorService service;

    public FornecedorController(FornecedorService service) {
        this.service = service;
    }

    /** Cadastra um fornecedor no monitoramento e faz a primeira avaliação de risco. */
    @Operation(summary = "Cadastra um fornecedor", description = "Valida o CNPJ, consulta os dados públicos e faz a primeira avaliação de risco. Responde 409 se o CNPJ já estiver cadastrado.")
    @PostMapping
    public ResponseEntity<FornecedorView> cadastrar(@Valid @RequestBody CadastroRequest request) {
        FornecedorView criado = service.cadastrar(request.cnpj());
        String id = criado.cnpj().replaceAll("[^0-9A-Z]", "");
        return ResponseEntity.created(URI.create("/api/fornecedores/" + id)).body(criado);
    }

    /** Lista os fornecedores, opcionalmente filtrando pelo nível de risco (ALTO, MEDIO ou BAIXO). */
    @Operation(summary = "Lista os fornecedores", description = "Paginado e ordenado pela razão social. Filtre por nível de risco com o parâmetro nivel.")
    @GetMapping
    public PaginaView<FornecedorView> listar(
            @Parameter(description = "ALTO, MEDIO ou BAIXO") @RequestParam(required = false) NivelRisco nivel,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return service.listar(nivel, pagina, tamanho);
    }

    @Operation(summary = "Detalha um fornecedor", description = "Inclui a última avaliação de risco. Use só os dígitos do CNPJ na URL.")
    @GetMapping("/{cnpj}")
    public FornecedorView buscar(@PathVariable String cnpj) {
        return service.buscar(cnpj);
    }

    /** Histórico de avaliações, da mais recente para a mais antiga. */
    @Operation(summary = "Histórico de avaliações", description = "Da mais recente para a mais antiga.")
    @GetMapping("/{cnpj}/avaliacoes")
    public List<AvaliacaoView> historico(@PathVariable String cnpj) {
        return service.historico(cnpj);
    }

    /** Consulta os dados de novo e registra uma nova avaliação. */
    @Operation(summary = "Reavalia um fornecedor", description = "Consulta os dados de novo, recalcula o risco e informa se ele mudou ou piorou.")
    @PostMapping("/{cnpj}/reavaliacoes")
    public AvaliacaoView reavaliar(@PathVariable String cnpj) {
        return service.reavaliar(cnpj);
    }

    @Operation(summary = "Remove um fornecedor do monitoramento", description = "Apaga também o histórico de avaliações.")
    @DeleteMapping("/{cnpj}")
    public ResponseEntity<Void> remover(@PathVariable String cnpj) {
        service.remover(cnpj);
        return ResponseEntity.noContent().build();
    }
}
