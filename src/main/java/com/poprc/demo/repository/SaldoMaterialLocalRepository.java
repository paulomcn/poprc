package com.poprc.demo.repository;

import com.poprc.demo.model.SaldoMaterialLocal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaldoMaterialLocalRepository extends JpaRepository<SaldoMaterialLocal, Long> {
    Optional<SaldoMaterialLocal> findByMaterialIdAndLocalEstoqueId(Long materialId, Long localEstoqueId);
    List<SaldoMaterialLocal> findByMaterialIdOrderByLocalEstoqueNomeAsc(Long materialId);
    List<SaldoMaterialLocal> findAllByOrderByMaterialNomeAscLocalEstoqueNomeAsc();
    boolean existsByMaterialId(Long materialId);
}
