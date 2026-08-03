package com.poprc.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ImportacaoEstoquePlanilhaRequest(
        String nomeArquivo,
        String hashSha256,
        Long localEstoqueId,
        List<ItemImportacao> itens,
        List<RetiradaImportacao> retiradas) {

    public record ItemImportacao(
            String nome,
            Integer quantidade,
            BigDecimal custoUnitario) {
    }

    public record RetiradaImportacao(
            String aba,
            Long comarcaId,
            String nomeMaterial,
            BigDecimal saldoInicial,
            BigDecimal quantidadeRetirada,
            BigDecimal saldoFinal,
            BigDecimal custoUnitario,
            LocalDate dataRetirada) {
    }
}
