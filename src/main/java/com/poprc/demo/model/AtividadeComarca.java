package com.poprc.demo.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "atividades_comarca")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AtividadeComarca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_encerramento")
    private LocalDate dataEncerramento;

    // Campo obrigatório (validado também no Service) - detalhamento do que foi
    // feito na comarca
    @Column(name = "descricao_atividades", columnDefinition = "TEXT", nullable = false)
    private String descricaoAtividades;

    // Lista de caminhos/URLs das fotos de evidência (tabela relacionada
    // atividade_comarca_fotos)
    @ElementCollection
    @CollectionTable(name = "atividade_comarca_fotos", joinColumns = @JoinColumn(name = "atividade_comarca_id"))
    @Column(name = "url_foto", columnDefinition = "TEXT")
    private List<String> fotosEvidencia = new ArrayList<>();

    @Column(name = "data_registro")
    private LocalDateTime dataRegistro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comarca_id", nullable = false)
    @JsonIgnore // não precisamos devolver a comarca inteira; o front já sabe o comarcaId pelo
                // contexto
    private Comarca comarca;
}