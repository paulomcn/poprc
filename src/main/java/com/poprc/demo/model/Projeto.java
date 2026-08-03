package com.poprc.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "projetos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Projeto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "contrato_id")
    private Contrato contrato;

    private LocalDate dataInicio;

    private LocalDate dataFim;

    @Enumerated(EnumType.STRING)
    private ProjetoStatus status;

    @ManyToOne
    @JoinColumn(name = "responsavel_id")
    private Funcionario responsavel;

    @OneToMany(mappedBy = "projeto", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("projeto")
    private List<ProjetoMembro> equipe = new ArrayList<>();

    @Column(name = "as_built_status")
    private String asBuiltStatus = "PENDENTE";

    private Boolean arquivado = false;

    private LocalDateTime arquivadoEm;

    private String arquivadoPor;

    @Column(length = 1000)
    private String motivoArquivamento;

    @OneToMany(mappedBy = "projeto", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("projeto")
    private List<MaterialProjeto> materiais;

    // NOVO: usado só na criação para nomear a comarca vinculada; não é
    // persistido
    @Transient
    private String nomeComarcaVinculada;
}
