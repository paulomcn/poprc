package com.poprc.demo.repository;

import com.poprc.demo.model.ImportacaoNotaFiscal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportacaoNotaFiscalRepository extends JpaRepository<ImportacaoNotaFiscal, Long> {
    boolean existsByHashSha256(String hashSha256);
    List<ImportacaoNotaFiscal> findTop50ByOrderByDataImportacaoDesc();
}
