package com.poprc.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comarcas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Comarca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nomeComarca;

    private String endereco;

    private String juizResponsavel;

    private String contatoLocal;

    private Integer quantidadePontos;

    private String situacao;

    @Column(precision = 19, scale = 2)
    private BigDecimal percentualConcluido;

    @Column(columnDefinition = "TEXT")
    private String pendencias;
}
