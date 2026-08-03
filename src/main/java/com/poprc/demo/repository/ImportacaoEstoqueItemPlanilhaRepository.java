package com.poprc.demo.repository;

import com.poprc.demo.model.ImportacaoEstoqueItemPlanilha;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportacaoEstoqueItemPlanilhaRepository
        extends JpaRepository<ImportacaoEstoqueItemPlanilha, Long> {

    List<ImportacaoEstoqueItemPlanilha> findByImportacaoIdOrderByNomePlanilhaAsc(Long importacaoId);

    boolean existsByImportacaoId(Long importacaoId);
}
