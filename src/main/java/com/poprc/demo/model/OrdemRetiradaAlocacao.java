package com.poprc.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "ordem_retirada_alocacoes")
@Data
public class OrdemRetiradaAlocacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordem_retirada_item_id", nullable = false)
    @JsonIgnoreProperties({ "ordemRetirada", "material", "materialItem", "alocacoes" })
    private OrdemRetiradaItem item;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "unidade_rastreavel_id", nullable = false)
    private UnidadeEstoqueRastreavel unidadeRastreavel;

    @Column(precision = 14, scale = 3, nullable = false)
    private BigDecimal metragemRetirada = BigDecimal.ZERO;

    @Column(precision = 14, scale = 3, nullable = false)
    private BigDecimal metragemDevolvida = BigDecimal.ZERO;

    @Column(name = "evidencia_retirada_path", length = 500)
    private String evidenciaRetiradaPath;

    @Column(name = "evidencia_retirada_nome")
    private String evidenciaRetiradaNome;

    @Column(name = "evidencia_retirada_data")
    private LocalDateTime evidenciaRetiradaData;

    @Column(name = "metragem_restante_apos_retirada", precision = 14, scale = 3)
    private BigDecimal metragemRestanteAposRetirada;

    @Column(name = "evidencia_devolucao_path", length = 500)
    private String evidenciaDevolucaoPath;

    @Column(name = "evidencia_devolucao_nome")
    private String evidenciaDevolucaoNome;

    @Column(name = "evidencia_devolucao_data")
    private LocalDateTime evidenciaDevolucaoData;

    @Column(name = "metragem_restante_apos_devolucao", precision = 14, scale = 3)
    private BigDecimal metragemRestanteAposDevolucao;
}
