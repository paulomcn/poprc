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
import java.math.BigDecimal;
import lombok.Data;

@Entity
@Table(name = "ordem_retirada_alocacoes")
@Data
public class OrdemRetiradaAlocacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordem_retirada_item_id", nullable = false)
    @JsonIgnoreProperties({ "ordemRetirada", "material", "materialItem", "alocacoes" })
    private OrdemRetiradaItem item;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "unidade_rastreavel_id", nullable = false)
    private UnidadeEstoqueRastreavel unidadeRastreavel;

    @Column(precision = 14, scale = 3, nullable = false)
    private BigDecimal metragemRetirada = BigDecimal.ZERO;

    @Column(precision = 14, scale = 3, nullable = false)
    private BigDecimal metragemDevolvida = BigDecimal.ZERO;
}
