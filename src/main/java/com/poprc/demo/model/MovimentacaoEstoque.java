package com.poprc.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "movimentacoes_estoque")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovimentacaoEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer quantidade;

    @Column(precision = 14, scale = 3)
    private BigDecimal metragem;

    @Column(precision = 14, scale = 3)
    private BigDecimal saldoAnterior;

    @Column(precision = 14, scale = 3)
    private BigDecimal saldoPosterior;

    @Enumerated(EnumType.STRING)
    private UnidadeMedida unidadeMedida;

    @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime dataMovimentacao;

    @Enumerated(EnumType.STRING)
    private TipoMovimentacao tipo;

    @Column(columnDefinition = "TEXT")
    private String observacao;

    private String motivo;

    private String estoqueOrigem;

    private String estoqueDestino;

    private String autorizadoPor;

    private String retiradoPor;

    private String lancadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id")
    private Material material;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_rastreavel_id")
    @JsonIgnoreProperties({ "material", "hibernateLazyInitializer", "handler" })
    private UnidadeEstoqueRastreavel unidadeRastreavel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id")
    private Funcionario funcionario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id")
    @JsonIgnoreProperties({ "materiais", "contrato", "responsavel", "hibernateLazyInitializer", "handler" })
    private Projeto projeto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordem_servico_id")
    @JsonIgnoreProperties({ "contrato", "projeto", "hibernateLazyInitializer", "handler" })
    private OrdemServico ordemServico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordem_retirada_id")
    @JsonIgnoreProperties({ "ordemServico", "comarca", "itens", "hibernateLazyInitializer", "handler" })
    private OrdemRetirada ordemRetirada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comarca_id")
    @JsonIgnoreProperties({ "materiais", "projeto", "ordemServico", "hibernateLazyInitializer", "handler" })
    private Comarca comarca;
}
