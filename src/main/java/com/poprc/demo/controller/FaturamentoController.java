package com.poprc.demo.controller;

import com.poprc.demo.model.Faturamento;
import com.poprc.demo.service.FaturamentoService;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/faturamentos")
@RequiredArgsConstructor
public class FaturamentoController {

    private final FaturamentoService service;

    @PostMapping
    public ResponseEntity<Faturamento> registrar(@RequestBody NovaMedicaoDTO dto) {
        Faturamento faturamento = new Faturamento();
        faturamento.setServicosExecutados(dto.getServicosExecutados());
        faturamento.setValorMedicao(dto.getValorMedicao());

        Faturamento salvo = service.registrarMedicao(faturamento, dto.getContratoId());
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    @PutMapping("/{id}/emitir-nf")
    public ResponseEntity<Faturamento> emitirNF(
            @PathVariable Long id,
            @RequestBody EmitirNfDTO dto) {
        Faturamento atualizado = service.emitirNotaFiscal(id, dto.getNumeroNotaFiscal(), dto.getDataVencimento());
        return ResponseEntity.ok(atualizado);
    }

    @PutMapping("/{id}/baixa")
    public ResponseEntity<Faturamento> darBaixa(@PathVariable Long id) {
        Faturamento atualizado = service.darBaixaPagamento(id);
        return ResponseEntity.ok(atualizado);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Faturamento> obter(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    // DTOs limpos para isolar a entrada do request
    @Data
    public static class NovaMedicaoDTO {
        private String servicosExecutados;
        private BigDecimal valorMedicao;
        private Long contratoId;
    }

    @Data
    public static class EmitirNfDTO {
        private String numeroNotaFiscal;
        private LocalDate dataVencimento;
    }

    @GetMapping
    public ResponseEntity<List<Faturamento>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }
}