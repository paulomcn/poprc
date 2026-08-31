package com.poprc.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ReconciliacaoRetiradasPlanilhaDTO {
    private ReconciliacaoRetiradasPlanilhaDTO() {
    }

    public record Resultado(
            boolean aplicado,
            int divergencias,
            List<Divergencia> itens) {
    }

    public record Divergencia(
            Long retiradaImportadaId,
            String aba,
            Long materialId,
            String material,
            BigDecimal quantidadeAnterior,
            BigDecimal quantidadeNova,
            BigDecimal saldoInicialAnterior,
            BigDecimal saldoInicialNovo,
            BigDecimal saldoFinalAnterior,
            BigDecimal saldoFinalNovo,
            BigDecimal faltaAnterior,
            BigDecimal faltaNova) {
    }

    public record Evento(
            Long id,
            Long retiradaImportadaId,
            String aba,
            String material,
            String nomeArquivo,
            BigDecimal quantidadeAnterior,
            BigDecimal quantidadeNova,
            String reconciliadoPor,
            LocalDateTime reconciliadoEm) {
    }
}
