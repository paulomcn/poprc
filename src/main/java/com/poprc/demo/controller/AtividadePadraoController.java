package com.poprc.demo.controller;

import com.poprc.demo.model.AtividadePadrao;
import com.poprc.demo.repository.AtividadePadraoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/atividades-padrao")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AtividadePadraoController {

    private final AtividadePadraoRepository atividadePadraoRepository;

    @GetMapping
    public ResponseEntity<List<AtividadePadrao>> listarTodas() {
        Sort sort = Sort.by(Sort.Order.asc("ordemExibicao"), Sort.Order.asc("nome"));
        return ResponseEntity.ok(atividadePadraoRepository.findAll(sort));
    }

    @GetMapping("/ativas")
    public ResponseEntity<List<AtividadePadrao>> listarAtivas() {
        return ResponseEntity.ok(atividadePadraoRepository.findByAtivoTrueOrderByOrdemExibicaoAscNomeAsc());
    }

    @PostMapping
    public ResponseEntity<AtividadePadrao> criar(@RequestBody AtividadePadrao request) {
        normalizar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(atividadePadraoRepository.save(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AtividadePadrao> atualizar(@PathVariable Long id, @RequestBody AtividadePadrao request) {
        return atividadePadraoRepository.findById(id)
                .map(atividade -> {
                    atividade.setNome(request.getNome());
                    atividade.setCategoria(request.getCategoria());
                    atividade.setOrdemExibicao(request.getOrdemExibicao());
                    atividade.setAtivo(request.getAtivo());
                    normalizar(atividade);
                    return ResponseEntity.ok(atividadePadraoRepository.save(atividade));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<AtividadePadrao> alternarStatus(@PathVariable Long id) {
        return atividadePadraoRepository.findById(id)
                .map(atividade -> {
                    atividade.setAtivo(!Boolean.TRUE.equals(atividade.getAtivo()));
                    return ResponseEntity.ok(atividadePadraoRepository.save(atividade));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private void normalizar(AtividadePadrao atividade) {
        if (atividade.getNome() == null || atividade.getNome().trim().isEmpty()) {
            throw new IllegalArgumentException("Nome da atividade padrao e obrigatorio.");
        }
        atividade.setNome(atividade.getNome().trim());
        if (atividade.getCategoria() == null || atividade.getCategoria().trim().isEmpty()) {
            atividade.setCategoria("Geral");
        } else {
            atividade.setCategoria(atividade.getCategoria().trim());
        }
        if (atividade.getAtivo() == null) {
            atividade.setAtivo(true);
        }
        if (atividade.getOrdemExibicao() == null) {
            atividade.setOrdemExibicao(0);
        }
    }
}
