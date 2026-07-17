package com.poprc.demo.repository;

import com.poprc.demo.model.ProjetoMembro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjetoMembroRepository extends JpaRepository<ProjetoMembro, Long> {
    List<ProjetoMembro> findByProjetoIdOrderByResponsavelPrincipalDescIdAsc(Long projetoId);
    List<ProjetoMembro> findByFuncionarioId(Long funcionarioId);
    void deleteByProjetoId(Long projetoId);
}
