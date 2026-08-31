package com.poprc.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReconciliacaoRetiradasPlanilhaRequest(
        String nomeArquivo,
        String hashSha256,
        boolean confirmar,
        List<Item> itens) {

    public record Item(
            Long retiradaImportadaId,
            BigDecimal saldoInicial,
            BigDecimal quantidadeRetirada,
            BigDecimal saldoFinal,
            LocalDate dataRetirada) {
    }
}
