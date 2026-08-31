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
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "reconciliacoes_retiradas_planilha")
@Data
public class ReconciliacaoRetiradaPlanilha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retirada_importada_id", nullable = false)
    private ImportacaoRetiradaPlanilha retiradaImportada;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "aba_origem", nullable = false)
    private String abaOrigem;

    @Column(name = "nome_arquivo", nullable = false)
    private String nomeArquivo;

    @Column(name = "hash_origem", length = 64, nullable = false)
    private String hashOrigem;

    @Column(name = "origem", length = 30, nullable = false)
    private String origem;

    @Column(name = "motivo", length = 500, nullable = false)
    private String motivo;

    @Column(name = "quantidade_anterior", precision = 14, scale = 3, nullable = false)
    private BigDecimal quantidadeAnterior;

    @Column(name = "quantidade_nova", precision = 14, scale = 3, nullable = false)
    private BigDecimal quantidadeNova;

    @Column(name = "saldo_inicial_anterior", precision = 14, scale = 3, nullable = false)
    private BigDecimal saldoInicialAnterior;

    @Column(name = "saldo_inicial_novo", precision = 14, scale = 3, nullable = false)
    private BigDecimal saldoInicialNovo;

    @Column(name = "saldo_final_anterior", precision = 14, scale = 3, nullable = false)
    private BigDecimal saldoFinalAnterior;

    @Column(name = "saldo_final_novo", precision = 14, scale = 3, nullable = false)
    private BigDecimal saldoFinalNovo;

    @Column(name = "falta_anterior", precision = 14, scale = 3, nullable = false)
    private BigDecimal faltaAnterior;

    @Column(name = "falta_nova", precision = 14, scale = 3, nullable = false)
    private BigDecimal faltaNova;

    @Column(name = "data_retirada_anterior")
    private LocalDate dataRetiradaAnterior;

    @Column(name = "data_retirada_nova")
    private LocalDate dataRetiradaNova;

    @Column(name = "reconciliado_por", nullable = false)
    private String reconciliadoPor;

    @Column(name = "reconciliado_em", nullable = false)
    private LocalDateTime reconciliadoEm;
}
