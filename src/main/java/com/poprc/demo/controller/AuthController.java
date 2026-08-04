package com.poprc.demo.controller;

import com.poprc.demo.model.Funcionario;
import com.poprc.demo.repository.FuncionarioRepository;
import com.poprc.demo.security.SessaoAutenticacaoService;
import com.poprc.demo.security.UsuarioAutenticado;
import com.poprc.demo.service.AutenticacaoLocalService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final FuncionarioRepository funcionarioRepository;
    private final AutenticacaoLocalService autenticacaoLocalService;
    private final SessaoAutenticacaoService sessaoAutenticacaoService;

    @Value("${app.security.dev-login-enabled:false}")
    private boolean devLoginEnabled;

    @Value("${app.security.enabled:true}")
    private boolean securityEnabled;

    @Value("${app.security.zoho-enabled:false}")
    private boolean zohoEnabled;

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    @GetMapping("/config")
    public Map<String, Boolean> configuracao() {
        return Map.of(
                "securityEnabled", securityEnabled,
                "devLoginEnabled", devLoginEnabled,
                "zohoEnabled", securityEnabled && zohoEnabled,
                "bootstrapRequired", funcionarioRepository.count() == 0);
    }

    @PostMapping("/bootstrap")
    public ResponseEntity<UsuarioResponse> configurarPrimeiroAdministrador(
            @RequestBody BootstrapRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        Funcionario funcionario = autenticacaoLocalService.criarAdministradorInicial(
                request.nome(), request.cpf(), request.senha(), request.cidade());
        UsuarioAutenticado usuario = UsuarioAutenticado.de(funcionario, "CPF_SENHA");
        sessaoAutenticacaoService.autenticar(usuario, httpRequest, httpResponse);
        return ResponseEntity.status(201).body(UsuarioResponse.de(usuario, funcionario));
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> usuarioAtual(Authentication authentication) {
        UsuarioAutenticado usuario = extrairUsuario(authentication);
        return funcionarioRepository.findById(usuario.getFuncionarioId())
                .map(funcionario -> ResponseEntity.ok(UsuarioResponse.de(usuario, funcionario)))
                .orElseGet(() -> ResponseEntity.ok(UsuarioResponse.de(usuario, null)));
    }

    @PostMapping("/login")
    public ResponseEntity<UsuarioResponse> loginLocal(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        UsuarioAutenticado usuario = autenticacaoLocalService.autenticar(request.cpf(), request.senha());
        sessaoAutenticacaoService.autenticar(usuario, httpRequest, httpResponse);
        Funcionario funcionario = funcionarioRepository.findById(usuario.getFuncionarioId()).orElse(null);
        return ResponseEntity.ok(UsuarioResponse.de(usuario, funcionario));
    }

    @PostMapping("/reauth")
    public ResponseEntity<Void> reautenticar(
            @RequestBody SenhaRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        UsuarioAutenticado usuario = extrairUsuario(authentication);
        autenticacaoLocalService.confirmarSenha(usuario.getFuncionarioId(), request.senha());
        sessaoAutenticacaoService.marcarReautenticacao(httpRequest);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/alterar-senha")
    public ResponseEntity<UsuarioResponse> alterarSenha(
            @RequestBody AlterarSenhaRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        UsuarioAutenticado atual = extrairUsuario(authentication);
        Funcionario funcionario = autenticacaoLocalService.alterarSenha(
                atual.getFuncionarioId(), request.senhaAtual(), request.novaSenha(),
                atual.isTrocaSenhaObrigatoria());
        UsuarioAutenticado atualizado = UsuarioAutenticado.de(funcionario, atual.getMetodoAutenticacao());
        sessaoAutenticacaoService.autenticar(atualizado, httpRequest, httpResponse);
        return ResponseEntity.ok(UsuarioResponse.de(atualizado, funcionario));
    }

    private UsuarioAutenticado extrairUsuario(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UsuarioAutenticado usuario)) {
            throw new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException(
                    "Autenticação necessária.");
        }
        return usuario;
    }

    public record LoginRequest(String cpf, String senha) { }
    public record SenhaRequest(String senha) { }
    public record AlterarSenhaRequest(String senhaAtual, String novaSenha) { }
    public record BootstrapRequest(String nome, String cpf, String senha, String cidade) { }

    public record UsuarioResponse(
            Long funcionarioId,
            String nome,
            String email,
            String perfil,
            String metodoAutenticacao,
            boolean trocaSenhaObrigatoria,
            String cpfMascarado,
            String telefone,
            String funcao,
            String cidade,
            boolean senhaConfigurada) {
        static UsuarioResponse de(UsuarioAutenticado usuario, Funcionario funcionario) {
            return new UsuarioResponse(
                    usuario.getFuncionarioId(), usuario.getNome(), usuario.getEmail(), usuario.getPerfil(),
                    usuario.getMetodoAutenticacao(), usuario.isTrocaSenhaObrigatoria(),
                    funcionario == null ? null : funcionario.getCpfMascarado(),
                    funcionario == null ? null : funcionario.getTelefone(),
                    funcionario == null ? null : funcionario.getFuncao(),
                    funcionario == null ? null : funcionario.getCidade(),
                    funcionario != null && funcionario.isSenhaConfigurada());
        }
    }
}
