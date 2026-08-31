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
@Table(name = "importacoes_estoque_planilha_itens")
@Data
public class ImportacaoEstoqueItemPlanilha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "importacao_id", nullable = false)
    private ImportacaoEstoquePlanilha importacao;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "nome_planilha", nullable = false)
    private String nomePlanilha;

    @Column(name = "saldo_anterior", nullable = false)
    private BigDecimal saldoAnterior;

    @Column(name = "saldo_importado", nullable = false)
    private BigDecimal saldoImportado;

    @Column(name = "custo_unitario", precision = 19, scale = 4, nullable = false)
    private BigDecimal custoUnitario = BigDecimal.ZERO;

    @Column(name = "custo_anterior", precision = 19, scale = 4, nullable = false)
    private BigDecimal custoAnterior = BigDecimal.ZERO;

    @Column(name = "linha_origem")
    private Integer linhaOrigem;

    @Column(nullable = false, length = 30)
    private String acao;
}
