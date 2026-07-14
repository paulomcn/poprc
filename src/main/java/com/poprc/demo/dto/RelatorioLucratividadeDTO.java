package com.poprc.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelatorioLucratividadeDTO {
    private Long projetoId;
    private String nomeProjeto;
    private BigDecimal totalFaturado;
    private BigDecimal totalCustoViagens;
    private BigDecimal totalCustoMateriais;
    private Boolean custoMateriaisDisponivel;
    private Boolean resultadoFinanceiroParcial;
    private BigDecimal custoTotalAcumulado;
    private BigDecimal lucroBruto;
    private BigDecimal margemLucro;
    private String saudeFinanceira;
}
