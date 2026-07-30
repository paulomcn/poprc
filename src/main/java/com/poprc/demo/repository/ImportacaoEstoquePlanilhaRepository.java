package com.poprc.demo.repository;

import com.poprc.demo.model.ImportacaoEstoquePlanilha;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportacaoEstoquePlanilhaRepository
        extends JpaRepository<ImportacaoEstoquePlanilha, Long> {

    boolean existsByHashSha256(String hashSha256);
}
