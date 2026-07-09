package com.poprc.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ordem_retirada_itens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdemRetiradaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nomeMaterial;

    private String categoria;

    private Integer quantidadeSolicitada = 0;

    private Integer quantidadeRetirada = 0;

    private Integer quantidadeDevolvida = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordem_retirada_id")
    @JsonIgnoreProperties("itens")
    private OrdemRetirada ordemRetirada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id")
    private Material material;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_item_id")
    @JsonIgnoreProperties({ "comarca", "material" })
    private MaterialItem materialItem;
}
