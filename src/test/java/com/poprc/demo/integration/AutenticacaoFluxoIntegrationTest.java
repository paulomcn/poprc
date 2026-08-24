package com.poprc.demo.integration;

import com.poprc.demo.DemoApplication;
import com.poprc.demo.model.Funcionario;
import com.poprc.demo.model.PerfilAcesso;
import com.poprc.demo.repository.FuncionarioRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.SessionRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = DemoApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.security.enabled=true",
        "app.security.dev-login-enabled=false",
        "app.security.zoho-enabled=false"
})
class AutenticacaoFluxoIntegrationTest {

    private static final String CPF = "11144477735";
    private static final String CPF_ESTOQUE = "52998224725";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ApplicationContext applicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Long funcionarioId;
    private Long funcionarioEstoqueId;

    @BeforeEach
    void criarUsuario() {
        funcionarioRepository.findByCpf(CPF).ifPresent(funcionarioRepository::delete);
        funcionarioRepository.findByCpf(CPF_ESTOQUE).ifPresent(funcionarioRepository::delete);
        Funcionario funcionario = new Funcionario();
        funcionario.setNome("Administrador Autenticacao");
        funcionario.setFuncao("Administrador");
        funcionario.setCidade("Natal");
        funcionario.setCpf(CPF);
        funcionario.setPerfilAcesso(PerfilAcesso.ADMIN);
        funcionario.setAtivo(true);
        funcionario.setSenhaHash(passwordEncoder.encode("Temporaria123"));
        funcionario.setTrocaSenhaObrigatoria(true);
        funcionarioId = funcionarioRepository.saveAndFlush(funcionario).getId();
    }

    @AfterEach
    void removerUsuario() {
        if (funcionarioId != null) {
            funcionarioRepository.findById(funcionarioId).ifPresent(funcionarioRepository::delete);
        }
        if (funcionarioEstoqueId != null) {
            funcionarioRepository.findById(funcionarioEstoqueId).ifPresent(funcionarioRepository::delete);
        }
    }

    @Test
    void loginCriaSessaoPersistidaELogoutRevogaAcesso() throws Exception {
        Cookie sessao = login("Temporaria123");

        mockMvc.perform(get("/api/auth/me").cookie(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.funcionarioId").value(funcionarioId))
                .andExpect(jsonPath("$.trocaSenhaObrigatoria").value(true));

        mockMvc.perform(post("/api/auth/logout").cookie(sessao).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me").cookie(sessao))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void primeiraTrocaPermiteNovoLoginComSenhaDefinitiva() throws Exception {
        Cookie sessao = login("Temporaria123");

        mockMvc.perform(post("/api/auth/alterar-senha")
                        .cookie(sessao)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "senhaAtual": "",
                                  "novaSenha": "Definitiva123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trocaSenhaObrigatoria").value(false))
                .andExpect(jsonPath("$.senhaConfigurada").value(true));

        mockMvc.perform(post("/api/auth/logout").cookie(sessao).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cpf": "111.444.777-35",
                                  "senha": "Definitiva123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trocaSenhaObrigatoria").value(false));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cpf": "111.444.777-35",
                                  "senha": "Temporaria123"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void configuracaoNaoAnunciaZohoSemCredenciaisHabilitadas() throws Exception {
        mockMvc.perform(get("/api/auth/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.securityEnabled").value(true))
                .andExpect(jsonPath("$.devLoginEnabled").value(false))
                .andExpect(jsonPath("$.zohoEnabled").value(false));

        mockMvc.perform(get("/oauth2/authorization/zoho"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aplicacaoUsaRepositorioJdbcParaPersistirSessoes() {
        assertThat(applicationContext.getBeansOfType(SessionRepository.class).values())
                .anyMatch(repository ->
                        repository.getClass().getName().contains("JdbcIndexedSessionRepository"));
    }

    @Test
    void desativacaoDoFuncionarioRevogaSessaoExistente() throws Exception {
        Cookie sessao = login("Temporaria123");
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId).orElseThrow();
        funcionario.setAtivo(false);
        funcionarioRepository.saveAndFlush(funcionario);

        mockMvc.perform(get("/api/auth/me").cookie(sessao))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value(
                        "A conta foi desativada ou teve o perfil alterado. Entre novamente."));
    }

    @Test
    void tokenCsrfExpostoNoEndpointAutorizaLogoutReal() throws Exception {
        Cookie sessao = login("Temporaria123");
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf").cookie(sessao))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode csrfBody = objectMapper.readTree(csrfResult.getResponse().getContentAsString());
        Cookie csrfCookie = new Cookie("XSRF-TOKEN", csrfBody.get("token").asText());

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(sessao, csrfCookie)
                        .header(csrfBody.get("headerName").asText(), csrfBody.get("token").asText()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me").cookie(sessao))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bancoVazioPermiteCriarSomenteOPrimeiroAdministrador() throws Exception {
        funcionarioRepository.deleteById(funcionarioId);
        funcionarioId = null;

        mockMvc.perform(get("/api/auth/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bootstrapRequired").value(true));

        MvcResult resultado = mockMvc.perform(post("/api/auth/bootstrap")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Primeiro Administrador",
                                  "cpf": "111.444.777-35",
                                  "senha": "Definitiva123",
                                  "cidade": "Natal"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.perfil").value("ADMIN"))
                .andExpect(jsonPath("$.trocaSenhaObrigatoria").value(false))
                .andReturn();
        assertThat(resultado.getResponse().getCookie("SESSION")).isNotNull();
        funcionarioId = funcionarioRepository.findByCpf(CPF).orElseThrow().getId();

        mockMvc.perform(get("/api/auth/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bootstrapRequired").value(false));

        mockMvc.perform(post("/api/auth/bootstrap")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Segundo Administrador",
                                  "cpf": "529.982.247-25",
                                  "senha": "Definitiva123",
                                  "cidade": "Natal"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void duasSessoesDeUsuariosDiferentesPermanecemIndependentes() throws Exception {
        Funcionario estoque = new Funcionario();
        estoque.setNome("Operador Estoque Simultaneo");
        estoque.setFuncao("Almoxarife");
        estoque.setCidade("Natal");
        estoque.setCpf(CPF_ESTOQUE);
        estoque.setPerfilAcesso(PerfilAcesso.ESTOQUE);
        estoque.setAtivo(true);
        estoque.setSenhaHash(passwordEncoder.encode("Estoque123"));
        estoque.setTrocaSenhaObrigatoria(false);
        funcionarioEstoqueId = funcionarioRepository.saveAndFlush(estoque).getId();

        Cookie sessaoAdmin = login(CPF, "Temporaria123");
        Cookie sessaoEstoque = login(CPF_ESTOQUE, "Estoque123");

        mockMvc.perform(get("/api/estoque/materiais").cookie(sessaoAdmin))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/estoque/materiais").cookie(sessaoEstoque))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout").cookie(sessaoEstoque).with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/auth/me").cookie(sessaoEstoque))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me").cookie(sessaoAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.funcionarioId").value(funcionarioId));
    }

    private Cookie login(String senha) throws Exception {
        return login(CPF, senha);
    }

    private Cookie login(String cpf, String senha) throws Exception {
        Cookie sessao = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cpf": "%s",
                                  "senha": "%s"
                                }
                                """.formatted(cpf, senha)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookie("SESSION");
        assertThat(sessao).isNotNull();
        return sessao;
    }
}
