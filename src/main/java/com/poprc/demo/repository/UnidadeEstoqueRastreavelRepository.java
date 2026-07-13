package com.poprc.demo.repository;

import com.poprc.demo.model.UnidadeEstoqueRastreavel;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnidadeEstoqueRastreavelRepository extends JpaRepository<UnidadeEstoqueRastreavel, Long> {
    boolean existsByCodigoIgnoreCase(String codigo);

    List<UnidadeEstoqueRastreavel> findAllByOrderByDataEntradaDesc();

    List<UnidadeEstoqueRastreavel> findByMaterialIdOrderByDataEntradaDesc(Long materialId);
}
