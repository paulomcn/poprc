package com.poprc.demo.repository;

import com.poprc.demo.model.Faturamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List; 

@Repository
public interface FaturamentoRepository extends JpaRepository<Faturamento, Long> {
    
    List<Faturamento> findByContratoId(Long contratoId);
}