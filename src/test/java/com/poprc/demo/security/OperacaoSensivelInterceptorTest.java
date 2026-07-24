package com.poprc.demo.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.poprc.demo.model.LogOperacaoSensivel;
import com.poprc.demo.repository.LogOperacaoSensivelRepository;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class OperacaoSensivelInterceptorTest {
    private LogOperacaoSensivelRepository logRepository;
    private OperacaoSensivelInterceptor interceptor;

    @BeforeEach
    void configurar() {
        logRepository = mock(LogOperacaoSensivelRepository.class);
        interceptor = new OperacaoSensivelInterceptor(
                logRepository, new PoliticaOperacaoSensivel());
        ReflectionTestUtils.setField(interceptor, "securityEnabled", true);
        ReflectionTestUtils.setField(interceptor, "reauthenticationMinutes", 5L);
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void exigeSenhaQuandoConfirmacaoExpirou() throws Exception {
        autenticar();
        MockHttpServletRequest request = requisicaoSensivel();
        request.getSession().setAttribute(
                SessaoAutenticacaoService.REAUTENTICADO_EM,
                Instant.now().minusSeconds(6 * 60));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean permitido = interceptor.preHandle(request, response, new Object());

        assertThat(permitido).isFalse();
        assertThat(response.getStatus()).isEqualTo(428);
        assertThat(response.getContentAsString()).contains("\"reautenticacaoNecessaria\":true");
    }

    @Test
    void liberaConfirmacaoRecenteERegistraResultado() throws Exception {
        autenticar();
        MockHttpServletRequest request = requisicaoSensivel();
        request.setRemoteAddr("192.168.0.20");
        request.getSession().setAttribute(
                SessaoAutenticacaoService.REAUTENTICADO_EM, Instant.now());
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
        response.setStatus(201);
        interceptor.afterCompletion(request, response, new Object(), null);

        ArgumentCaptor<LogOperacaoSensivel> captor =
                ArgumentCaptor.forClass(LogOperacaoSensivel.class);
        verify(logRepository).save(captor.capture());
        LogOperacaoSensivel log = captor.getValue();
        assertThat(log.getFuncionarioId()).isEqualTo(10L);
        assertThat(log.getMetodoHttp()).isEqualTo("POST");
        assertThat(log.getCaminho()).isEqualTo("/api/estoque/saida");
        assertThat(log.getEnderecoIp()).isEqualTo("192.168.0.20");
        assertThat(log.getStatusHttp()).isEqualTo(201);
    }

    private void autenticar() {
        UsuarioAutenticado usuario = new UsuarioAutenticado(
                10L, "Usuario Teste", null, "ESTOQUE", "CPF_SENHA", false);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        usuario, null, usuario.getAuthorities()));
    }

    private MockHttpServletRequest requisicaoSensivel() {
        return new MockHttpServletRequest("POST", "/api/estoque/saida");
    }
}
