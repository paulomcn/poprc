package com.poprc.demo.controller;

import com.poprc.demo.model.AsBuilt;
import com.poprc.demo.service.AsBuiltService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/as-built")
@RequiredArgsConstructor
public class AsBuiltController {

    private final AsBuiltService service;

    @PostMapping
    public ResponseEntity<AsBuilt> criar(@RequestBody AsBuilt asBuilt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.salvar(asBuilt));
    }

    @PatchMapping("/{id}/nova-versao")
    public ResponseEntity<AsBuilt> atualizarVersao(
            @PathVariable Long id, 
            @RequestBody AtualizarVersaoDTO dto) {
        
        AsBuilt atualizado = service.atualizarVersao(id, dto.getNovaUrl(), dto.getUsuario());
        return ResponseEntity.ok(atualizado);
    }

    @Data
    public static class AtualizarVersaoDTO {
        private String novaUrl;
        private String usuario;
    }
}