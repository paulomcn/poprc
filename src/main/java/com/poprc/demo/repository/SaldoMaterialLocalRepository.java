package com.poprc.demo.repository;

import com.poprc.demo.model.SaldoMaterialLocal;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaldoMaterialLocalRepository extends JpaRepository<SaldoMaterialLocal, Long> {
    Optional<SaldoMaterialLocal> findByMaterialIdAndLocalEstoqueId(Long materialId, Long localEstoqueId);
    List<SaldoMaterialLocal> findByMaterialIdOrderByLocalEstoqueNomeAsc(Long materialId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SaldoMaterialLocal s where s.id = :id")
    Optional<SaldoMaterialLocal> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SaldoMaterialLocal s where s.material.id = :materialId and s.localEstoque.id = :localId")
    Optional<SaldoMaterialLocal> findByMaterialIdAndLocalEstoqueIdForUpdate(
            @Param("materialId") Long materialId, @Param("localId") Long localId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SaldoMaterialLocal s where s.material.id = :materialId order by s.localEstoque.id")
    List<SaldoMaterialLocal> findByMaterialIdForUpdate(@Param("materialId") Long materialId);
    List<SaldoMaterialLocal> findAllByOrderByMaterialNomeAscLocalEstoqueNomeAsc();
    boolean existsByMaterialId(Long materialId);
}
