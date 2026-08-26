package com.poprc.demo.model;

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
import lombok.Data;

@Entity
@Table(name = "importacoes_retornos_planilha")
@Data
public class ImportacaoRetornoPlanilha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "importacao_id", nullable = false)
    private ImportacaoEstoquePlanilha importacao;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "comarca_id", nullable = false)
    private Comarca comarca;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "aba_origem", nullable = false)
    private String abaOrigem;

    @Column(name = "quantidade_retornada", precision = 14, scale = 3, nullable = false)
    private BigDecimal quantidadeRetornada = BigDecimal.ZERO;
}
