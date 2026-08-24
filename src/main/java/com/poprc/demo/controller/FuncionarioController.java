package com.poprc.demo.controller;

import com.poprc.demo.model.Funcionario;
import com.poprc.demo.model.PerfilAcesso;
import com.poprc.demo.repository.FuncionarioRepository;
import com.poprc.demo.security.CpfUtils;
import com.poprc.demo.service.AuditoriaAcessoService;
import com.poprc.demo.service.AutenticacaoLocalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/funcionarios")
@RequiredArgsConstructor
public class FuncionarioController {

    private final FuncionarioRepository funcionarioRepository;
    private final AutenticacaoLocalService autenticacaoLocalService;
    private final AuditoriaAcessoService auditoriaAcessoService;

    /**
     * POST: Inserir novo funcionário
     */
    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> inserirFuncionario(
            @RequestBody Funcionario funcionario,
            Authentication authentication) {
        try {
            Map<String, Object> response = new HashMap<>();

            if (funcionario.getNome() == null || funcionario.getNome().trim().isEmpty()) {
                response.put("erro", "Nome do funcionário é obrigatório");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            normalizarAcesso(funcionario);
            funcionario.setTelefone(normalizarTelefone(funcionario.getTelefone()));
            String cpfNormalizado = normalizarCpf(funcionario.getCpf());
            funcionario.setCpf(cpfNormalizado);
            String senhaTemporaria = funcionario.getSenha();
            if (senhaTemporaria != null && !senhaTemporaria.isBlank()) {
                if (cpfNormalizado == null) {
                    throw new IllegalArgumentException("Informe o CPF para configurar o acesso por senha.");
                }
                autenticacaoLocalService.validarNovaSenha(senhaTemporaria);
            }
            if (funcionario.getEmail() != null
                    && funcionarioRepository.findByEmailIgnoreCase(funcionario.getEmail()).isPresent()) {
                response.put("erro", "Já existe um funcionário com este e-mail");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }
            if (cpfNormalizado != null && funcionarioRepository.findByCpf(cpfNormalizado).isPresent()) {
                response.put("erro", "Já existe um funcionário com este CPF");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

            Funcionario funcionarioSalvo = funcionarioRepository.save(funcionario);
            if (senhaTemporaria != null && !senhaTemporaria.isBlank()) {
                funcionarioSalvo = autenticacaoLocalService.definirSenhaTemporaria(
                        funcionarioSalvo.getId(), senhaTemporaria);
            }
            auditoriaAcessoService.registrar(authentication, "FUNCIONARIO_CRIADO",
                    funcionarioSalvo.getId(),
                    "Perfil inicial: " + funcionarioSalvo.getPerfilAcesso()
                            + "; acesso ativo: " + Boolean.TRUE.equals(funcionarioSalvo.getAtivo()));
            response.put("funcionario", funcionarioSalvo);
            response.put("mensagem", "Funcionário inserido com sucesso");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("erro", e.getMessage()));
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            Map<String, Object> erro = new HashMap<>();
            erro.put("erro", "Erro ao inserir funcionário: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
        }
    }

    /**
     * GET /{id}: Buscar funcionário por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Funcionario> buscarPorId(@PathVariable Long id) {
        return funcionarioRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * ️ PUT /{id}: Atualizar funcionário existente
     */
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> atualizarFuncionario(@PathVariable Long id,
            @RequestBody Funcionario dadosAtualizados,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        String emailNormalizado = normalizarEmail(dadosAtualizados.getEmail());
        String cpfNormalizado = normalizarCpf(dadosAtualizados.getCpf());
        if (emailNormalizado != null && funcionarioRepository.findByEmailIgnoreCase(emailNormalizado)
                .filter(existente -> !existente.getId().equals(id)).isPresent()) {
            response.put("erro", "Já existe um funcionário com este e-mail");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        if (cpfNormalizado != null && funcionarioRepository.findByCpf(cpfNormalizado)
                .filter(existente -> !existente.getId().equals(id)).isPresent()) {
            response.put("erro", "Já existe um funcionário com este CPF");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        Funcionario funcionario = funcionarioRepository.findById(id).orElse(null);
        if (funcionario == null) return ResponseEntity.notFound().build();

        PerfilAcesso perfilAnterior = funcionario.getPerfilAcesso();
        boolean ativoAnterior = Boolean.TRUE.equals(funcionario.getAtivo());
        PerfilAcesso novoPerfil = dadosAtualizados.getPerfilAcesso() == null
                ? perfilAnterior
                : dadosAtualizados.getPerfilAcesso();
        boolean novoAtivo = dadosAtualizados.getAtivo() == null
                ? ativoAnterior
                : Boolean.TRUE.equals(dadosAtualizados.getAtivo());
        try {
            validarUltimoAdministrador(funcionario, novoPerfil, novoAtivo);
            if (dadosAtualizados.getSenha() != null && !dadosAtualizados.getSenha().isBlank()) {
                String cpfEfetivo = cpfNormalizado == null ? funcionario.getCpf() : cpfNormalizado;
                if (cpfEfetivo == null) {
                    throw new IllegalArgumentException("Informe o CPF para configurar o acesso por senha.");
                }
                autenticacaoLocalService.validarNovaSenha(dadosAtualizados.getSenha());
            }
            funcionario.setNome(dadosAtualizados.getNome());
            funcionario.setFuncao(dadosAtualizados.getFuncao());
            funcionario.setCidade(dadosAtualizados.getCidade());
            if (cpfNormalizado != null) funcionario.setCpf(cpfNormalizado);
            funcionario.setTelefone(normalizarTelefone(dadosAtualizados.getTelefone()));
            funcionario.setEmail(emailNormalizado);
            funcionario.setPerfilAcesso(novoPerfil);
            funcionario.setAtivo(novoAtivo);

            if (dadosAtualizados.getCertificacoes() != null) {
                funcionario.setCertificacoes(dadosAtualizados.getCertificacoes());
            }
            if (dadosAtualizados.getDocumentPaths() != null) {
                funcionario.setDocumentPaths(dadosAtualizados.getDocumentPaths());
            }

            Funcionario salvo = funcionarioRepository.save(funcionario);
            if (!perfilAnterior.equals(novoPerfil)) {
                auditoriaAcessoService.registrar(authentication, "PERFIL_ALTERADO", id,
                        "Perfil: " + perfilAnterior + " -> " + novoPerfil);
            }
            if (ativoAnterior != novoAtivo) {
                auditoriaAcessoService.registrar(authentication, "STATUS_ALTERADO", id,
                        "Acesso: " + (ativoAnterior ? "ativo" : "inativo")
                                + " -> " + (novoAtivo ? "ativo" : "inativo"));
            }
            if (dadosAtualizados.getSenha() != null && !dadosAtualizados.getSenha().isBlank()) {
                salvo = autenticacaoLocalService.definirSenhaTemporaria(id, dadosAtualizados.getSenha());
                auditoriaAcessoService.registrar(authentication, "SENHA_TEMPORARIA_REDEFINIDA", id,
                        "Senha temporária redefinida; troca obrigatória no próximo login.");
            }
            response.put("funcionario", salvo);
            response.put("mensagem", "Funcionário atualizado com sucesso!");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("erro", exception.getMessage()));
        }
    }

    /**
     * Listar todos os funcionários
     */
    @GetMapping
    public ResponseEntity<List<Funcionario>> listarTodos() {
        return ResponseEntity.ok(funcionarioRepository.findAll());
    }

    @GetMapping("/nome")
    public ResponseEntity<List<Funcionario>> buscarPorNome(@RequestParam String nome) {
        return ResponseEntity.ok(funcionarioRepository.findByNome(nome));
    }

    @GetMapping("/funcao")
    public ResponseEntity<List<Funcionario>> buscarPorFuncao(@RequestParam String funcao) {
        return ResponseEntity.ok(funcionarioRepository.findByFuncao(funcao));
    }

    @GetMapping("/cidade")
    public ResponseEntity<List<Funcionario>> buscarPorCidade(@RequestParam String cidade) {
        return ResponseEntity.ok(funcionarioRepository.findByCidade(cidade));
    }

    @GetMapping("/perfis-acesso")
    public ResponseEntity<List<PerfilAcesso>> listarPerfisAcesso() {
        return ResponseEntity.ok(List.of(PerfilAcesso.values()));
    }

    @GetMapping("/auditoria-acessos")
    public ResponseEntity<List<com.poprc.demo.model.LogOperacaoSensivel>> listarAuditoriaAcessos() {
        return ResponseEntity.ok(auditoriaAcessoService.listarAlteracoesRecentes());
    }

    @PostMapping("/{id}/redefinir-senha")
    @Transactional
    public ResponseEntity<Map<String, Object>> redefinirSenha(
            @PathVariable Long id,
            @RequestBody RedefinirSenhaRequest request,
            Authentication authentication) {
        Funcionario funcionario = autenticacaoLocalService.definirSenhaTemporaria(id, request.senhaTemporaria());
        auditoriaAcessoService.registrar(authentication, "SENHA_TEMPORARIA_REDEFINIDA", id,
                "Senha temporária redefinida; troca obrigatória no próximo login.");
        return ResponseEntity.ok(Map.of(
                "mensagem", "Senha temporária definida. A troca será exigida no próximo login.",
                "funcionario", funcionario));
    }

    private void normalizarAcesso(Funcionario funcionario) {
        funcionario.setEmail(normalizarEmail(funcionario.getEmail()));
        if (funcionario.getPerfilAcesso() == null) {
            funcionario.setPerfilAcesso(PerfilAcesso.TECNICO);
        }
        if (funcionario.getAtivo() == null) {
            funcionario.setAtivo(true);
        }
    }

    private String normalizarEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase();
    }

    private String normalizarCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) return null;
        if (!CpfUtils.valido(cpf)) throw new IllegalArgumentException("CPF inválido.");
        return CpfUtils.normalizar(cpf);
    }

    private String normalizarTelefone(String telefone) {
        return telefone == null || telefone.isBlank() ? null : telefone.trim();
    }

    private void validarUltimoAdministrador(
            Funcionario funcionario, PerfilAcesso novoPerfil, boolean novoAtivo) {
        boolean removeAdministradorAtivo = PerfilAcesso.ADMIN.equals(funcionario.getPerfilAcesso())
                && Boolean.TRUE.equals(funcionario.getAtivo())
                && (!PerfilAcesso.ADMIN.equals(novoPerfil) || !novoAtivo);
        if (removeAdministradorAtivo
                && funcionarioRepository.countByPerfilAcessoAndAtivoTrue(PerfilAcesso.ADMIN) <= 1) {
            throw new IllegalArgumentException(
                    "O último administrador ativo não pode ser desativado nem ter o perfil alterado.");
        }
    }

    public record RedefinirSenhaRequest(String senhaTemporaria) { }
}
