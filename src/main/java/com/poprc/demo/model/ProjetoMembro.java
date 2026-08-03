package com.poprc.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "projetos_membros", uniqueConstraints = {
        @UniqueConstraint(name = "uk_projeto_membro_funcionario", columnNames = { "projeto_id", "funcionario_id" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjetoMembro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projeto_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Projeto projeto;

    @ManyToOne(optional = false)
    @JoinColumn(name = "funcionario_id", nullable = false)
    @JsonIgnoreProperties({ "documentPaths", "certificacoes" })
    private Funcionario funcionario;

    @Column(nullable = false, length = 40)
    private String papel;

    @Column(nullable = false)
    private Boolean responsavelPrincipal = false;
}
