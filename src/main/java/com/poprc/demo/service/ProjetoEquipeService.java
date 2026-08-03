package com.poprc.demo.service;

import com.poprc.demo.dto.EquipeProjetoRequest;
import com.poprc.demo.model.Funcionario;
import com.poprc.demo.model.Projeto;
import com.poprc.demo.model.ProjetoMembro;
import com.poprc.demo.repository.FuncionarioRepository;
import com.poprc.demo.repository.ProjetoMembroRepository;
import com.poprc.demo.repository.ProjetoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProjetoEquipeService {

    private static final Set<String> PAPEIS_VALIDOS = Set.of("LIDER_EQUIPE", "TECNICO");

    private final ProjetoRepository projetoRepository;
    private final ProjetoMembroRepository membroRepository;
    private final FuncionarioRepository funcionarioRepository;

    @Transactional(readOnly = true)
    public List<ProjetoMembro> listar(Long projetoId) {
        if (!projetoRepository.existsById(projetoId)) {
            throw new IllegalArgumentException("Projeto não encontrado.");
        }
        return membroRepository.findByProjetoIdOrderByResponsavelPrincipalDescIdAsc(projetoId);
    }

    @Transactional
    public List<ProjetoMembro> substituirEquipe(Long projetoId, EquipeProjetoRequest request) {
        Projeto projeto = projetoRepository.findById(projetoId)
                .orElseThrow(() -> new IllegalArgumentException("Projeto não encontrado."));
        if (Boolean.TRUE.equals(projeto.getArquivado())) {
            throw new IllegalStateException("Não é possível alterar a equipe de um projeto arquivado.");
        }
        List<EquipeProjetoRequest.MembroRequest> membros = request != null ? request.getMembros() : null;
        if (membros == null || membros.isEmpty()) {
            throw new IllegalArgumentException("A equipe precisa ter ao menos um funcionário.");
        }

        Set<Long> funcionariosUnicos = new HashSet<>();
        long principais = membros.stream().filter(item -> Boolean.TRUE.equals(item.getResponsavelPrincipal())).count();
        if (principais != 1) {
            throw new IllegalArgumentException("Defina exatamente um responsável principal para o projeto.");
        }

        List<ProjetoMembro> novosMembros = membros.stream().map(item -> {
            if (item.getFuncionarioId() == null || !funcionariosUnicos.add(item.getFuncionarioId())) {
                throw new IllegalArgumentException("Cada funcionário pode aparecer apenas uma vez na equipe.");
            }
            String papel = item.getPapel() == null ? "" : item.getPapel().trim().toUpperCase();
            if (!PAPEIS_VALIDOS.contains(papel)) {
                throw new IllegalArgumentException("Função de equipe inválida: " + item.getPapel());
            }
            Funcionario funcionario = funcionarioRepository.findById(item.getFuncionarioId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Funcionário não encontrado: " + item.getFuncionarioId()));
            ProjetoMembro membro = new ProjetoMembro();
            membro.setProjeto(projeto);
            membro.setFuncionario(funcionario);
            membro.setPapel(papel);
            membro.setResponsavelPrincipal(Boolean.TRUE.equals(item.getResponsavelPrincipal()));
            return membro;
        }).toList();

        membroRepository.deleteByProjetoId(projetoId);
        membroRepository.flush();
        ProjetoMembro principal = novosMembros.stream()
                .filter(item -> Boolean.TRUE.equals(item.getResponsavelPrincipal()))
                .findFirst()
                .orElseThrow();
        projeto.setResponsavel(principal.getFuncionario());
        projetoRepository.save(projeto);
        return membroRepository.saveAll(novosMembros);
    }
}
