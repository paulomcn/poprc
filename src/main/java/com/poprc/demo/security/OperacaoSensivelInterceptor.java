package com.poprc.demo.security;

import com.poprc.demo.model.LogOperacaoSensivel;
import com.poprc.demo.repository.LogOperacaoSensivelRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class OperacaoSensivelInterceptor implements HandlerInterceptor {
    private static final String LOG_REQUEST = "RC_LOG_OPERACAO_SENSIVEL";
    private static final List<String> PREFIXOS_SENSIVEIS = List.of(
            "/api/estoque", "/api/ordens-retirada", "/api/funcionarios", "/api/faturamentos");
    private static final List<String> TRECHOS_SENSIVEIS = List.of(
            "/arquivar", "/restaurar", "/concluir", "/homologar", "/reabrir",
            "/invalidar", "/assinaturas", "/executar", "/devolver");

    private final LogOperacaoSensivelRepository logRepository;

    @Value("${app.security.enabled:true}")
    private boolean securityEnabled;

    @Value("${app.security.reauthentication-minutes:5}")
    private long reauthenticationMinutes;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!securityEnabled || !sensivel(request)) return true;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UsuarioAutenticado usuario)) {
            return exigirSenha(response);
        }

        Object instante = request.getSession(false) == null
                ? null
                : request.getSession(false).getAttribute(SessaoAutenticacaoService.REAUTENTICADO_EM);
        long limite = "DEV".equals(usuario.getMetodoAutenticacao()) ? 480 : reauthenticationMinutes;
        if (!(instante instanceof Instant confirmadoEm)
                || Duration.between(confirmadoEm, Instant.now()).toMinutes() >= limite) {
            return exigirSenha(response);
        }
        request.setAttribute(LOG_REQUEST, usuario);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Object valor = request.getAttribute(LOG_REQUEST);
        if (!(valor instanceof UsuarioAutenticado usuario)) return;
        LogOperacaoSensivel log = new LogOperacaoSensivel();
        log.setFuncionarioId(usuario.getFuncionarioId());
        log.setUsuario(usuario.getEmail() == null ? usuario.getNome() : usuario.getEmail());
        log.setPerfil(usuario.getPerfil());
        log.setMetodoHttp(request.getMethod());
        log.setCaminho(request.getRequestURI());
        log.setStatusHttp(response.getStatus());
        log.setEnderecoIp(request.getRemoteAddr());
        log.setRegistradoEm(LocalDateTime.now());
        logRepository.save(log);
    }

    private boolean sensivel(HttpServletRequest request) {
        String metodo = request.getMethod();
        if ("GET".equals(metodo) || "OPTIONS".equals(metodo)) return false;
        String caminho = request.getRequestURI();
        if (caminho.startsWith("/api/auth/")) return false;
        return "DELETE".equals(metodo)
                || PREFIXOS_SENSIVEIS.stream().anyMatch(caminho::startsWith)
                || TRECHOS_SENSIVEIS.stream().anyMatch(caminho::contains);
    }

    private boolean exigirSenha(HttpServletResponse response) throws Exception {
        response.setStatus(428);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"erro\":\"Confirme sua senha para concluir esta operação.\","
                        + "\"reautenticacaoNecessaria\":true}");
        return false;
    }
}
