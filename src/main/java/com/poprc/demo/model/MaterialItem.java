package com.poprc.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "comarca_materiais")
@Data
public class MaterialItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nomeMaterial;
    private int quantidadePrevista;
    private int quantidadeAuditada = 0; // Atualizado na tela de auditoria
    private Boolean estoqueReservado = false;
    private Boolean estoqueBaixado = false;

    @ManyToOne
    @JoinColumn(name = "material_id")
    private Material material;

    @ManyToOne
    @JoinColumn(name = "comarca_id")
    private Comarca comarca;
}
