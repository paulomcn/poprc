package com.poprc.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "prestacoes_contas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrestacaoContas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "caminho_nota_fiscal", nullable = false)
    private String caminhoNotaFiscal;

    @Column(name = "custo_real", nullable = false)
    private BigDecimal custoReal;

    @Column(nullable = false)
    private String status; // EXCEDIDO ou DENTRO_DO_ORCAMENTO

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viagem_id", nullable = false, unique = true)
    private Viagem viagem;
}