package com.poprc.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ImportacaoEstoquePlanilhaDetalheDTO(
        Long importacaoId,
        String nomeArquivo,
        LocalDateTime dataImportacao,
        LocalDateTime dataComplementacao,
        String importadoPor,
        String complementadoPor,
        String deposito,
        Integer itensProcessados,
        Integer materiaisCriados,
        Integer materiaisAtualizados,
        Integer ajustesPositivos,
        Integer ajustesNegativos,
        BigDecimal valorTotalImportado,
        Integer entradasImportadas,
        Integer abasRetiradaProcessadas,
        Integer retiradasImportadas,
        Integer faltasIdentificadas,
        List<ItemEstoque> itens,
        List<Retirada> retiradas) {

    public record ItemEstoque(
            Long materialId,
            String material,
            BigDecimal saldoAnterior,
            BigDecimal saldoImportado,
            BigDecimal custoUnitario,
            String acao) {
    }

    public record Retirada(
            String aba,
            Long ordemRetiradaId,
            String numeroOr,
            Long comarcaId,
            String comarca,
            String numeroOs,
            Long materialId,
            String material,
            BigDecimal saldoInicial,
            BigDecimal quantidadeRetirada,
            BigDecimal saldoFinal,
            BigDecimal quantidadeFaltante,
            BigDecimal custoUnitario,
            LocalDate dataRetirada) {
    }
}
