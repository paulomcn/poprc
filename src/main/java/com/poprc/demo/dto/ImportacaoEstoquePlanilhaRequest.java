package com.poprc.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ImportacaoEstoquePlanilhaRequest(
        String nomeArquivo,
        String hashSha256,
        Long localEstoqueId,
        List<ItemImportacao> itens,
        List<RetiradaImportacao> retiradas,
        List<String> avisos) {

    public ImportacaoEstoquePlanilhaRequest(
            String nomeArquivo,
            String hashSha256,
            Long localEstoqueId,
            List<ItemImportacao> itens,
            List<RetiradaImportacao> retiradas) {
        this(nomeArquivo, hashSha256, localEstoqueId, itens, retiradas, List.of());
    }

    public record ItemImportacao(
            String nome,
            Integer quantidade,
            BigDecimal custoUnitario,
            Integer linhaOrigem) {

        public ItemImportacao(String nome, Integer quantidade, BigDecimal custoUnitario) {
            this(nome, quantidade, custoUnitario, null);
        }
    }

    public record RetiradaImportacao(
            String aba,
            Long comarcaId,
            String nomeMaterial,
            BigDecimal saldoInicial,
            BigDecimal quantidadeRetirada,
            BigDecimal saldoFinal,
            BigDecimal custoUnitario,
            LocalDate dataRetirada,
            Integer linhaOrigem) {

        public RetiradaImportacao(
                String aba,
                Long comarcaId,
                String nomeMaterial,
                BigDecimal saldoInicial,
                BigDecimal quantidadeRetirada,
                BigDecimal saldoFinal,
                BigDecimal custoUnitario,
                LocalDate dataRetirada) {
            this(
                    aba,
                    comarcaId,
                    nomeMaterial,
                    saldoInicial,
                    quantidadeRetirada,
                    saldoFinal,
                    custoUnitario,
                    dataRetirada,
                    null);
        }
    }
}
