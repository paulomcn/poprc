package com.poprc.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "faturamentos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Faturamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "servicos_executados", columnDefinition = "TEXT")
    private String servicosExecutados;

    @Column(name = "valor_medicao", nullable = false)
    private BigDecimal valorMedicao;

    @Column(name = "numero_nota_fiscal")
    private String numeroNotaFiscal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SituacaoFaturamento situacao;

    @Column(name = "data_vencimento")
    private LocalDate dataVencimento;

    @Column(name = "data_emissao")
    private LocalDate dataEmissao;

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;

    @Column(name = "competencia_fiscal")
    private LocalDate competenciaFiscal;

    @Column(name = "aliquota_imposto_retido", precision = 7, scale = 6)
    private BigDecimal aliquotaImpostoRetido;

    @Column(name = "aliquota_imposto_pagar", precision = 7, scale = 6)
    private BigDecimal aliquotaImpostoPagar;

    @Column(name = "imposto_retido", precision = 19, scale = 2)
    private BigDecimal impostoRetido;

    @Column(name = "imposto_pagar", precision = 19, scale = 2)
    private BigDecimal impostoPagar;

    @Column(name = "imposto_total", precision = 19, scale = 2)
    private BigDecimal impostoTotal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_id", nullable = false)
    @JsonIgnoreProperties("projetos")
    private Contrato contrato;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id")
    @JsonIgnoreProperties({ "materiais", "equipe", "responsavel" })
    private Projeto projeto;
}
