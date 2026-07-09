package com.poprc.demo.service;

import com.poprc.demo.model.Funcionario;
import com.poprc.demo.model.Material;
import com.poprc.demo.model.MovimentacaoEstoque;
import com.poprc.demo.model.TipoMovimentacao;
import com.poprc.demo.repository.FuncionarioRepository;
import com.poprc.demo.repository.MaterialRepository;
import com.poprc.demo.repository.MovimentacaoEstoqueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EstoqueService {

    private final MaterialRepository materialRepository;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    private final FuncionarioRepository funcionarioRepository;

    @Transactional
    public MovimentacaoEstoque registrarEntrada(Long materialId, Integer quantidade, Long funcionarioId) {
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new IllegalArgumentException("Material não encontrado"));
        
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado"));

        material.setQuantidadeDisponivel(material.getQuantidadeDisponivel() + quantidade);
        materialRepository.save(material);

        MovimentacaoEstoque movimentacao = new MovimentacaoEstoque();
        movimentacao.setMaterial(material);
        movimentacao.setQuantidade(quantidade);
        movimentacao.setTipo(TipoMovimentacao.ENTRADA);
        movimentacao.setFuncionario(funcionario);
        movimentacao.setDataMovimentacao(LocalDateTime.now());

        return movimentacaoEstoqueRepository.save(movimentacao);
    }

    @Transactional
    public MovimentacaoEstoque registrarSaida(Long materialId, Integer quantidade, Long funcionarioId, Long comarcaId) {
        throw new IllegalArgumentException("Saída de material exige uma Ordem de Retirada executada e assinada.");
    }
}
