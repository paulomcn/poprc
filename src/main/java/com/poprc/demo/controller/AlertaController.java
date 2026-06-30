package com.poprc.demo.controller;

import com.poprc.demo.model.ConfiguracaoNotificacao;
import com.poprc.demo.repository.ConfiguracaoNotificacaoRepository;
import com.poprc.demo.service.AgendadorAlertasService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/alertas")
@RequiredArgsConstructor
public class AlertaController {

    private final AgendadorAlertasService agendadorAlertasService;
    private final ConfiguracaoNotificacaoRepository configRepository;

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
    public ResponseEntity<Map<String, String>> forcarDisparoTodosAlertas() {
        Map<String, String> resposta = new HashMap<>();
        try {
            agendadorAlertasService.executarVarreduraDiariaDeAlertas();
            resposta.put("status", "sucesso");
            resposta.put("message", "Varredura do banco executada com sucesso!");
            return ResponseEntity.ok(resposta);
        } catch (Exception e) {
            resposta.put("status", "erro");
            resposta.put("message", "Falha ao forçar disparo: " + e.getMessage());
            return ResponseEntity.internalServerError().body(resposta);
        }
    }
}