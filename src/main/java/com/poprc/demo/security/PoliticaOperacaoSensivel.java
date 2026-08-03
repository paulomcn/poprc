package com.poprc.demo.security;

import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

@Component
public class PoliticaOperacaoSensivel {
    private static final List<String> ROTAS_SENSIVEIS = List.of(
            "/api/estoque/**",
            "/api/ordens-retirada/**",
            "/api/funcionarios/**",
            "/api/faturamentos/**",
            "/api/as-built/**",
            "/api/comarcas/*/as-built/**",
            "/api/projetos/*/as-built/**",
            "/api/comarcas/materiais/*/auditoria",
            "/api/documentos-internos/*/invalidar",
            "/api/documentos-internos/*/assinar",
            "/api/documentos-internos/*/assinaturas/**",
            "/api/contratos/*/arquivar",
            "/api/contratos/*/restaurar",
            "/api/projetos/*/arquivar",
            "/api/projetos/*/restaurar",
            "/api/ordens-servico/*/arquivar",
            "/api/ordens-servico/*/restaurar",
            "/api/ordens-servico/reparar-vinculos-comarcas",
            "/api/comarcas/*/arquivar",
            "/api/comarcas/*/restaurar",
            "/api/comarcas/*/concluir",
            "/api/alertas/configuracoes",
            "/api/alertas/disparar-todos");

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public boolean exigeReautenticacao(String metodo, String caminho) {
        if (metodo == null || caminho == null || metodoSeguro(metodo)
                || caminho.startsWith("/api/auth/")) {
            return false;
        }
        if ("DELETE".equalsIgnoreCase(metodo)) {
            return true;
        }
        return ROTAS_SENSIVEIS.stream().anyMatch(padrao -> pathMatcher.match(padrao, caminho));
    }

    private boolean metodoSeguro(String metodo) {
        return "GET".equalsIgnoreCase(metodo)
                || "HEAD".equalsIgnoreCase(metodo)
                || "OPTIONS".equalsIgnoreCase(metodo);
    }
}
