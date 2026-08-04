package com.poprc.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.poprc.demo.model.LogOperacaoSensivel;
import com.poprc.demo.repository.LogOperacaoSensivelRepository;
import com.poprc.demo.security.UsuarioAutenticado;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class AuditoriaAcessoServiceTest {

    @Test
    void registraAtorAlvoEAlteracaoSemGuardarCredenciais() {
        LogOperacaoSensivelRepository repository = mock(LogOperacaoSensivelRepository.class);
        AuditoriaAcessoService service = new AuditoriaAcessoService(repository);
        UsuarioAutenticado usuario = new UsuarioAutenticado(
                7L, "Administrador", "admin@empresa.com", "ADMIN", "CPF_SENHA", false);
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                usuario, null, usuario.getAuthorities());

        service.registrar(authentication, "PERFIL_ALTERADO", 22L,
                "Perfil: TECNICO -> ESTOQUE");

        ArgumentCaptor<LogOperacaoSensivel> captor =
                ArgumentCaptor.forClass(LogOperacaoSensivel.class);
        verify(repository).save(captor.capture());
        LogOperacaoSensivel log = captor.getValue();
        assertThat(log.getFuncionarioId()).isEqualTo(7L);
        assertThat(log.getAlvoFuncionarioId()).isEqualTo(22L);
        assertThat(log.getTipoEvento()).isEqualTo("ACESSO_PERFIL_ALTERADO");
        assertThat(log.getDetalhes()).isEqualTo("Perfil: TECNICO -> ESTOQUE");
        assertThat(log.getDetalhes()).doesNotContainIgnoringCase("senha");
    }
}
