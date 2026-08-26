package com.poprc.demo.repository;

import com.poprc.demo.model.ImportacaoRetiradaPlanilha;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportacaoRetiradaPlanilhaRepository
        extends JpaRepository<ImportacaoRetiradaPlanilha, Long> {

    List<ImportacaoRetiradaPlanilha> findByImportacaoIdOrderByAbaOrigemAscMaterialNomeAsc(Long importacaoId);

    List<ImportacaoRetiradaPlanilha> findAllByOrderByImportacaoDataImportacaoDescAbaOrigemAsc();

    boolean existsByImportacaoId(Long importacaoId);

    boolean existsByAbaOrigemIgnoreCaseAndComarcaProjetoContratoId(
            String abaOrigem, Long contratoId);
}
