package com.poprc.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "viagens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Viagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "solicitacao_veiculo")
    private String solicitacaoVeiculo;

    @Column(name = "hospedagem_detalhes")
    private String hospedagemDetalhes;

    @Column(name = "adiantamento_diarias")
    private BigDecimal adiantamentoDiarias;

    @Column(name = "custo_planejado")
    private BigDecimal custoPlanejado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusViagem status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id", nullable = false)
    @JsonIgnoreProperties({"certificacoes", "documentPaths"}) // Corta loops pesados do funcionário
    private Funcionario funcionario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", nullable = false)
    @JsonIgnoreProperties({"contrato", "atividades", "ordensServico"}) // 💥 CORTA O LOOP INFINITO AQUI
    private Projeto projeto;
}