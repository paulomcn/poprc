package com.poprc.demo.service;

import com.poprc.demo.model.NotificacaoOperacional;
import com.poprc.demo.repository.NotificacaoOperacionalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificacaoOperacionalServiceTest {

    private NotificacaoOperacionalRepository repository;
    private NotificacaoOperacionalService service;

    @BeforeEach
    void setUp() {
        repository = mock(NotificacaoOperacionalRepository.class);
        service = new NotificacaoOperacionalService(repository);
    }

    @Test
    void deveCriarNotificacaoQuandoChaveAindaNaoExiste() {
        when(repository.existsByChave("OS_ATRASADA:10")).thenReturn(false);

        boolean criada = service.registrarSeAusente(
                "OS_ATRASADA:10", "OS_ATRASADA", "CRITICA", "OS atrasada", "Prazo vencido", null, null);

        assertTrue(criada);
        verify(repository).save(org.mockito.ArgumentMatchers.any(NotificacaoOperacional.class));
    }

    @Test
    void naoDeveDuplicarNotificacaoComMesmaChave() {
        when(repository.existsByChave("OS_ATRASADA:10")).thenReturn(true);

        boolean criada = service.registrarSeAusente(
                "OS_ATRASADA:10", "OS_ATRASADA", "CRITICA", "OS atrasada", "Prazo vencido", null, null);

        assertFalse(criada);
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deveRegistrarMomentoDaLeitura() {
        NotificacaoOperacional notificacao = new NotificacaoOperacional();
        notificacao.setId(3L);
        when(repository.findById(3L)).thenReturn(Optional.of(notificacao));
        when(repository.save(notificacao)).thenReturn(notificacao);

        NotificacaoOperacional atualizada = service.marcarComoLida(3L);

        assertNotNull(atualizada.getLidaEm());
        verify(repository).save(notificacao);
    }
}
