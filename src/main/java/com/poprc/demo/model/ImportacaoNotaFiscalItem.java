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
@Table(name = "importacoes_notas_fiscais_itens")
@Data
public class ImportacaoNotaFiscalItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String codigoProduto;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String descricao;
    private String ncm;
    private String cfop;
    private String unidadeFiscal;
    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal quantidade;
    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal valorUnitario;
    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal valorTotal;
    private String acao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "importacao_id")
    private ImportacaoNotaFiscal importacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id")
    private Material material;
}
