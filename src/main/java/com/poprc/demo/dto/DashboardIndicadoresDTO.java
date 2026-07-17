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
    private Long totalOrdensServico;
    private Long ordensAbertas;
    private Long ordensEmExecucao;
    private Long ordensConcluidas;
    private Long ordensProximasPrazo;
    private Long obrasEmVistoria;
    private Long obrasEmInfraestrutura;
    private Long obrasEmViradaRede;
}
