package com.poprc.demo.repository;

import com.poprc.demo.model.UnidadeEstoqueRastreavel;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UnidadeEstoqueRastreavelRepository extends JpaRepository<UnidadeEstoqueRastreavel, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UnidadeEstoqueRastreavel u where u.id = :id")
    Optional<UnidadeEstoqueRastreavel> findByIdForUpdate(@Param("id") Long id);

    boolean existsByCodigoIgnoreCase(String codigo);

    List<UnidadeEstoqueRastreavel> findAllByOrderByDataEntradaDesc();

    List<UnidadeEstoqueRastreavel> findByMaterialIdOrderByDataEntradaDesc(Long materialId);
}
