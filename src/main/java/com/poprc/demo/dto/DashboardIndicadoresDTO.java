package com.poprc.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardIndicadoresDTO {
    private Long contratosAtivos;
    private BigDecimal valorTotalContratado;
    private BigDecimal valorFaturado;
    private BigDecimal valorPendenteFaturamento;
    private Long totalComarcasConcluidas;
    private Long totalComarcasEmAtraso;
    private BigDecimal custosAcumuladosViagem;
}