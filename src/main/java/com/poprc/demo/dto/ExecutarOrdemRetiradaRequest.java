package com.poprc.demo.dto;

import lombok.Data;

@Data
public class ExecutarOrdemRetiradaRequest {
    private String conferidoPor;
    private String levadoPor;
    private String assinaturaConferenteBase64;
    private String assinaturaRetiranteBase64;
}
