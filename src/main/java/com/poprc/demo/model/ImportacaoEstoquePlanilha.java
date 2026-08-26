package com.poprc.demo.model;

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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "importacoes_estoque_planilha", uniqueConstraints = {
        @UniqueConstraint(name = "uk_importacao_estoque_hash", columnNames = "hash_sha256")
})
@Data
public class ImportacaoEstoquePlanilha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome_arquivo", nullable = false)
    private String nomeArquivo;

    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String hashSha256;

    @Column(name = "data_importacao", nullable = false)
    private LocalDateTime dataImportacao;

    @Column(name = "importado_por", nullable = false)
    private String importadoPor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "local_estoque_id", nullable = false)
    private LocalEstoque localEstoque;

    @Column(name = "itens_processados", nullable = false)
    private Integer itensProcessados = 0;

    @Column(name = "materiais_criados", nullable = false)
    private Integer materiaisCriados = 0;

    @Column(name = "materiais_atualizados", nullable = false)
    private Integer materiaisAtualizados = 0;

    @Column(name = "ajustes_positivos", nullable = false)
    private Integer ajustesPositivos = 0;

    @Column(name = "ajustes_negativos", nullable = false)
    private Integer ajustesNegativos = 0;

    @Column(name = "valor_total_importado", precision = 19, scale = 2, nullable = false)
    private BigDecimal valorTotalImportado = BigDecimal.ZERO;

    @Column(name = "entradas_importadas", nullable = false)
    private Integer entradasImportadas = 0;

    @Column(name = "abas_retirada_processadas", nullable = false)
    private Integer abasRetiradaProcessadas = 0;

    @Column(name = "retiradas_importadas", nullable = false)
    private Integer retiradasImportadas = 0;

    @Column(name = "faltas_identificadas", nullable = false)
    private Integer faltasIdentificadas = 0;

    @Column(name = "data_complementacao")
    private LocalDateTime dataComplementacao;

    @Column(name = "complementado_por")
    private String complementadoPor;

    @Column(name = "saldo_consolidado", nullable = false)
    private Boolean saldoConsolidado = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "contrato_id")
    private Contrato contrato;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "responsavel_id")
    private Funcionario responsavel;

    @Column(name = "projetos_criados", nullable = false)
    private Integer projetosCriados = 0;

    @Column(name = "ordens_servico_criadas", nullable = false)
    private Integer ordensServicoCriadas = 0;

    @Column(name = "ordens_retirada_criadas", nullable = false)
    private Integer ordensRetiradaCriadas = 0;

    @Column(name = "retornos_importados", nullable = false)
    private Integer retornosImportados = 0;

    @Column(name = "simulacao_faltas", nullable = false)
    private Integer simulacaoFaltas = 0;
}
