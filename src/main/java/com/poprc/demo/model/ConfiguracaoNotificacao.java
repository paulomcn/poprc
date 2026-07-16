package com.poprc.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "configuracoes_notificacao")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracaoNotificacao {

    @Id
    private Long id = 1L; // Garante que sempre usaremos apenas a linha 1 do banco

    private String emailGestor;
    private String whatsappGestor;
    private boolean alertaOsAtrasada;
    private boolean alertaEstoqueCritico;
    private boolean alertaContratoVencendo;
    private Integer antecedenciaOsHoras = 24;
    private Integer antecedenciaContratoDias = 30;
}
