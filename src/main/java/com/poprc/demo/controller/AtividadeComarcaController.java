package com.poprc.demo.controller;

import com.poprc.demo.exception.ArquivoInvalidoException;
import com.poprc.demo.exception.DescricaoAtividadeObrigatoriaException;
import com.poprc.demo.model.AtividadeComarca;
import com.poprc.demo.service.AtividadeComarcaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comarcas")
@RequiredArgsConstructor
public class AtividadeComarcaController {

    private final AtividadeComarcaService atividadeComarcaService;

    @PostMapping("/{comarcaId}/atividades")
    public ResponseEntity<AtividadeComarca> registrar(
            @PathVariable Long comarcaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataEncerramento,
            @RequestParam String descricaoAtividades,
            @RequestParam(value = "fotos", required = false) List<MultipartFile> fotos) {

        AtividadeComarca atividade = atividadeComarcaService.registrarAtividade(
                comarcaId, dataInicio, dataEncerramento, descricaoAtividades, fotos);

        return ResponseEntity.status(HttpStatus.CREATED).body(atividade);
    }

    @GetMapping("/{comarcaId}/atividades")
    public ResponseEntity<List<AtividadeComarca>> listar(@PathVariable Long comarcaId) {
        return ResponseEntity.ok(atividadeComarcaService.listarPorComarca(comarcaId));
    }

    @PutMapping("/atividades/{id}")
    public ResponseEntity<AtividadeComarca> atualizar(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataEncerramento,
            @RequestParam String descricaoAtividades,
            @RequestParam(value = "novasFotos", required = false) List<MultipartFile> novasFotos) {

        AtividadeComarca atividade = atividadeComarcaService.atualizarAtividade(
                id, dataInicio, dataEncerramento, descricaoAtividades, novasFotos);

        return ResponseEntity.ok(atividade);
    }

    @ExceptionHandler(DescricaoAtividadeObrigatoriaException.class)
    public ResponseEntity<Map<String, String>> tratarDescricaoObrigatoria(DescricaoAtividadeObrigatoriaException e) {
        Map<String, String> erro = new HashMap<>();
        erro.put("erro", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    @ExceptionHandler(ArquivoInvalidoException.class)
    public ResponseEntity<Map<String, String>> tratarArquivoInvalido(ArquivoInvalidoException e) {
        Map<String, String> erro = new HashMap<>();
        erro.put("erro", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }
}