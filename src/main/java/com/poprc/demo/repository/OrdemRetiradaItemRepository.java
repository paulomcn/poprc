package com.poprc.demo.repository;

import com.poprc.demo.model.OrdemRetiradaItem;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdemRetiradaItemRepository extends JpaRepository<OrdemRetiradaItem, Long> {
    Optional<OrdemRetiradaItem> findByOrdemRetiradaIdAndMaterialId(
            Long ordemRetiradaId, Long materialId);
}
