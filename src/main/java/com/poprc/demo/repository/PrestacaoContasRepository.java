package com.poprc.demo.repository;

import com.poprc.demo.model.PrestacaoContas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrestacaoContasRepository extends JpaRepository<PrestacaoContas, Long> {
}