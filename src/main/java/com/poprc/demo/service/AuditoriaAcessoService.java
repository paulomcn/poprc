package com.poprc.demo.service;

import com.poprc.demo.model.LogOperacaoSensivel;
import com.poprc.demo.repository.LogOperacaoSensivelRepository;
import com.poprc.demo.security.UsuarioAutenticado;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditoriaAcessoService {
    private static final String PREFIXO_EVENTO = "ACESSO_";

    private final LogOperacaoSensivelRepository logRepository;

    public void registrar(Authentication authentication, String tipoEvento,
            Long alvoFuncionarioId, String detalhes) {
        LogOperacaoSensivel log = new LogOperacaoSensivel();
        if (authentication != null && authentication.getPrincipal() instanceof UsuarioAutenticado usuario) {
            log.setFuncionarioId(usuario.getFuncionarioId());
            log.setUsuario(usuario.getEmail() == null ? usuario.getNome() : usuario.getEmail());
            log.setPerfil(usuario.getPerfil());
        } else {
            log.setUsuario(authentication == null ? "Sistema" : authentication.getName());
            log.setPerfil("SISTEMA");
        }
        log.setMetodoHttp("AUDIT");
        log.setCaminho("/api/funcionarios/" + alvoFuncionarioId);
        log.setStatusHttp(200);
        log.setTipoEvento(PREFIXO_EVENTO + tipoEvento);
        log.setAlvoFuncionarioId(alvoFuncionarioId);
        log.setDetalhes(detalhes);
        log.setRegistradoEm(LocalDateTime.now());
        logRepository.save(log);
    }

    public List<LogOperacaoSensivel> listarAlteracoesRecentes() {
        return logRepository.findTop100ByTipoEventoStartingWithOrderByRegistradoEmDesc(PREFIXO_EVENTO);
    }
}
