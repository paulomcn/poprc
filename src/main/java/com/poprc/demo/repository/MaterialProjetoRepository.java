package com.poprc.demo.repository;

import com.poprc.demo.model.MaterialProjeto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialProjetoRepository extends JpaRepository<MaterialProjeto, Long> {
    List<MaterialProjeto> findByProjetoId(Long projetoId);
}