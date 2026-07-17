package com.poprc.demo.repository;

import com.poprc.demo.model.PrestacaoContas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List; 

@Repository
public interface PrestacaoContasRepository extends JpaRepository<PrestacaoContas, Long> {
    
    
    List<PrestacaoContas> findByViagemProjetoId(Long projetoId);

    boolean existsByViagemId(Long viagemId);
}
