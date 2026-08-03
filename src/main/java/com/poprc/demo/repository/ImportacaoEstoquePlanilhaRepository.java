package com.poprc.demo.repository;

import com.poprc.demo.model.ImportacaoEstoquePlanilha;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportacaoEstoquePlanilhaRepository
        extends JpaRepository<ImportacaoEstoquePlanilha, Long> {

    boolean existsByHashSha256(String hashSha256);

    Optional<ImportacaoEstoquePlanilha> findByHashSha256(String hashSha256);

    List<ImportacaoEstoquePlanilha> findAllByOrderByDataImportacaoDesc();
}
