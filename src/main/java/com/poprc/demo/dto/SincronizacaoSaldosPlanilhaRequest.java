package com.poprc.demo.dto;

import java.math.BigDecimal;
import java.util.List;

public record SincronizacaoSaldosPlanilhaRequest(
        String nomeArquivo,
        String hashSha256,
        Long localEstoqueId,
        List<ItemSaldo> itens) {

    public record ItemSaldo(
            Long materialId,
            String nomePlanilha,
            BigDecimal saldo,
            Integer linhaOrigem) {
    }
}
