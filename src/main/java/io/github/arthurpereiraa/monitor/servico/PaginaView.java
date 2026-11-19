package io.github.arthurpereiraa.monitor.servico;

import java.util.List;
import org.springframework.data.domain.Page;

/** Página de resultados num formato estável (sem expor a classe Page do Spring Data no JSON). */
public record PaginaView<T>(List<T> itens, int pagina, int tamanho, long total, int totalPaginas) {

    static <T> PaginaView<T> de(Page<T> page) {
        return new PaginaView<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
