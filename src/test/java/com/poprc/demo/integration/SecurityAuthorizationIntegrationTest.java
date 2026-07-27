package com.poprc.demo.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.poprc.demo.DemoApplication;
import com.poprc.demo.model.Funcionario;
import com.poprc.demo.model.PerfilAcesso;
import com.poprc.demo.repository.FuncionarioRepository;
import com.poprc.demo.security.UsuarioAutenticado;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = DemoApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.security.enabled=true",
        "app.security.dev-login-enabled=false"
})
@Transactional
class SecurityAuthorizationIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Test
    void apiSemSessaoRetornaNaoAutorizado() throws Exception {
        int status = mockMvc.perform(get("/api/estoque/materiais"))
                .andReturn().getResponse().getStatus();
        assertThat(status).isEqualTo(401);
    }

    @Test
    void tecnicoNaoPodeMovimentarEstoque() throws Exception {
        int status = mockMvc.perform(post("/api/estoque/entrada")
                        .with(user("tecnico").roles("TECNICO"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn().getResponse().getStatus();
        assertThat(status).isEqualTo(403);
    }

    @Test
    void perfilEstoquePassaPelaCamadaDeAutorizacao() throws Exception {
        int status = mockMvc.perform(post("/api/estoque/entrada")
                        .with(user("estoquista").roles("ESTOQUE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn().getResponse().getStatus();
        assertThat(status).isNotEqualTo(401).isNotEqualTo(403);
    }

    @Test
    void tecnicoNaoPodeCriarOrdemDeServico() throws Exception {
        int status = mockMvc.perform(post("/api/ordens-servico")
                        .with(user("tecnico").roles("TECNICO"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn().getResponse().getStatus();
        assertThat(status).isEqualTo(403);
    }

    @Test
    void tecnicoPodeConsultarAtividadesPadrao() throws Exception {
        int status = mockMvc.perform(get("/api/atividades-padrao")
                        .with(user("tecnico").roles("TECNICO")))
                .andReturn().getResponse().getStatus();
        assertThat(status).isNotEqualTo(401).isNotEqualTo(403);
    }

    @Test
    void auditorNaoPodeAlterarFluxoOperacionalDaObra() throws Exception {
        int status = mockMvc.perform(patch("/api/comarcas/1/avancar-etapa")
                        .with(user("auditor").roles("AUDITOR"))
                        .with(csrf()))
                .andReturn().getResponse().getStatus();
        assertThat(status).isEqualTo(403);
    }

    @Test
    void estoquePodeConsultarComarcasParaRastreabilidade() throws Exception {
        int status = mockMvc.perform(get("/api/comarcas")
                        .with(user("estoque").roles("ESTOQUE")))
                .andReturn().getResponse().getStatus();
        assertThat(status).isNotEqualTo(401).isNotEqualTo(403);
    }

    @Test
    void auditorNaoPodeRegistrarOperacoesDeCampo() throws Exception {
        int status = mockMvc.perform(post("/api/campo/ponto")
                        .with(user("auditor").roles("AUDITOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn().getResponse().getStatus();
        assertThat(status).isEqualTo(403);
    }

    @Test
    void tecnicoNaoPodeAbrirComprovanteFinanceiroDiretamente() throws Exception {
        mockMvc.perform(get("/uploads/financeiro/comprovantes/nota-fiscal.pdf")
                        .with(user("tecnico").roles("TECNICO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void estoqueNaoPodeAbrirEvidenciaDeCampoDiretamente() throws Exception {
        mockMvc.perform(get("/uploads/evidencias/vistoria.png")
                        .with(user("estoquista").roles("ESTOQUE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void tecnicoNaoPodeContornarEscopoUsandoUrlEstaticaDeEvidencia() throws Exception {
        mockMvc.perform(get("/uploads/evidencias/vistoria.png")
                        .with(user("tecnico").roles("TECNICO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void tecnicoNaoPodeContornarEscopoUsandoUrlEstaticaDeDocumento() throws Exception {
        mockMvc.perform(get("/uploads/documentos/ordens-retirada/or.pdf")
                        .with(user("tecnico").roles("TECNICO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void estoqueNaoPodeAbrirFotoProtegidaDaVistoria() throws Exception {
        mockMvc.perform(get("/api/comarcas/1/vistoria/foto")
                        .with(user("estoquista").roles("ESTOQUE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void estoquePodeConsultarFilaOperacionalDaSuaArea() throws Exception {
        mockMvc.perform(get("/api/pendencias-operacionais")
                        .param("area", "ADMINISTRACAO")
                        .with(authentication(autenticacao("ESTOQUE"))))
                .andExpect(status().isOk());
    }

    @Test
    void tecnicoPodeConsultarFilaLimitadaAsSuasOrdens() throws Exception {
        mockMvc.perform(get("/api/pendencias-operacionais")
                        .param("funcionarioId", "999")
                        .with(authentication(autenticacao("TECNICO"))))
                .andExpect(status().isOk());
    }

    @Test
    void operacaoCriticaSemConfirmacaoRecenteSolicitaSenha() throws Exception {
        mockMvc.perform(post("/api/funcionarios")
                        .with(user("administrador").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is(428))
                .andExpect(jsonPath("$.reautenticacaoNecessaria").value(true));
    }

    private UsernamePasswordAuthenticationToken autenticacao(String perfil) {
        Funcionario funcionario = new Funcionario();
        funcionario.setNome("Usuário " + perfil);
        funcionario.setFuncao("Homologação");
        funcionario.setCidade("João Pessoa");
        funcionario.setPerfilAcesso(PerfilAcesso.valueOf(perfil));
        funcionario = funcionarioRepository.saveAndFlush(funcionario);
        UsuarioAutenticado usuario = new UsuarioAutenticado(
                funcionario.getId(), funcionario.getNome(), null, perfil, "CPF_SENHA", false);
        return new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
    }
}
