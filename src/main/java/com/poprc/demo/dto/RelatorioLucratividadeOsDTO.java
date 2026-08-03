package com.poprc.demo.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelatorioLucratividadeOsDTO {
    private Long ordemServicoId;
    private String numeroOs;
    private Long projetoId;
    private String status;
    private BigDecimal totalFaturado;
    private BigDecimal totalCustoMateriais;
    private BigDecimal lucroOperacional;
    private BigDecimal margemLucro;
    private Boolean custoMateriaisDisponivel;
    private Boolean custoMateriaisEstimado;
}
