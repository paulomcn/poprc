package com.poprc.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ImportacaoEstoquePlanilhaRequest(
        String nomeArquivo,
        String hashSha256,
        Long localEstoqueId,
        Boolean saldoConsolidado,
        Boolean autoCriarOperacoes,
        Long contratoId,
        Long responsavelId,
        BigDecimal estoqueMinimoPadrao,
        List<ItemImportacao> itens,
        List<EntradaImportacao> entradas,
        List<RetiradaImportacao> retiradas,
        List<RetornoImportacao> retornos,
        List<SimulacaoImportacao> simulacao,
        List<String> avisos) {

    public ImportacaoEstoquePlanilhaRequest(
            String nomeArquivo,
            String hashSha256,
            Long localEstoqueId,
            List<ItemImportacao> itens,
            List<RetiradaImportacao> retiradas) {
        this(
                nomeArquivo,
                hashSha256,
                localEstoqueId,
                false,
                false,
                null,
                null,
                BigDecimal.ZERO,
                itens,
                List.of(),
                retiradas,
                List.of(),
                List.of(),
                List.of());
    }

    public ImportacaoEstoquePlanilhaRequest(
            String nomeArquivo,
            String hashSha256,
            Long localEstoqueId,
            List<ItemImportacao> itens,
            List<RetiradaImportacao> retiradas,
            List<String> avisos) {
        this(
                nomeArquivo,
                hashSha256,
                localEstoqueId,
                false,
                false,
                null,
                null,
                BigDecimal.ZERO,
                itens,
                List.of(),
                retiradas,
                List.of(),
                List.of(),
                avisos);
    }

    public record ItemImportacao(
            String nome,
            Integer quantidade,
            BigDecimal saldo,
            BigDecimal custoUnitario,
            Integer linhaOrigem) {

        public ItemImportacao(String nome, Integer quantidade, BigDecimal custoUnitario) {
            this(nome, quantidade, null, custoUnitario, null);
        }

        public ItemImportacao(
                String nome, Integer quantidade, BigDecimal custoUnitario, Integer linhaOrigem) {
            this(nome, quantidade, null, custoUnitario, linhaOrigem);
        }

        public BigDecimal saldoEfetivo() {
            return saldo != null
                    ? saldo
                    : BigDecimal.valueOf(quantidade != null ? quantidade : 0);
        }
    }

    public record EntradaImportacao(
            String tipo,
            String cabecalhoOrigem,
            String fornecedor,
            LocalDate dataEntrada,
            String nomeMaterial,
            BigDecimal quantidade,
            BigDecimal custoUnitario,
            Integer linhaOrigem,
            Integer colunaOrigem) {
    }

    public record RetiradaImportacao(
            String aba,
            Long comarcaId,
            String cidade,
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
                    null,
                    nomeMaterial,
                    saldoInicial,
                    quantidadeRetirada,
                    saldoFinal,
                    custoUnitario,
                    dataRetirada,
                    null);
        }

        public RetiradaImportacao(
                String aba,
                Long comarcaId,
                String nomeMaterial,
                BigDecimal saldoInicial,
                BigDecimal quantidadeRetirada,
                BigDecimal saldoFinal,
                BigDecimal custoUnitario,
                LocalDate dataRetirada,
                Integer linhaOrigem) {
            this(
                    aba,
                    comarcaId,
                    null,
                    nomeMaterial,
                    saldoInicial,
                    quantidadeRetirada,
                    saldoFinal,
                    custoUnitario,
                    dataRetirada,
                    linhaOrigem);
        }
    }

    public record RetornoImportacao(
            String aba,
            String cidade,
            String nomeMaterial,
            BigDecimal quantidadeRetornada,
            Integer linhaOrigem) {
    }

    public record SimulacaoImportacao(
            String nomeMaterial,
            BigDecimal estoqueAtual,
            BigDecimal quantidadeSimulada,
            BigDecimal saldoFinal,
            BigDecimal quantidadeFaltante,
            Integer linhaOrigem) {
    }
}
