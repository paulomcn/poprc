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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "documentos_assinaturas_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoAssinaturaLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documento_id", nullable = false, updatable = false)
    private DocumentoInterno documento;

    @Column(nullable = false, updatable = false, length = 40)
    private String papel;

    @Column(name = "nome_assinante", nullable = false, updatable = false)
    private String nomeAssinante;

    @Column(name = "registrado_por", nullable = false, updatable = false)
    private String registradoPor;

    @Column(name = "registrado_em", nullable = false, updatable = false)
    private LocalDateTime registradoEm;

    @Column(name = "hash_assinatura", nullable = false, updatable = false, length = 64)
    private String hashAssinatura;

    @Column(name = "hash_documento", nullable = false, updatable = false, length = 64)
    private String hashDocumento;
}
