package com.poprc.demo.repository;

import com.poprc.demo.model.ImportacaoNotaFiscalItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportacaoNotaFiscalItemRepository extends JpaRepository<ImportacaoNotaFiscalItem, Long> {
    List<ImportacaoNotaFiscalItem> findByImportacaoIdOrderByIdAsc(Long importacaoId);
}
