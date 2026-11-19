package io.github.arthurpereiraa.monitor.persistencia;

import io.github.arthurpereiraa.monitor.dominio.NivelRisco;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FornecedorRepository extends JpaRepository<FornecedorEntity, Long> {

    Optional<FornecedorEntity> findByCnpj(String cnpj);

    boolean existsByCnpj(String cnpj);

    Page<FornecedorEntity> findByNivelRisco(NivelRisco nivelRisco, Pageable pageable);

    /** Só os CNPJs, paginados: é o que o job de reavaliação em lote lê. */
    @Query("select f.cnpj from FornecedorEntity f")
    Page<String> listarCnpjs(Pageable pageable);
}
