package com.poprc.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "materiais")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false)
    private String nome;

    @Column(unique = true, nullable = false)
    private String partNumber;

    private String fabricante;

    private String fornecedor;

    private String categoria = "MATERIAL_CONSUMO";

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(columnDefinition = "TEXT")
    private String fotoProdutoUrl;

    private Integer quantidadeDisponivel;
    private Integer quantidadeReservada = 0;

    @Enumerated(EnumType.STRING)
    private TipoControleEstoque tipoControle = TipoControleEstoque.UNIDADE;

    @Enumerated(EnumType.STRING)
    private UnidadeMedida unidadeMedida = UnidadeMedida.UNIDADE;

    private String dimensao;

    @Column(precision = 14, scale = 3)
    private BigDecimal comprimentoPorPeca;

    @Column(precision = 14, scale = 3)
    private BigDecimal metragemDisponivel = BigDecimal.ZERO;

    @Column(precision = 14, scale = 3)
    private BigDecimal metragemReservada = BigDecimal.ZERO;

    @Column(precision = 14, scale = 3)
    private BigDecimal estoqueMinimo = BigDecimal.ZERO;

    @Column(name = "custo_medio", precision = 15, scale = 4, nullable = false)
    private BigDecimal custoMedio = BigDecimal.ZERO;

    private String localizacao;

    @Column(nullable = false)
    private Boolean ativo = true;

    private LocalDateTime removidoEm;

    private String removidoPor;

    private LocalDateTime restauradoEm;

    private String restauradoPor;

    @Transient
    public BigDecimal getValorTotalEstoque() {
        BigDecimal saldo = controlaMetragem()
                ? valor(metragemDisponivel)
                : BigDecimal.valueOf(quantidadeDisponivel != null ? quantidadeDisponivel : 0);
        return valor(custoMedio).multiply(saldo).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean controlaMetragem() {
        return TipoControleEstoque.METRAGEM.equals(tipoControle)
                || TipoControleEstoque.BOBINA.equals(tipoControle)
                || TipoControleEstoque.ROLO.equals(tipoControle);
    }

    private BigDecimal valor(BigDecimal numero) {
        return numero != null ? numero : BigDecimal.ZERO;
    }
}
