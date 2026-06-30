package com.poprc.demo.service;

import com.poprc.demo.dto.DashboardIndicadoresDTO;
import com.poprc.demo.repository.ContratoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ContratoRepository contratoRepository;

    /**
     * Roda a inteligência analítica cruzando dados e somando valores reais
     */
    public DashboardIndicadoresDTO calcularIndicadoresExecutivos() {
        // 1. Puxa dados reais do banco usando o que já temos de contratos
        long contratosAtivos = contratoRepository.count();

        BigDecimal valorTotalContratado = contratoRepository.findAll().stream()
                .map(c -> c.getValorGlobal() != null ? c.getValorGlobal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Simulação das regras de faturamento, comarcas e viagens
        // (Mantenha esses placeholders para o build passar limpo até criarmos essas
        // tabelas)
        BigDecimal valorFaturado = valorTotalContratado.multiply(new BigDecimal("0.60")); // Simula 60% faturado
        BigDecimal valorPendente = valorTotalContratado.subtract(valorFaturado);

        long comarcasConcluidas = 42L; // Placeholder analítico
        long comarcasEmAtraso = 3L; // Placeholder analítico
        BigDecimal custosViagem = new BigDecimal("12450.80"); // Placeholder analítico

        // 3. Gospe o DTO montado de elite
        return new DashboardIndicadoresDTO(
                contratosAtivos,
                valorTotalContratado,
                valorFaturado,
                valorPendente,
                comarcasConcluidas,
                comarcasEmAtraso,
                custosViagem);
    }
}