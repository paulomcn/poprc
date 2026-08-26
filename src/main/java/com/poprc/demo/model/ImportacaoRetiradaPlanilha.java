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
import lombok.Data;

@Entity
@Table(name = "importacoes_retiradas_planilha")
@Data
public class ImportacaoRetiradaPlanilha {

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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ordem_retirada_id")
    private OrdemRetirada ordemRetirada;

    @Column(name = "aba_origem", nullable = false)
    private String abaOrigem;

    @Column(name = "saldo_inicial", precision = 14, scale = 3, nullable = false)
    private BigDecimal saldoInicial = BigDecimal.ZERO;

    @Column(name = "quantidade_retirada", precision = 14, scale = 3, nullable = false)
    private BigDecimal quantidadeRetirada = BigDecimal.ZERO;

    @Column(name = "saldo_final", precision = 14, scale = 3, nullable = false)
    private BigDecimal saldoFinal = BigDecimal.ZERO;

    @Column(name = "quantidade_faltante", precision = 14, scale = 3, nullable = false)
    private BigDecimal quantidadeFaltante = BigDecimal.ZERO;

    @Column(name = "custo_unitario", precision = 19, scale = 4, nullable = false)
    private BigDecimal custoUnitario = BigDecimal.ZERO;

    @Column(name = "data_retirada")
    private LocalDate dataRetirada;
}
