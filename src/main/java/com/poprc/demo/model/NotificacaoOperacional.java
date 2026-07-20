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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "notificacoes_operacionais",
        uniqueConstraints = @UniqueConstraint(name = "uk_notificacao_chave", columnNames = "chave"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificacaoOperacional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String chave;

    @Column(name = "chave_base", nullable = false, length = 180)
    private String chaveBase;

    @Column(nullable = false, length = 60)
    private String tipo;

    @Column(nullable = false, length = 20)
    private String severidade;

    @Column(nullable = false, length = 180)
    private String titulo;

    @Column(nullable = false, length = 2000)
    private String mensagem;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    @Column(name = "lida_em")
    private LocalDateTime lidaEm;

    @Column(name = "resolvida_em")
    private LocalDateTime resolvidaEm;

    @Column(name = "motivo_resolucao", length = 500)
    private String motivoResolucao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordem_servico_id")
    private OrdemServico ordemServico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destinatario_id")
    private Funcionario destinatario;
}
