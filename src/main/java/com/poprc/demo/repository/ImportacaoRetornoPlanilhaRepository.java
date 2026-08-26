package com.poprc.demo.repository;

import com.poprc.demo.model.ImportacaoRetornoPlanilha;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportacaoRetornoPlanilhaRepository
        extends JpaRepository<ImportacaoRetornoPlanilha, Long> {

    boolean existsByAbaOrigemIgnoreCaseAndComarcaProjetoContratoIdAndMaterialIdAndQuantidadeRetornada(
            String abaOrigem,
            Long contratoId,
            Long materialId,
            java.math.BigDecimal quantidadeRetornada);
}
