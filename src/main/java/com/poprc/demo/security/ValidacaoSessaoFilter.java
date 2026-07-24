package com.poprc.demo.security;

import com.poprc.demo.repository.FuncionarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class ValidacaoSessaoFilter extends OncePerRequestFilter {

    private final FuncionarioRepository funcionarioRepository;

    @Value("${app.security.enabled:true}")
    private boolean securityEnabled;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (securityEnabled && authentication != null
                && authentication.getPrincipal() instanceof UsuarioAutenticado usuario) {
            boolean sessaoValida = funcionarioRepository.findById(usuario.getFuncionarioId())
                    .filter(funcionario -> Boolean.TRUE.equals(funcionario.getAtivo()))
                    .filter(funcionario -> funcionario.getPerfilAcesso().name().equals(usuario.getPerfil()))
                    .isPresent();
            if (!sessaoValida) {
                if (request.getSession(false) != null) request.getSession(false).invalidate();
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(
                        "{\"erro\":\"A conta foi desativada ou teve o perfil alterado. Entre novamente.\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
