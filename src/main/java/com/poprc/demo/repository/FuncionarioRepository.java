package com.poprc.demo.repository;

import com.poprc.demo.model.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {
    
    @Query("SELECT f FROM Funcionario f WHERE f.nome = ?1")
    List<Funcionario> findByNome(String nome);
    
    @Query("SELECT f FROM Funcionario f WHERE f.funcao = ?1")
    List<Funcionario> findByFuncao(String funcao);
    
    @Query("SELECT f FROM Funcionario f WHERE f.cidade = ?1")
    List<Funcionario> findByCidade(String cidade);
}
