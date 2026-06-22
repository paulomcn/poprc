package com.poprc.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "materiais_projeto")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaterialProjeto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quantidade_prevista", nullable = false)
    private Integer quantidadePrevista;

    @Column(name = "quantidade_utilizada", nullable = false)
    private Integer quantidadeUtilizada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;
}