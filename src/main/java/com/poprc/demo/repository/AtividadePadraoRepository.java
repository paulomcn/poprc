package com.poprc.demo.repository;

import com.poprc.demo.model.AtividadePadrao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AtividadePadraoRepository extends JpaRepository<AtividadePadrao, Long> {

    List<AtividadePadrao> findByAtivoTrueOrderByOrdemExibicaoAscNomeAsc();
}
