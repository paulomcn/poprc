package com.poprc.demo.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
public class SessaoAutenticacaoService {
    public static final String REAUTENTICADO_EM = "RC_REAUTENTICADO_EM";
    public static final String METODO_AUTENTICACAO = "RC_METODO_AUTENTICACAO";

    private final HttpSessionSecurityContextRepository repository = new HttpSessionSecurityContextRepository();

    public void autenticar(UsuarioAutenticado usuario, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                usuario, null, usuario.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        repository.saveContext(context, request, response);
        request.getSession().setAttribute(METODO_AUTENTICACAO, usuario.getMetodoAutenticacao());
        request.getSession().setAttribute(REAUTENTICADO_EM, Instant.now());
    }

    public void marcarReautenticacao(HttpServletRequest request) {
        request.getSession().setAttribute(REAUTENTICADO_EM, Instant.now());
    }
}
