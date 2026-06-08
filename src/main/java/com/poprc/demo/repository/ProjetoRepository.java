package com.poprc.demo.repository;

import com.poprc.demo.model.Projeto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProjetoRepository extends JpaRepository<Projeto, Long> {
    
    @Query("SELECT p FROM Projeto p WHERE p.contrato.id = ?1")
    List<Projeto> findByContratoId(Long contratoId);
    
    @Query("SELECT p FROM Projeto p WHERE p.responsavel.id = ?1")
    List<Projeto> findByResponsavelId(Long responsavelId);
}
