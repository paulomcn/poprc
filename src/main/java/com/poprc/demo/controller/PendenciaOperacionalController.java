package com.poprc.demo.controller;

import com.poprc.demo.dto.PendenciaOperacionalDTO;
import com.poprc.demo.service.PendenciaOperacionalService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pendencias-operacionais")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PendenciaOperacionalController {

    private final PendenciaOperacionalService service;

    @GetMapping
    public ResponseEntity<List<PendenciaOperacionalDTO>> listar(
            @RequestParam(required = false) String area,
            @RequestParam(required = false) Long funcionarioId) {
        return ResponseEntity.ok(service.listar(area, funcionarioId));
    }
}
