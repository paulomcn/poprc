package com.poprc.demo.controller;

import com.poprc.demo.repository.ContratoRepository;
import com.poprc.demo.repository.ProjetoRepository;
import com.poprc.demo.repository.FuncionarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private ContratoRepository contratoRepository;

    @Autowired
    private ProjetoRepository projetoRepository;

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> buscarEstatisticas() {
        // 1. Conta os registros reais direto nas tabelas do Postgres
        long totalContratos = contratoRepository.count();
        long totalProjetos = projetoRepository.count();
        long totalFuncionarios = funcionarioRepository.count();

        // 2. Calcula a soma do valor global de todos os contratos de forma segura
        BigDecimal valorTotalGlobal = contratoRepository.findAll().stream()
                .map(c -> c.getValorGlobal() != null ? c.getValorGlobal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Monta o mapa de resposta estruturado pro Front-end
        Map<String, Object> response = new HashMap<>();
        response.put("contratosAtivos", totalContratos);
        response.put("projetosAndamento", totalProjetos);
        response.put("funcionarios", totalFuncionarios);
        response.put("valorTotal", valorTotalGlobal);

        return ResponseEntity.ok(response);
    }
}