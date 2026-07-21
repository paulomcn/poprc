package com.poprc.demo.dto;

import java.time.LocalDateTime;

public record PendenciaOperacionalDTO(
        String id,
        String tipo,
        String areaResponsavel,
        String prioridade,
        String titulo,
        String descricao,
        Long ordemServicoId,
        String numeroOs,
        Long comarcaId,
        String nomeObra,
        String responsavelNome,
        LocalDateTime deadline,
        String acao,
        String rota) {
}
