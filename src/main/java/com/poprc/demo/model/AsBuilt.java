package com.poprc.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "as_builts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsBuilt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome_arquivo", nullable = false)
    private String nomeArquivo;

    @Column(name = "tipo_arquivo", nullable = false) // PDF, DWG, VISIO
    private String tipoArquivo;

    @Column(name = "url_arquivo", nullable = false)
    private String urlArquivo;

    @Column(nullable = false)
    private Integer versao = 1;

    @Column(name = "historico_versoes", columnDefinition = "TEXT")
    private String historicoVersoes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;
}