package com.poprc.demo.controller;

import com.poprc.demo.model.MaterialProjeto;
import com.poprc.demo.service.MaterialProjetoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/materiais-projeto")
@RequiredArgsConstructor
public class MaterialProjetoController {

    private final MaterialProjetoService service;

    @PostMapping
    public ResponseEntity<MaterialProjeto> criar(@RequestBody MaterialProjeto mp) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.salvar(mp));
    }

    @GetMapping("/{id}/metricas")
    public ResponseEntity<MaterialProjetoService.MetricasMaterialDTO> obterMetricas(@PathVariable Long id) {
        return ResponseEntity.ok(service.calcularMetricas(id));
    }
}