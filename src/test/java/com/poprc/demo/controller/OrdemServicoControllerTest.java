package com.poprc.demo.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.poprc.demo.model.OrdemServico;
import com.poprc.demo.model.StatusOS;
import com.poprc.demo.repository.OrdemServicoRepository;
import com.poprc.demo.security.UsuarioAutenticado;
import com.poprc.demo.service.AcessoOperacionalService;
import com.poprc.demo.service.ArquivamentoService;
import com.poprc.demo.service.FluxoOrdemServicoService;
import com.poprc.demo.service.OrdemServicoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class OrdemServicoControllerTest {

    private OrdemServicoService ordemServicoService;
    private AcessoOperacionalService acessoOperacionalService;
    private OrdemServicoController controller;

    @BeforeEach
    void setUp() {
        ordemServicoService = mock(OrdemServicoService.class);
        acessoOperacionalService = mock(AcessoOperacionalService.class);
        controller = new OrdemServicoController(
                ordemServicoService,
                mock(OrdemServicoRepository.class),
                mock(ArquivamentoService.class),
                mock(FluxoOrdemServicoService.class),
                acessoOperacionalService);
    }

    @Test
    void tecnicoPodeSomenteEnviarExecucaoParaValidacao() {
        OrdemServicoController.StatusUpdateRequest request = requisicao(StatusOS.AGUARDANDO_DEVOLUCAO);

        assertThrows(AccessDeniedException.class,
                () -> controller.atualizarStatus(1L, request, autenticacao("TECNICO", "Tecnico Teste")));

        verifyNoInteractions(ordemServicoService, acessoOperacionalService);
    }

    @Test
    void responsavelDoHistoricoVemDaSessaoAutenticada() {
        OrdemServicoController.StatusUpdateRequest request = requisicao(StatusOS.AGUARDANDO_VALIDACAO);
        request.setResponsavel("Nome informado pelo cliente");
        OrdemServico ordem = new OrdemServico();
        when(ordemServicoService.atualizarStatus(1L, StatusOS.AGUARDANDO_VALIDACAO, "Tecnico Teste"))
                .thenReturn(ordem);
        Authentication authentication = autenticacao("TECNICO", "Tecnico Teste");

        ResponseEntity<OrdemServico> response = controller.atualizarStatus(
                1L, request, authentication);

        assertEquals(200, response.getStatusCode().value());
        verify(acessoOperacionalService).garantirAcessoOrdem(
                1L, authentication);
        verify(ordemServicoService).atualizarStatus(
                1L, StatusOS.AGUARDANDO_VALIDACAO, "Tecnico Teste");
    }

    private OrdemServicoController.StatusUpdateRequest requisicao(StatusOS status) {
        OrdemServicoController.StatusUpdateRequest request = new OrdemServicoController.StatusUpdateRequest();
        request.setStatus(status);
        return request;
    }

    private Authentication autenticacao(String perfil, String nome) {
        UsuarioAutenticado usuario = new UsuarioAutenticado(
                1L, nome, null, perfil, "CPF_SENHA", false);
        return new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
    }
}
