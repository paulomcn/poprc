package com.poprc.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ImportacaoEstoquePlanilhaResultadoDTO(
        Long importacaoId,
        String nomeArquivo,
        LocalDateTime dataImportacao,
        String importadoPor,
        String deposito,
        Integer itensProcessados,
        Integer materiaisCriados,
        Integer materiaisAtualizados,
        Integer ajustesPositivos,
        Integer ajustesNegativos,
        BigDecimal valorTotalImportado) {
}
