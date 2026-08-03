package com.poprc.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelatorioLucratividadeDTO {
    private Long projetoId;
    private String nomeProjeto;
    private Long contratoId;
    private String numeroContrato;
    private Long ordemServicoId;
    private String numeroOs;
    private BigDecimal totalFaturado;
    private BigDecimal totalCustoViagens;
    private BigDecimal totalCustoMateriais;
    private Boolean custoMateriaisDisponivel;
    private Boolean custoMateriaisEstimado;
    private Boolean resultadoFinanceiroParcial;
    private BigDecimal custoTotalAcumulado;
    private BigDecimal lucroBruto;
    private BigDecimal margemLucro;
    private String saudeFinanceira;
    private BigDecimal receitaSemOrdemServico;
    private BigDecimal custoMateriaisSemOrdemServico;
    private BigDecimal custoViagensNaoAlocado;
    private List<RelatorioLucratividadeOsDTO> ordensServico;
}
