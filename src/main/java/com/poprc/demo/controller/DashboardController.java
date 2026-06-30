package com.poprc.demo.controller;

import com.poprc.demo.dto.DashboardIndicadoresDTO;
import com.poprc.demo.repository.ContratoRepository;
import com.poprc.demo.repository.ProjetoRepository;
import com.poprc.demo.repository.FuncionarioRepository;
import com.poprc.demo.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor // Mágica do Lombok: injeta tudo pelo construtor automaticamente
public class DashboardController {

    // Trocar para 'private final' ativa a injeção segura por construtor do Lombok
    private final ContratoRepository contratoRepository;
    private final ProjetoRepository projetoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final DashboardService dashboardService; // Injeta o novo Service analítico

    /**
     * Mantém a sua rota antiga funcionando 100% para a Home não quebrar!
     */
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

    /**
     * Nova Rota: Busca os KPIs estratégicos restritos à DIRETORIA ou ao CLIENTE
     */
    @GetMapping("/executivo")
    @PreAuthorize("hasAnyRole('ROLE_DIRETORIA', 'ROLE_CLIENTE')") // ️ Trava de segurança real
    public ResponseEntity<DashboardIndicadoresDTO> buscarDashboardExecutivo() {
        DashboardIndicadoresDTO dto = dashboardService.calcularIndicadoresExecutivos();
        return ResponseEntity.ok(dto);
    }
}