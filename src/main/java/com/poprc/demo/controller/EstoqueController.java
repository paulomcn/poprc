package com.poprc.demo.controller;

import com.poprc.demo.model.MovimentacaoEstoque;
import com.poprc.demo.service.EstoqueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/estoque")
@RequiredArgsConstructor
public class EstoqueController {

    private final EstoqueService estoqueService;

    @PostMapping("/entrada")
    public ResponseEntity<MovimentacaoEstoque> registrarEntrada(@RequestBody EntradaRequest request) {
        MovimentacaoEstoque movimentacao = estoqueService.registrarEntrada(
                request.getMaterialId(),
                request.getQuantidade(),
                request.getFuncionarioId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(movimentacao);
    }

    @PostMapping("/saida")
    public ResponseEntity<MovimentacaoEstoque> registrarSaida(@RequestBody SaidaRequest request) {
        MovimentacaoEstoque movimentacao = estoqueService.registrarSaida(
                request.getMaterialId(),
                request.getQuantidade(),
                request.getFuncionarioId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(movimentacao);
    }

    public static class EntradaRequest {
        private Long materialId;
        private Integer quantidade;
        private Long funcionarioId;

        public Long getMaterialId() {
            return materialId;
        }

        public void setMaterialId(Long materialId) {
            this.materialId = materialId;
        }

        public Integer getQuantidade() {
            return quantidade;
        }

        public void setQuantidade(Integer quantidade) {
            this.quantidade = quantidade;
        }

        public Long getFuncionarioId() {
            return funcionarioId;
        }

        public void setFuncionarioId(Long funcionarioId) {
            this.funcionarioId = funcionarioId;
        }
    }

    public static class SaidaRequest {
        private Long materialId;
        private Integer quantidade;
        private Long funcionarioId;

        public Long getMaterialId() {
            return materialId;
        }

        public void setMaterialId(Long materialId) {
            this.materialId = materialId;
        }

        public Integer getQuantidade() {
            return quantidade;
        }

        public void setQuantidade(Integer quantidade) {
            this.quantidade = quantidade;
        }

        public Long getFuncionarioId() {
            return funcionarioId;
        }

        public void setFuncionarioId(Long funcionarioId) {
            this.funcionarioId = funcionarioId;
        }
    }
}
