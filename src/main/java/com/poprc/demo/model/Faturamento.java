package com.poprc.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "faturamentos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Faturamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "servicos_executados", columnDefinition = "TEXT", nullable = false)
    private String servicosExecutados;

    @Column(name = "valor_medicao", nullable = false)
    private BigDecimal valorMedicao;

    @Column(name = "numero_nota_fiscal")
    private String numeroNotaFiscal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SituacaoFaturamento situacao;

    @Column(name = "data_vencimento")
    private LocalDate dataVencimento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_id", nullable = false)
    @JsonIgnoreProperties("projetos")
    private Contrato contrato;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id")
    @JsonIgnoreProperties({ "materiais", "equipe", "responsavel" })
    private Projeto projeto;
}
