package com.poprc.demo.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.poprc.demo.DemoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = DemoApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.security.enabled=true",
        "app.security.dev-login-enabled=false"
})
class SecurityAuthorizationIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

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
}
