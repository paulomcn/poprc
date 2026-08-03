package com.poprc.demo.repository;

import com.poprc.demo.model.MovimentacaoEstoque;
import com.poprc.demo.model.TipoMovimentacao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Long> {
    List<MovimentacaoEstoque> findByComarcaIdOrderByDataMovimentacaoDesc(Long comarcaId);
    List<MovimentacaoEstoque> findByProjetoIdOrderByDataMovimentacaoDesc(Long projetoId);
    List<MovimentacaoEstoque> findByOrdemRetiradaIdAndMaterialIdAndTipoOrderByDataMovimentacaoAsc(
            Long ordemRetiradaId, Long materialId, TipoMovimentacao tipo);
    List<MovimentacaoEstoque> findAllByOrderByDataMovimentacaoDesc();
}
