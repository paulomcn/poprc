package com.poprc.demo.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "contratos")
@Data // Adeus Getters, Setters, Equals e HashCode manuais
@NoArgsConstructor
@AllArgsConstructor
public class Contrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cliente;

    private String contrato;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_contratante", nullable = false)
    private TipoContratante tipoContratante = TipoContratante.SETOR_PUBLICO;

    private LocalDate vigenciaInicio;

    private LocalDate vigenciaFim;

    @ManyToOne
    @JoinColumn(name = "fiscal_tecnico_id")
    private Funcionario fiscalTecnico;

    @ManyToOne
    @JoinColumn(name = "gestor_contrato_id")
    private Funcionario gestorContrato;

    @Column(precision = 19, scale = 2)
    private BigDecimal valorGlobal;

    @Column(length = 2000)
    private String escopo;
    private String status = "ATIVO";

    private Boolean arquivado = false;

    private LocalDateTime arquivadoEm;

    private String arquivadoPor;

    @Column(length = 1000)
    private String motivoArquivamento;

    @OneToMany(mappedBy = "contrato", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("contrato") // Evita recursão infinita no JSON
    @ToString.Exclude // Evita loop no toString do Lombok
    @EqualsAndHashCode.Exclude // Evita loop no equals do Lombok
    private List<Projeto> projetos = new ArrayList<>();
}
