package com.poprc.demo.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class EquipeProjetoRequest {
    private List<MembroRequest> membros = new ArrayList<>();

    @Data
    public static class MembroRequest {
        private Long funcionarioId;
        private String papel;
        private Boolean responsavelPrincipal = false;
    }
}
