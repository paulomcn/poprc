package com.poprc.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.Data;

@Entity
@Table(name = "saldos_materiais_locais", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "material_id", "local_estoque_id" })
})
@Data
public class SaldoMaterialLocal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "material_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Material material;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "local_estoque_id", nullable = false)
    private LocalEstoque localEstoque;

    private Integer quantidadeDisponivel = 0;
    private Integer quantidadeReservada = 0;

    @Column(precision = 14, scale = 3, nullable = false)
    private BigDecimal metragemDisponivel = BigDecimal.ZERO;

    @Column(precision = 14, scale = 3, nullable = false)
    private BigDecimal metragemReservada = BigDecimal.ZERO;
}
