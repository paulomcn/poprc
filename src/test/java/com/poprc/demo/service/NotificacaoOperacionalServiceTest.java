package com.poprc.demo.service;

import com.poprc.demo.model.NotificacaoOperacional;
import com.poprc.demo.repository.NotificacaoOperacionalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
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
        when(repository.findFirstByChaveBaseAndResolvidaEmIsNull("OS_ATRASADA:10"))
                .thenReturn(Optional.empty());
        when(repository.existsByChave("OS_ATRASADA:10")).thenReturn(false);

        boolean criada = service.registrarSeAusente(
                "OS_ATRASADA:10", "OS_ATRASADA", "CRITICA", "OS atrasada", "Prazo vencido", null, null);

        assertTrue(criada);
        verify(repository).save(org.mockito.ArgumentMatchers.any(NotificacaoOperacional.class));
    }

    @Test
    void naoDeveDuplicarNotificacaoComMesmaChave() {
        NotificacaoOperacional ativa = new NotificacaoOperacional();
        when(repository.findFirstByChaveBaseAndResolvidaEmIsNull("OS_ATRASADA:10"))
                .thenReturn(Optional.of(ativa));

        boolean criada = service.registrarSeAusente(
                "OS_ATRASADA:10", "OS_ATRASADA", "CRITICA", "OS atrasada", "Prazo vencido", null, null);

        assertFalse(criada);
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deveResolverNotificacaoQuandoCondicaoDeixarDeExistir() {
        NotificacaoOperacional notificacao = new NotificacaoOperacional();
        notificacao.setChaveBase("ESTOQUE_CRITICO:10:20");
        when(repository.findAllByTipoAndResolvidaEmIsNull("ESTOQUE_CRITICO"))
                .thenReturn(java.util.List.of(notificacao));

        int quantidade = service.resolverAusentesDoTipo(
                "ESTOQUE_CRITICO", Set.of(), "Saldo normalizado.");

        assertEquals(1, quantidade);
        assertNotNull(notificacao.getResolvidaEm());
        assertEquals("Saldo normalizado.", notificacao.getMotivoResolucao());
        verify(repository).saveAll(anyList());
    }

    @Test
    void naoDeveResolverNotificacaoQueContinuaAtiva() {
        NotificacaoOperacional notificacao = new NotificacaoOperacional();
        notificacao.setChaveBase("ESTOQUE_CRITICO:10:20");
        when(repository.findAllByTipoAndResolvidaEmIsNull("ESTOQUE_CRITICO"))
                .thenReturn(java.util.List.of(notificacao));

        int quantidade = service.resolverAusentesDoTipo(
                "ESTOQUE_CRITICO", Set.of("ESTOQUE_CRITICO:10:20"), "Saldo normalizado.");

        assertEquals(0, quantidade);
        assertNull(notificacao.getResolvidaEm());
        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void deveCriarNovaOcorrenciaDepoisDaResolucaoAnterior() {
        String chaveBase = "OS_ATRASADA:10";
        when(repository.findFirstByChaveBaseAndResolvidaEmIsNull(chaveBase))
                .thenReturn(Optional.empty());
        when(repository.existsByChave(chaveBase)).thenReturn(true);
        ArgumentCaptor<NotificacaoOperacional> captor = ArgumentCaptor.forClass(NotificacaoOperacional.class);

        boolean criada = service.registrarSeAusente(
                chaveBase, "OS_ATRASADA", "CRITICA", "OS atrasada", "Prazo vencido", null, null);

        assertTrue(criada);
        verify(repository).save(captor.capture());
        assertEquals(chaveBase, captor.getValue().getChaveBase());
        assertTrue(captor.getValue().getChave().startsWith(chaveBase + ":OCORRENCIA:"));
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
