package io.github.arthurpereiraa.monitor.persistencia;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvaliacaoRepository extends JpaRepository<AvaliacaoEntity, Long> {

    List<AvaliacaoEntity> findByFornecedorIdOrderByAvaliadoEmDescIdDesc(Long fornecedorId);

    Optional<AvaliacaoEntity> findFirstByFornecedorIdOrderByAvaliadoEmDescIdDesc(Long fornecedorId);

    void deleteByFornecedorId(Long fornecedorId);
}
