package com.poprc.demo.repository;

import com.poprc.demo.model.EvidenciaFoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EvidenciaFotoRepository extends JpaRepository<EvidenciaFoto, Long> {
}