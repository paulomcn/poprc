package com.poprc.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "projetos")
@Data //
@NoArgsConstructor // Construtor padrão vazio
@AllArgsConstructor // Construtor com todos os campos
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

    // ️ CAMPOS NOVOS DA OPÇÃO 1 (AUDITORIA REAL)
    @Column(name = "as_built_status")
    private String asBuiltStatus = "PENDENTE"; // PENDENTE ou HOMOLOGADO

    @OneToMany(mappedBy = "projeto", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("projeto")
    private List<MaterialProjeto> materiais;
}