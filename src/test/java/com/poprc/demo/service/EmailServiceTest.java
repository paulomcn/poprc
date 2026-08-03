package com.poprc.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class EmailServiceTest {

    private JavaMailSender mailSender;
    private EmailService service;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        service = new EmailService(mailSender);
        ReflectionTestUtils.setField(service, "remetente", "alertas@empresa.com");
    }

    @Test
    void naoDeveSimularEnvioQuandoCanalEstiverDesabilitado() {
        ReflectionTestUtils.setField(service, "habilitado", false);

        EmailService.ResultadoEnvio resultado = service.enviarEmailAlerta(
                "gestor@empresa.com", "Prazo", "Mensagem");

        assertEquals(EmailService.StatusEnvio.DESABILITADO, resultado.status());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void deveInformarEnvioRealQuandoSmtpAceitarMensagem() {
        ReflectionTestUtils.setField(service, "habilitado", true);

        EmailService.ResultadoEnvio resultado = service.enviarEmailAlerta(
                "gestor@empresa.com", "Prazo", "Mensagem");

        assertEquals(EmailService.StatusEnvio.ENVIADO, resultado.status());
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void deveExporFalhaDoServidorSmtp() {
        ReflectionTestUtils.setField(service, "habilitado", true);
        doThrow(new IllegalStateException("SMTP indisponível"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        EmailService.ResultadoEnvio resultado = service.enviarEmailAlerta(
                "gestor@empresa.com", "Prazo", "Mensagem");

        assertEquals(EmailService.StatusEnvio.FALHA, resultado.status());
    }
}
