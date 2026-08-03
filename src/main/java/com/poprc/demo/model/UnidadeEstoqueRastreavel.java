package com.poprc.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "unidades_estoque_rastreaveis")
@Data
public class UnidadeEstoqueRastreavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false, unique = true)
    private String codigo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "material_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Material material;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "local_estoque_id")
    private LocalEstoque localEstoque;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoControleEstoque tipo;

    @Column(precision = 14, scale = 3, nullable = false)
    private BigDecimal metragemInicial;

    @Column(precision = 14, scale = 3, nullable = false)
    private BigDecimal metragemAtual;

    @Column(nullable = false)
    private LocalDateTime dataEntrada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusUnidadeRastreavel status = StatusUnidadeRastreavel.LACRADA;

    @Column(columnDefinition = "TEXT")
    private String observacao;
}
