package com.poprc.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
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

    // NOVO: vínculo de origem com o projeto que gerou esta comarca
    // automaticamente
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id")
    @JsonIgnoreProperties({ "materiais" })
    private Projeto projeto;
}