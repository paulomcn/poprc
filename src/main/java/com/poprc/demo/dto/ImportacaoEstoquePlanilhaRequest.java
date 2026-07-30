package com.poprc.demo.dto;

import java.math.BigDecimal;
import java.util.List;

public record ImportacaoEstoquePlanilhaRequest(
        String nomeArquivo,
        String hashSha256,
        Long localEstoqueId,
        List<ItemImportacao> itens) {

    public record ItemImportacao(
            String nome,
            Integer quantidade,
            BigDecimal custoUnitario) {
    }
}
