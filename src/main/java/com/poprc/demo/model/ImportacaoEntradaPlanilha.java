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
@Table(name = "importacoes_entradas_planilha")
@Data
public class ImportacaoEntradaPlanilha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "importacao_id", nullable = false)
    private ImportacaoEstoquePlanilha importacao;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "chave_evento", nullable = false, length = 36, unique = true)
    private String chaveEvento;

    @Column(name = "tipo_entrada", nullable = false, length = 30)
    private String tipoEntrada;

    @Column(name = "cabecalho_origem", nullable = false)
    private String cabecalhoOrigem;

    @Column(name = "fornecedor")
    private String fornecedor;

    @Column(name = "data_entrada")
    private LocalDate dataEntrada;

    @Column(name = "quantidade", precision = 14, scale = 3, nullable = false)
    private BigDecimal quantidade = BigDecimal.ZERO;

    @Column(name = "custo_unitario", precision = 19, scale = 2, nullable = false)
    private BigDecimal custoUnitario = BigDecimal.ZERO;

    @Column(name = "linha_origem")
    private Integer linhaOrigem;

    @Column(name = "coluna_origem")
    private Integer colunaOrigem;
}
