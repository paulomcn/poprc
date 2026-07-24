package com.poprc.demo.service;

import com.poprc.demo.model.Comarca;
import com.poprc.demo.model.OrdemServico;
import com.poprc.demo.repository.ComarcaRepository;
import com.poprc.demo.repository.OrdemServicoRepository;
import com.poprc.demo.repository.ProjetoRepository;
import com.poprc.demo.security.UsuarioAutenticado;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AcessoOperacionalService {

    private final ProjetoRepository projetoRepository;
    private final OrdemServicoRepository ordemServicoRepository;
    private final ComarcaRepository comarcaRepository;

    @Transactional(readOnly = true)
    public List<OrdemServico> filtrarOrdensPermitidas(
            List<OrdemServico> ordens,
            Authentication authentication) {
        UsuarioAutenticado usuario = tecnico(authentication);
        if (usuario == null) return ordens;
        Set<Long> projetosPermitidos = projetosPermitidos(usuario.getFuncionarioId());
        return ordens.stream()
                .filter(ordem -> ordem.getProjeto() != null
                        && projetosPermitidos.contains(ordem.getProjeto().getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Comarca> filtrarComarcasPermitidas(
            List<Comarca> comarcas,
            Authentication authentication) {
        UsuarioAutenticado usuario = tecnico(authentication);
        if (usuario == null) return comarcas;
        Set<Long> projetosPermitidos = projetosPermitidos(usuario.getFuncionarioId());
        return comarcas.stream()
                .filter(comarca -> comarca.getProjeto() != null
                        && projetosPermitidos.contains(comarca.getProjeto().getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public void garantirAcessoOrdem(Long ordemServicoId, Authentication authentication) {
        UsuarioAutenticado usuario = tecnico(authentication);
        if (usuario == null) return;
        OrdemServico ordem = ordemServicoRepository.findById(ordemServicoId)
                .orElseThrow(() -> new IllegalArgumentException("Ordem de serviço não encontrada."));
        if (ordem.getProjeto() == null
                || !projetosPermitidos(usuario.getFuncionarioId()).contains(ordem.getProjeto().getId())) {
            throw acessoNegado();
        }
    }

    @Transactional(readOnly = true)
    public void garantirAcessoComarca(Long comarcaId, Authentication authentication) {
        UsuarioAutenticado usuario = tecnico(authentication);
        if (usuario == null) return;
        Comarca comarca = comarcaRepository.findById(comarcaId)
                .orElseThrow(() -> new IllegalArgumentException("Obra não encontrada."));
        if (comarca.getProjeto() == null
                || !projetosPermitidos(usuario.getFuncionarioId()).contains(comarca.getProjeto().getId())) {
            throw acessoNegado();
        }
    }

    private Set<Long> projetosPermitidos(Long funcionarioId) {
        return projetoRepository.findByResponsavelId(funcionarioId).stream()
                .map(projeto -> projeto.getId())
                .collect(Collectors.toSet());
    }

    private UsuarioAutenticado tecnico(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof UsuarioAutenticado usuario)
                || !"TECNICO".equals(usuario.getPerfil())) {
            return null;
        }
        return usuario;
    }

    private AccessDeniedException acessoNegado() {
        return new AccessDeniedException("Esta OS não está atribuída à equipe do técnico autenticado.");
    }
}
