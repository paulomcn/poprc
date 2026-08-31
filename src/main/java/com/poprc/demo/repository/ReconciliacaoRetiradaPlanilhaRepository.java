package com.poprc.demo.repository;

import com.poprc.demo.model.ReconciliacaoRetiradaPlanilha;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconciliacaoRetiradaPlanilhaRepository
        extends JpaRepository<ReconciliacaoRetiradaPlanilha, Long> {

    boolean existsByRetiradaImportadaIdAndHashOrigem(Long retiradaImportadaId, String hashOrigem);

    List<ReconciliacaoRetiradaPlanilha> findTop100ByOrderByReconciliadoEmDesc();
}
