package com.poprc.demo.dto;

import java.math.BigDecimal;
import java.util.List;

public record AtualizacaoCustosPlanilhaRequest(
        String nomeArquivo,
        String hashSha256,
        List<ItemCusto> itens) {

    public record ItemCusto(
            Long materialId,
            String nomePlanilha,
            BigDecimal custoUnitario,
            Integer linhaOrigem) {
    }
}
