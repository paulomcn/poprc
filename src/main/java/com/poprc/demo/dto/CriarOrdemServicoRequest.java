package com.poprc.demo.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CriarOrdemServicoRequest {
    private Long contratoId;
    private Long projetoId;
    private String descricao;
    private LocalDateTime dataHoraInicio;
    private LocalDateTime dataHoraFim;
    private LocalDateTime deadline;
    private List<MaterialPrevistoRequest> materiais;

    @Data
    public static class MaterialPrevistoRequest {
        private Long materialId;
        private Integer quantidadePrevista;
    }
}
