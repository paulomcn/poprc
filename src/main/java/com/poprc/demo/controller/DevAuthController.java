package com.poprc.demo.controller;

import com.poprc.demo.model.Funcionario;
import com.poprc.demo.repository.FuncionarioRepository;
import com.poprc.demo.security.UsuarioAutenticado;
import com.poprc.demo.security.SessaoAutenticacaoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.security.dev-login-enabled", havingValue = "true")
public class DevAuthController {
    private final FuncionarioRepository funcionarioRepository;
    private final SessaoAutenticacaoService sessaoAutenticacaoService;

    @GetMapping("/dev-usuarios")
    public List<UsuarioDevResponse> listarUsuarios() {
        List<UsuarioDevResponse> usuarios = funcionarioRepository.findByAtivoTrueOrderByNomeAsc().stream()
                .map(UsuarioDevResponse::de)
                .toList();
        return usuarios.isEmpty()
                ? List.of(new UsuarioDevResponse(0L, "Administrador de Desenvolvimento", "Bootstrap local", "ADMIN"))
                : usuarios;
    }

    @PostMapping("/dev-login")
    public ResponseEntity<AuthController.UsuarioResponse> login(
            @RequestBody LoginDevRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        if (Long.valueOf(0L).equals(request.funcionarioId()) && funcionarioRepository.count() == 0) {
            UsuarioAutenticado bootstrap = new UsuarioAutenticado(
                    0L, "Administrador de Desenvolvimento", null, "ADMIN", "DEV", false);
            return autenticar(bootstrap, httpRequest, httpResponse);
        }
        Funcionario funcionario = funcionarioRepository.findById(request.funcionarioId())
                .filter(item -> Boolean.TRUE.equals(item.getAtivo()))
                .orElseThrow(() -> new IllegalArgumentException("Funcionário ativo não encontrado."));
        return autenticar(UsuarioAutenticado.de(funcionario, "DEV"), httpRequest, httpResponse);
    }

    private ResponseEntity<AuthController.UsuarioResponse> autenticar(
            UsuarioAutenticado usuario,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        sessaoAutenticacaoService.autenticar(usuario, httpRequest, httpResponse);
        Funcionario funcionario = usuario.getFuncionarioId() == 0L
                ? null
                : funcionarioRepository.findById(usuario.getFuncionarioId()).orElse(null);
        return ResponseEntity.ok(AuthController.UsuarioResponse.de(usuario, funcionario));
    }

    public record LoginDevRequest(Long funcionarioId) { }

    public record UsuarioDevResponse(Long id, String nome, String funcao, String perfil) {
        static UsuarioDevResponse de(Funcionario funcionario) {
            return new UsuarioDevResponse(funcionario.getId(), funcionario.getNome(), funcionario.getFuncao(),
                    funcionario.getPerfilAcesso().name());
        }
    }
}
