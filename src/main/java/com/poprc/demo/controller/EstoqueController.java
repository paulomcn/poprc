package com.poprc.demo.controller;

import com.poprc.demo.model.Material;
import com.poprc.demo.model.MovimentacaoEstoque;
import com.poprc.demo.repository.MaterialRepository;
import com.poprc.demo.repository.MovimentacaoEstoqueRepository; //  IMPORT DO REPOSITORY NOVO
import com.poprc.demo.service.EstoqueService;
import lombok.RequiredArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/estoque")
@RequiredArgsConstructor
public class EstoqueController {

    private final EstoqueService estoqueService;
    private final MaterialRepository materialRepository;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository; // INJEÇÃO DIRETA

    @GetMapping("/materiais")
    public ResponseEntity<List<Material>> listarMateriais() {
        List<Material> materiais = materialRepository.findAll();
        return ResponseEntity.ok(materiais);
    }

    @PostMapping("/materiais")
    public ResponseEntity<Material> cadastrarNovoMaterial(@RequestBody Material material) {
        // Garante que o material novo comece com zero unidades disponíveis no estoque
        if (material.getQuantidadeDisponivel() == null) {
            material.setQuantidadeDisponivel(0);
        }
        Material salvo = materialRepository.save(material);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    // ROTA DO HISTÓRICO CORRIGIDA: Buscando direto do banco pelo repository
    @GetMapping("/historico")
    public ResponseEntity<List<MovimentacaoEstoque>> listarHistorico() {
        List<MovimentacaoEstoque> historico = movimentacaoEstoqueRepository.findAll();
        return ResponseEntity.ok(historico);
    }

    @PostMapping("/entrada")
    public ResponseEntity<MovimentacaoEstoque> registrarEntrada(@RequestBody EntradaRequest request) {
        MovimentacaoEstoque movimentacao = estoqueService.registrarEntrada(
                request.getMaterialId(),
                request.getQuantidade(),
                request.getFuncionarioId());
        return ResponseEntity.status(HttpStatus.CREATED).body(movimentacao);
    }

    @PostMapping("/saida")
    public ResponseEntity<MovimentacaoEstoque> registrarSaida(@RequestBody SaidaRequest request) {
        MovimentacaoEstoque movimentacao = estoqueService.registrarSaida(
                request.getMaterialId(),
                request.getQuantidade(),
                request.getFuncionarioId());
        return ResponseEntity.status(HttpStatus.CREATED).body(movimentacao);
    }

    @Data
    public static class EntradaRequest {
        private Long materialId;
        private Integer quantidade;
        private Long funcionarioId;
    }

    @Data
    public static class SaidaRequest {
        private Long materialId;
        private Integer quantidade;
        private Long funcionarioId;
    }
}