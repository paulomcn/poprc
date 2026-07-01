package com.poprc.demo.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.poprc.demo.model.ConfiguracaoNotificacao;
import com.poprc.demo.model.Contrato;
import com.poprc.demo.model.Material;
import com.poprc.demo.model.OrdemServico;
import com.poprc.demo.repository.ConfiguracaoNotificacaoRepository;
import com.poprc.demo.repository.ContratoRepository;
import com.poprc.demo.repository.MaterialRepository;
import com.poprc.demo.repository.OrdemServicoRepository;

import lombok.RequiredArgsConstructor;

@Service
@EnableScheduling
@RequiredArgsConstructor
public class AgendadorAlertasService {

    private final OrdemServicoRepository osRepository;
    private final MaterialRepository materialRepository;
    private final ContratoRepository contratoRepository;
    private final ConfiguracaoNotificacaoRepository configRepository; // INJETADO
    private final EmailService emailService;
    private final WhatsAppService whatsAppService;

    @Scheduled(cron = "0 0 8 * * ?")
    public void executarVarreduraDiariaDeAlertas() {
        System.out.println("  Iniciando motor de varredura inteligente do RC Operations Hub...");

        // ️ 1. Carrega as configurações reais do banco
        ConfiguracaoNotificacao config = configRepository.findById(1L)
                .orElseGet(() -> new ConfiguracaoNotificacao(1L, "diretoria@poprc.com", "5584999999999", true, true,
                        true));

        LocalDate hoje = LocalDate.now();
        StringBuilder relatorio = new StringBuilder();
        relatorio.append("=== RELATÓRIO DIÁRIO DE ALERTAS OPERACIONAIS ===\n\n");

        boolean temInconformidade = false;

        // Regra 1: Alerta OS Atrasada (Só roda se estiver ativa no banco)
        if (config.isAlertaOsAtrasada()) {
            List<OrdemServico> osAtrasadas = osRepository.findAll().stream()
                    .filter(os -> os.getDataExecucao() != null && os.getDataExecucao().isBefore(hoje))
                    .filter(os -> !"CONCLUIDA".equalsIgnoreCase(String.valueOf(os.getStatus()))
                            && !"FATURADA".equalsIgnoreCase(String.valueOf(os.getStatus())))
                    .collect(Collectors.toList());

            if (!osAtrasadas.isEmpty()) {
                temInconformidade = true;
                relatorio.append(" ️ ORDENS DE SERVIÇO ATRASADAS:\n");
                osAtrasadas.forEach(os -> relatorio.append(String.format("- OS #%d | Data Prevista: %s | Status: %s\n",
                        os.getId(), os.getDataExecucao(), os.getStatus())));
                relatorio.append("\n");
            }
        }

        // Regra 2: Alerta Estoque Crítico (Só roda se estiver ativa no banco)
        if (config.isAlertaEstoqueCritico()) {
            List<Material> estoqueCritico = materialRepository.findAll().stream()
                    .filter(m -> m.getQuantidadeDisponivel() != null && m.getQuantidadeDisponivel() <= 5)
                    .collect(Collectors.toList());

            if (!estoqueCritico.isEmpty()) {
                temInconformidade = true;
                relatorio.append("  ALERTA DE ESTOQUE CRÍTICO:\n");
                estoqueCritico.forEach(m -> relatorio.append(String.format("- %s (PN: %s) | Saldo Atual: %d unidades\n",
                        m.getNome(), m.getPartNumber(), m.getQuantidadeDisponivel())));
                relatorio.append("\n");
            }
        }

        // Regra 3: Alerta Vencimento Contratual (Só roda se estiver ativa no banco)
        if (config.isAlertaContratoVencendo()) {
            List<Contrato> contratosVencendo = contratoRepository.findAll().stream()
                    .filter(c -> c.getVigenciaFim() != null)
                    .filter(c -> {
                        long diasParaVencer = ChronoUnit.DAYS.between(hoje, c.getVigenciaFim());
                        return diasParaVencer >= 0 && diasParaVencer <= 30;
                    })
                    .collect(Collectors.toList());

            if (!contratosVencendo.isEmpty()) {
                temInconformidade = true;
                relatorio.append("  CONTRATOS PRÓXIMOS AO VENCIMENTO (FIM DE VIGÊNCIA):\n");
                contratosVencendo
                        .forEach(c -> relatorio.append(String.format("- Contrato: %s | Cliente: %s | Vence em: %s\n",
                                c.getContrato(), c.getCliente(), c.getVigenciaFim())));
                relatorio.append("\n");
            }
        }

        // Disparo Dinâmico com dados do Postgres
        if (temInconformidade) {
            // Dispara e-mail para o e-mail cadastrado em tela pelo admin
            emailService.enviarEmailAlerta(config.getEmailGestor(), "[RC Operations] Alertas Ativos - " + hoje,
                    relatorio.toString());

            // Dispara WhatsApp para o número cadastrado em tela pelo admin
            whatsAppService.enviarMensagemAlerta(config.getWhatsappGestor(),
                    "  [RC Operations] Alertas pendentes identificados no banco! Cheque o e-mail: "
                            + config.getEmailGestor());
        } else {
            System.out.println("  Everything OK! Nenhuma inconformidade ou varreduras desativadas.");
        }
    }
}