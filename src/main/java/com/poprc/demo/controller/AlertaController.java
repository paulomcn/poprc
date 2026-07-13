package com.poprc.demo.controller;

import com.poprc.demo.model.ConfiguracaoNotificacao;
import com.poprc.demo.model.NotificacaoOperacional;
import com.poprc.demo.repository.ConfiguracaoNotificacaoRepository;
import com.poprc.demo.service.AgendadorAlertasService;
import com.poprc.demo.service.NotificacaoOperacionalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/alertas")
@RequiredArgsConstructor
public class AlertaController {

    private final AgendadorAlertasService agendadorAlertasService;
    private final ConfiguracaoNotificacaoRepository configRepository;
    private final NotificacaoOperacionalService notificacaoService;

    /**
     * GET: Busca as configurações do banco. Se não existir, cria o padrão.
     */
    @GetMapping("/configuracoes")
    public ResponseEntity<ConfiguracaoNotificacao> obterConfiguracoes() {
        ConfiguracaoNotificacao config = configRepository.findById(1L)
                .orElseGet(() -> configRepository.save(new ConfiguracaoNotificacao(
                        1L, "diretoria@poprc.com", "5584999999999", true, true, true)));
        return ResponseEntity.ok(config);
    }

    /**
     * POST: Salva ou atualiza os parâmetros globais no Postgres
     */
    @PostMapping("/configuracoes")
    public ResponseEntity<ConfiguracaoNotificacao> salvarConfiguracoes(
            @RequestBody ConfiguracaoNotificacao novaConfig) {
        novaConfig.setId(1L); // Força a gravar sempre na linha 1
        return ResponseEntity.ok(configRepository.save(novaConfig));
    }

    /**
     * Botão de Pânico: Dispara varredura manual
     */
    @PostMapping("/disparar-todos")
    public ResponseEntity<AgendadorAlertasService.VarreduraResultado> forcarDisparoTodosAlertas() {
        return ResponseEntity.ok(agendadorAlertasService.executarVarreduraDiariaDeAlertas());
    }

    @GetMapping("/notificacoes")
    public ResponseEntity<List<NotificacaoResponse>> listarNotificacoes(
            @RequestParam(required = false) Long funcionarioId) {
        return ResponseEntity.ok(notificacaoService.listar(funcionarioId).stream()
                .map(NotificacaoResponse::from)
                .toList());
    }

    @PatchMapping("/notificacoes/{id}/lida")
    public ResponseEntity<NotificacaoResponse> marcarComoLida(@PathVariable Long id) {
        return ResponseEntity.ok(NotificacaoResponse.from(notificacaoService.marcarComoLida(id)));
    }

    public record NotificacaoResponse(
            Long id,
            String tipo,
            String severidade,
            String titulo,
            String mensagem,
            LocalDateTime criadaEm,
            LocalDateTime lidaEm,
            Long ordemServicoId,
            String numeroOs,
            Long destinatarioId,
            String destinatarioNome) {
        static NotificacaoResponse from(NotificacaoOperacional notificacao) {
            return new NotificacaoResponse(
                    notificacao.getId(),
                    notificacao.getTipo(),
                    notificacao.getSeveridade(),
                    notificacao.getTitulo(),
                    notificacao.getMensagem(),
                    notificacao.getCriadaEm(),
                    notificacao.getLidaEm(),
                    notificacao.getOrdemServico() == null ? null : notificacao.getOrdemServico().getId(),
                    notificacao.getOrdemServico() == null ? null : notificacao.getOrdemServico().getNumeroOs(),
                    notificacao.getDestinatario() == null ? null : notificacao.getDestinatario().getId(),
                    notificacao.getDestinatario() == null ? null : notificacao.getDestinatario().getNome());
        }
    }
}
