package com.poprc.demo.service;

import com.poprc.demo.model.Funcionario;
import com.poprc.demo.repository.FuncionarioRepository;
import com.poprc.demo.security.CpfUtils;
import com.poprc.demo.security.UsuarioAutenticado;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AutenticacaoLocalService {
    private static final int MAX_TENTATIVAS = 5;
    private static final int MINUTOS_BLOQUEIO = 15;

    private final FuncionarioRepository funcionarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UsuarioAutenticado autenticar(String cpfInformado, String senha) {
        String cpf = CpfUtils.normalizar(cpfInformado);
        Funcionario funcionario = funcionarioRepository.findByCpf(cpf)
                .orElseThrow(this::credenciaisInvalidas);
        validarAtivoEBloqueio(funcionario);

        if (funcionario.getSenhaHash() == null || senha == null
                || !passwordEncoder.matches(senha, funcionario.getSenhaHash())) {
            registrarFalha(funcionario);
            throw credenciaisInvalidas();
        }

        funcionario.setTentativasLogin(0);
        funcionario.setBloqueadoAte(null);
        funcionarioRepository.save(funcionario);
        return UsuarioAutenticado.de(funcionario, "CPF_SENHA");
    }

    @Transactional(readOnly = true)
    public void confirmarSenha(Long funcionarioId, String senha) {
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(this::credenciaisInvalidas);
        if (funcionario.getSenhaHash() == null || senha == null
                || !passwordEncoder.matches(senha, funcionario.getSenhaHash())) {
            throw credenciaisInvalidas();
        }
    }

    @Transactional
    public Funcionario alterarSenha(
            Long funcionarioId,
            String senhaAtual,
            String novaSenha,
            boolean primeiraTroca) {
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(this::credenciaisInvalidas);
        if (!primeiraTroca && funcionario.getSenhaHash() != null
                && (senhaAtual == null || !passwordEncoder.matches(senhaAtual, funcionario.getSenhaHash()))) {
            throw new IllegalArgumentException("A senha atual não confere.");
        }
        validarNovaSenha(novaSenha);
        funcionario.setSenhaHash(passwordEncoder.encode(novaSenha));
        funcionario.setTrocaSenhaObrigatoria(false);
        funcionario.setTentativasLogin(0);
        funcionario.setBloqueadoAte(null);
        return funcionarioRepository.save(funcionario);
    }

    @Transactional
    public Funcionario definirSenhaTemporaria(Long funcionarioId, String novaSenha) {
        validarNovaSenha(novaSenha);
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado."));
        funcionario.setSenhaHash(passwordEncoder.encode(novaSenha));
        funcionario.setTrocaSenhaObrigatoria(true);
        funcionario.setTentativasLogin(0);
        funcionario.setBloqueadoAte(null);
        return funcionarioRepository.save(funcionario);
    }

    public void validarNovaSenha(String senha) {
        if (senha == null || senha.length() < 8
                || senha.chars().noneMatch(Character::isLetter)
                || senha.chars().noneMatch(Character::isDigit)) {
            throw new IllegalArgumentException("A senha deve ter ao menos 8 caracteres, com letras e números.");
        }
    }

    private void validarAtivoEBloqueio(Funcionario funcionario) {
        if (!Boolean.TRUE.equals(funcionario.getAtivo())) throw credenciaisInvalidas();
        if (funcionario.getBloqueadoAte() != null && funcionario.getBloqueadoAte().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("Acesso temporariamente bloqueado. Tente novamente mais tarde.");
        }
    }

    private void registrarFalha(Funcionario funcionario) {
        int tentativas = (funcionario.getTentativasLogin() == null ? 0 : funcionario.getTentativasLogin()) + 1;
        funcionario.setTentativasLogin(tentativas);
        if (tentativas >= MAX_TENTATIVAS) {
            funcionario.setBloqueadoAte(LocalDateTime.now().plusMinutes(MINUTOS_BLOQUEIO));
            funcionario.setTentativasLogin(0);
        }
        funcionarioRepository.save(funcionario);
    }

    private IllegalArgumentException credenciaisInvalidas() {
        return new IllegalArgumentException("CPF ou senha inválidos.");
    }
}
