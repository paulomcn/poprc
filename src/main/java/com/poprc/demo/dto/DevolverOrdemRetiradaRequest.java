package com.poprc.demo.dto;

import lombok.Data;

import java.util.List;

@Data
public class DevolverOrdemRetiradaRequest {
    private String devolvidoPor;
    private String recebidoPor;
    private String assinaturaRecebimentoBase64;
    private List<ItemDevolucaoRequest> itens;

    @Data
    public static class ItemDevolucaoRequest {
        private Long itemId;
        private Integer quantidadeDevolvida;
    }
}
