package com.poprc.demo.controller;

import com.poprc.demo.dto.DevolverOrdemRetiradaRequest;
import com.poprc.demo.dto.ExecutarOrdemRetiradaRequest;
import com.poprc.demo.model.OrdemRetirada;
import com.poprc.demo.service.OrdemRetiradaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ordens-retirada")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class OrdemRetiradaController {

    private final OrdemRetiradaService ordemRetiradaService;

    @GetMapping
    public ResponseEntity<List<OrdemRetirada>> listarTodas() {
        return ResponseEntity.ok(ordemRetiradaService.listarTodas());
    }

    @GetMapping("/comarca/{comarcaId}")
    public ResponseEntity<List<OrdemRetirada>> listarPorComarca(@PathVariable Long comarcaId) {
        return ResponseEntity.ok(ordemRetiradaService.listarPorComarca(comarcaId));
    }

    @GetMapping("/os/{ordemServicoId}")
    public ResponseEntity<List<OrdemRetirada>> listarPorOs(@PathVariable Long ordemServicoId) {
        return ResponseEntity.ok(ordemRetiradaService.listarPorOs(ordemServicoId));
    }

    @PostMapping("/os/{ordemServicoId}")
    public ResponseEntity<OrdemRetirada> gerarAdicionalPorOs(@PathVariable Long ordemServicoId) {
        return ResponseEntity.ok(ordemRetiradaService.criarAdicionalParaOs(ordemServicoId, "Sistema"));
    }

    @PatchMapping("/{id}/executar")
    public ResponseEntity<OrdemRetirada> executarRetirada(
            @PathVariable Long id,
            @RequestBody ExecutarOrdemRetiradaRequest request) {
        return ResponseEntity.ok(ordemRetiradaService.executarRetirada(id, request));
    }

    @PatchMapping("/{id}/devolver")
    public ResponseEntity<OrdemRetirada> devolver(
            @PathVariable Long id,
            @RequestBody DevolverOrdemRetiradaRequest request) {
        return ResponseEntity.ok(ordemRetiradaService.devolver(id, request));
    }
}
