package com.poprc.demo.service;

import com.poprc.demo.model.Funcionario;
import com.poprc.demo.model.NotificacaoOperacional;
import com.poprc.demo.model.OrdemServico;
import com.poprc.demo.repository.NotificacaoOperacionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificacaoOperacionalService {

    private final NotificacaoOperacionalRepository repository;

    @Transactional
    public boolean registrarSeAusente(
            String chave,
            String tipo,
            String severidade,
            String titulo,
            String mensagem,
            OrdemServico ordemServico,
            Funcionario destinatario) {
        if (repository.existsByChave(chave)) {
            return false;
        }

        NotificacaoOperacional notificacao = new NotificacaoOperacional();
        notificacao.setChave(chave);
        notificacao.setTipo(tipo);
        notificacao.setSeveridade(severidade);
        notificacao.setTitulo(titulo);
        notificacao.setMensagem(mensagem);
        notificacao.setCriadaEm(LocalDateTime.now());
        notificacao.setOrdemServico(ordemServico);
        notificacao.setDestinatario(destinatario);
        repository.save(notificacao);
        return true;
    }

    @Transactional(readOnly = true)
    public List<NotificacaoOperacional> listar(Long funcionarioId) {
        return funcionarioId == null
                ? repository.findAllByOrderByCriadaEmDesc()
                : repository.listarVisiveisPara(funcionarioId);
    }

    @Transactional
    public NotificacaoOperacional marcarComoLida(Long id) {
        NotificacaoOperacional notificacao = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notificação não encontrada."));
        if (notificacao.getLidaEm() == null) {
            notificacao.setLidaEm(LocalDateTime.now());
        }
        return repository.save(notificacao);
    }
}
