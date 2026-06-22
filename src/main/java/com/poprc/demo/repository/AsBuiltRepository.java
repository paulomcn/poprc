package com.poprc.demo.repository;

import com.poprc.demo.model.AsBuilt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AsBuiltRepository extends JpaRepository<AsBuilt, Long> {
    List<AsBuilt> findByProjetoId(Long projetoId);
}