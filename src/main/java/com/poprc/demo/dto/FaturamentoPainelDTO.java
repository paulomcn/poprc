package com.poprc.demo.dto;

import com.poprc.demo.model.SituacaoFaturamento;
import com.poprc.demo.model.TipoContratante;
import java.math.BigDecimal;
import java.time.LocalDate;

public record FaturamentoPainelDTO(
        Long id,
        Long contratoId,
        String numeroContrato,
        String cliente,
        TipoContratante tipoContratante,
        Long projetoId,
        String destino,
        String servicosExecutados,
        BigDecimal valorMedicao,
        String numeroNotaFiscal,
        LocalDate dataEmissao,
        LocalDate dataVencimento,
        LocalDate dataPagamento,
        SituacaoFaturamento situacao,
        LocalDate competenciaFiscal,
        BigDecimal aliquotaImpostoRetido,
        BigDecimal aliquotaImpostoPagar,
        BigDecimal impostoRetido,
        BigDecimal impostoPagar,
        BigDecimal impostoTotal,
        String alertaFiscal) {
}
