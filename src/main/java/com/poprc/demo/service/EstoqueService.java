package com.poprc.demo.service;

import com.poprc.demo.exception.SaldoInsuficienteException;
import com.poprc.demo.model.Comarca;
import com.poprc.demo.model.Funcionario;
import com.poprc.demo.model.Material;
import com.poprc.demo.model.MovimentacaoEstoque;
import com.poprc.demo.model.TipoMovimentacao;
import com.poprc.demo.repository.ComarcaRepository;
import com.poprc.demo.repository.FuncionarioRepository;
import com.poprc.demo.repository.MaterialRepository;
import com.poprc.demo.repository.MovimentacaoEstoqueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
//import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EstoqueService {

    private final MaterialRepository materialRepository;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final ComarcaRepository comarcaRepository;

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
        /*
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new IllegalArgumentException("Material não encontrado"));
        
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado"));

        int reservado = material.getQuantidadeReservada() != null ? material.getQuantidadeReservada() : 0;
        int disponivel = material.getQuantidadeDisponivel() != null ? material.getQuantidadeDisponivel() : 0;
        int livre = Math.max(0, disponivel - reservado);

        if (livre < quantidade) {
            throw new SaldoInsuficienteException(
                    "Saldo disponível de estoque insuficiente. Disponível: " + livre +
                    ", Reservado: " + reservado +
                    ", Solicitado: " + quantidade);
        }

        material.setQuantidadeDisponivel(disponivel - quantidade);
        materialRepository.save(material);

        MovimentacaoEstoque movimentacao = new MovimentacaoEstoque();
        movimentacao.setMaterial(material);
        movimentacao.setQuantidade(quantidade);
        movimentacao.setTipo(TipoMovimentacao.SAIDA);
        movimentacao.setFuncionario(funcionario);
        movimentacao.setDataMovimentacao(LocalDateTime.now());
        vincularDestinoOperacional(movimentacao, comarcaId);
        movimentacao.setObservacao(montarObservacaoSaidaManual(movimentacao));

        return movimentacaoEstoqueRepository.save(movimentacao);
        */
    }

    private void vincularDestinoOperacional(MovimentacaoEstoque movimentacao, Long comarcaId) {
        if (comarcaId == null) {
            return;
        }

        Comarca comarca = comarcaRepository.findById(comarcaId)
                .orElseThrow(() -> new IllegalArgumentException("Comarca não encontrada para vincular a saída."));
        movimentacao.setComarca(comarca);
        movimentacao.setOrdemServico(comarca.getOrdemServico());
        movimentacao.setProjeto(comarca.getProjeto() != null ? comarca.getProjeto()
                : comarca.getOrdemServico() != null ? comarca.getOrdemServico().getProjeto() : null);
    }

    private String montarObservacaoSaidaManual(MovimentacaoEstoque movimentacao) {
        if (movimentacao.getComarca() == null) {
            return "Saída manual sem OS vinculada";
        }

        String numeroOs = movimentacao.getOrdemServico() != null
                ? movimentacao.getOrdemServico().getNumeroOs()
                : "OS não vinculada";
        return "Saída manual | " + numeroOs + " | " + movimentacao.getComarca().getNomeComarca();
    }
}
