package com.poprc.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "historico_status_os")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoricoStatusOS {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ordem_servico_id", nullable = false, updatable = false)
    @JsonIgnore
    private OrdemServico ordemServico;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 40, updatable = false)
    private StatusOS statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, length = 40, updatable = false)
    private StatusOS statusNovo;

    @Column(nullable = false, length = 80, updatable = false)
    private String evento;

    @Column(nullable = false, length = 160, updatable = false)
    private String responsavel;

    @Column(name = "registrado_em", nullable = false, updatable = false)
    private LocalDateTime registradoEm;
}
