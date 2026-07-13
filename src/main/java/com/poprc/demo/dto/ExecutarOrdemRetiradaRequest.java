package com.poprc.demo.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ExecutarOrdemRetiradaRequest {
    private String conferidoPor;
    private String levadoPor;
    private String assinaturaConferenteBase64;
    private String assinaturaRetiranteBase64;
    private List<AlocacaoRequest> alocacoes;

    @Data
    public static class AlocacaoRequest {
        private Long itemId;
        private Long unidadeRastreavelId;
        private BigDecimal metragem;
    }
}
