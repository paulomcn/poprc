package com.poprc.demo.controller;

import com.poprc.demo.dto.DashboardIndicadoresDTO;
import com.poprc.demo.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * GET: Retorna os indicadores agregados respeitando os filtros dinâmicos
     */
    @GetMapping("/executivo")
    public ResponseEntity<DashboardIndicadoresDTO> obterDashboardExecutivo(
            @RequestParam(required = false) String contrato,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {

        DashboardIndicadoresDTO indicadores = dashboardService.calcularIndicadores(contrato, inicio, fim);
        return ResponseEntity.ok(indicadores);
    }
}
