package com.poprc.demo.controller;

import com.poprc.demo.dto.RelatorioLucratividadeDTO;
import com.poprc.demo.service.RelatorioFinanceiroService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/relatorios")
@RequiredArgsConstructor
public class RelatorioFinanceiroController {

    private final RelatorioFinanceiroService service;

    @GetMapping("/projeto/{id}/lucratividade")
    public ResponseEntity<RelatorioLucratividadeDTO> obterLucratividade(@PathVariable Long id) {
        return ResponseEntity.ok(service.gerarRelatorioLucratividade(id));
    }
}