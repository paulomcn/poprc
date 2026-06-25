package com.poprc.demo.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender; // O AJUSTE TA AQUI (.javamail)
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Dispara um e-mail simples de alerta para a diretoria/gestão
     */
    public void enviarEmailAlerta(String para, String assunto, String conteudo) {
        try {
            SimpleMailMessage mensagem = new SimpleMailMessage();
            mensagem.setTo(para);
            mensagem.setSubject(assunto);
            mensagem.setText(conteudo);
            mensagem.setFrom("sistema@poprc.com");

            mailSender.send(mensagem);
            System.out.println(" Alerta enviado com sucesso para: " + para);
        } catch (Exception e) {
            System.err.println("❌ Falha crítica ao disparar e-mail de alerta: " + e.getMessage());
        }
    }
}