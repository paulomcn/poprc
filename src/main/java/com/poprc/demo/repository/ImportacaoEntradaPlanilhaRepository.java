package com.poprc.demo.repository;

import com.poprc.demo.model.ImportacaoEntradaPlanilha;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportacaoEntradaPlanilhaRepository
        extends JpaRepository<ImportacaoEntradaPlanilha, Long> {

    boolean existsByChaveEvento(String chaveEvento);
}
