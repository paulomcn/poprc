package com.poprc.demo.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${alerts.email.enabled:false}")
    private boolean habilitado;

    @Value("${alerts.email.from:sistema@poprc.com}")
    private String remetente;

    /**
     * Dispara um e-mail simples de alerta para a diretoria/gestão
     */
    public ResultadoEnvio enviarEmailAlerta(String para, String assunto, String conteudo) {
        if (!habilitado) {
            return new ResultadoEnvio(StatusEnvio.DESABILITADO, "Canal de e-mail desabilitado.");
        }
        if (para == null || para.isBlank()) {
            return new ResultadoEnvio(StatusEnvio.NAO_CONFIGURADO, "Destinatário não configurado.");
        }

        try {
            SimpleMailMessage mensagem = new SimpleMailMessage();
            mensagem.setTo(para.trim());
            mensagem.setSubject(assunto);
            mensagem.setText(conteudo);
            mensagem.setFrom(remetente);

            mailSender.send(mensagem);
            log.info("Alerta enviado por e-mail para {}", para);
            return new ResultadoEnvio(StatusEnvio.ENVIADO, "E-mail enviado.");
        } catch (Exception e) {
            log.error("Falha ao enviar alerta por e-mail para {}", para, e);
            return new ResultadoEnvio(StatusEnvio.FALHA, e.getMessage());
        }
    }

    public boolean isHabilitado() {
        return habilitado;
    }

    public String getRemetente() {
        return remetente;
    }

    public enum StatusEnvio {
        ENVIADO,
        DESABILITADO,
        NAO_CONFIGURADO,
        FALHA
    }

    public record ResultadoEnvio(StatusEnvio status, String detalhe) {
    }
}
