package com.poprc.demo.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PoliticaOperacaoSensivelTest {
    private final PoliticaOperacaoSensivel politica = new PoliticaOperacaoSensivel();

    @Test
    void protegeMovimentacoesAssinaturasEHomologacoes() {
        assertThat(politica.exigeReautenticacao("POST", "/api/estoque/saida")).isTrue();
        assertThat(politica.exigeReautenticacao(
                "PATCH", "/api/documentos-internos/10/assinar")).isTrue();
        assertThat(politica.exigeReautenticacao(
                "PATCH", "/api/comarcas/4/as-built/homologar")).isTrue();
        assertThat(politica.exigeReautenticacao(
                "PUT", "/api/comarcas/materiais/7/auditoria")).isTrue();
        assertThat(politica.exigeReautenticacao(
                "PATCH", "/api/as-built/2/nova-versao")).isTrue();
    }

    @Test
    void protegeExclusaoMesmoForaDasRotasCatalogadas() {
        assertThat(politica.exigeReautenticacao(
                "DELETE", "/api/campo/evidencias/15")).isTrue();
    }

    @Test
    void permiteConsultasEAtualizacoesOperacionaisComuns() {
        assertThat(politica.exigeReautenticacao("GET", "/api/estoque/materiais")).isFalse();
        assertThat(politica.exigeReautenticacao(
                "PUT", "/api/ordens-servico/3/checklist")).isFalse();
        assertThat(politica.exigeReautenticacao(
                "POST", "/api/auth/reauth")).isFalse();
    }
}
