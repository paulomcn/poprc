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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "importacoes_notas_fiscais")
@Data
public class ImportacaoNotaFiscal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nomeArquivo;
    private String arquivoPath;
    private String hashSha256;
    private String tipoArquivo;
    private String chaveAcesso;
    private String numero;
    private String serie;
    private String emitenteNome;
    private String emitenteCnpj;
    private LocalDateTime dataEmissao;
    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal valorTotal = BigDecimal.ZERO;
    private LocalDateTime dataImportacao;
    private String importadoPor;
    private Integer itensProcessados = 0;
    private Integer materiaisCriados = 0;
    private Integer materiaisExistentes = 0;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "funcionario_id")
    private Funcionario funcionario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "local_estoque_id")
    private LocalEstoque localEstoque;
}
