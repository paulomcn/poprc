package com.poprc.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
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

    private String localizacao;
}
