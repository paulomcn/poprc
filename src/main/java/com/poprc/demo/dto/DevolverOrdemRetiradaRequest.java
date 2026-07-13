package com.poprc.demo.dto;

import lombok.Data;

import java.util.List;
import java.math.BigDecimal;

@Data
public class DevolverOrdemRetiradaRequest {
    private String devolvidoPor;
    private String recebidoPor;
    private String assinaturaRecebimentoBase64;
    private List<ItemDevolucaoRequest> itens;
    private List<AlocacaoDevolucaoRequest> alocacoes;

    @Data
    public static class ItemDevolucaoRequest {
        private Long itemId;
        private BigDecimal quantidadeDevolvida;
    }

    @Data
    public static class AlocacaoDevolucaoRequest {
        private Long alocacaoId;
        private BigDecimal metragemDevolvida;
    }
}
