package com.poprc.demo.controller;

import com.poprc.demo.model.Faturamento;
import com.poprc.demo.service.FaturamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/faturamentos")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class FaturamentoController {

    private final FaturamentoService faturamentoService;

    @GetMapping
    public ResponseEntity<List<Faturamento>> listarTodos() {
        return ResponseEntity.ok(faturamentoService.listarTodos());
    }

    @PostMapping
    public ResponseEntity<Faturamento> criarFaturamento(@RequestBody Faturamento faturamento) {
        Long contratoId = idContrato(faturamento);
        Long projetoId = idProjeto(faturamento);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(faturamentoService.registrarMedicao(faturamento, contratoId, projetoId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Faturamento> atualizarFaturamento(
            @PathVariable Long id,
            @RequestBody Faturamento dados) {
        return ResponseEntity.ok(faturamentoService.atualizarMedicao(
                id, dados, idContrato(dados), idProjeto(dados)));
    }

    @PutMapping("/{id}/emitir-nota")
    public ResponseEntity<Faturamento> emitirNotaFiscal(
            @PathVariable Long id,
            @RequestParam String numeroNotaFiscal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataVencimento) {
        return ResponseEntity.ok(
                faturamentoService.emitirNotaFiscal(id, numeroNotaFiscal, dataVencimento));
    }

    @PutMapping("/{id}/baixar-pagamento")
    public ResponseEntity<Faturamento> baixarPagamento(@PathVariable Long id) {
        return ResponseEntity.ok(faturamentoService.darBaixaPagamento(id));
    }

    @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
    public ResponseEntity<Map<String, String>> tratarErroDeRegra(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("erro", ex.getMessage()));
    }

    private Long idContrato(Faturamento faturamento) {
        if (faturamento.getContrato() == null || faturamento.getContrato().getId() == null) {
            throw new IllegalArgumentException("O contrato é obrigatório.");
        }
        return faturamento.getContrato().getId();
    }

    private Long idProjeto(Faturamento faturamento) {
        if (faturamento.getProjeto() == null || faturamento.getProjeto().getId() == null) {
            throw new IllegalArgumentException("O projeto é obrigatório.");
        }
        return faturamento.getProjeto().getId();
    }
}
