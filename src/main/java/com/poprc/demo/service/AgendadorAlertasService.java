package com.poprc.demo.service;

import com.poprc.demo.model.ConfiguracaoNotificacao;
import com.poprc.demo.model.Contrato;
import com.poprc.demo.model.Funcionario;
import com.poprc.demo.model.Material;
import com.poprc.demo.model.OrdemServico;
import com.poprc.demo.model.SaldoMaterialLocal;
import com.poprc.demo.model.StatusOS;
import com.poprc.demo.model.TipoControleEstoque;
import com.poprc.demo.repository.ConfiguracaoNotificacaoRepository;
import com.poprc.demo.repository.ContratoRepository;
import com.poprc.demo.repository.OrdemServicoRepository;
import com.poprc.demo.repository.SaldoMaterialLocalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

@Service
@EnableScheduling
@RequiredArgsConstructor
public class AgendadorAlertasService {

    private final OrdemServicoRepository osRepository;
    private final SaldoMaterialLocalRepository saldoMaterialLocalRepository;
    private final ContratoRepository contratoRepository;
    private final ConfiguracaoNotificacaoRepository configRepository;
    private final NotificacaoOperacionalService notificacaoService;
    private final EmailService emailService;

    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public VarreduraResultado executarVarreduraDiariaDeAlertas() {
        ConfiguracaoNotificacao config = configRepository.findById(1L)
                .orElseGet(() -> configuracaoPadrao());
        LocalDateTime agora = LocalDateTime.now();
        LocalDate hoje = agora.toLocalDate();
        int antecedenciaOsHoras = inteiroPositivo(config.getAntecedenciaOsHoras(), 24);
        int antecedenciaContratoDias = inteiroPositivo(config.getAntecedenciaContratoDias(), 30);
        int encontrados = 0;
        int novos = 0;
        int resolvidos = 0;
        int emailsEnviados = 0;
        int emailsFalhos = 0;

        if (config.isAlertaOsAtrasada()) {
            Set<String> osAtrasadasAtivas = new HashSet<>();
            Set<String> osProximasDoPrazoAtivas = new HashSet<>();
            for (OrdemServico os : osRepository.findAll()) {
                if (os.getDeadline() == null || finalizada(os.getStatus())) {
                    continue;
                }

                Funcionario responsavel = os.getProjeto() == null ? null : os.getProjeto().getResponsavel();
                if (os.getDeadline().isBefore(agora)) {
                    String chave = "OS_ATRASADA:" + os.getId() + ":" + os.getDeadline();
                    osAtrasadasAtivas.add(chave);
                    encontrados++;
                    boolean criada = notificacaoService.registrarSeAusente(
                            chave,
                            "OS_ATRASADA",
                            "CRITICA",
                            "OS " + os.getNumeroOs() + " atrasada",
                            "O prazo venceu em " + os.getDeadline() + ". Regularize a execução ou atualize o cronograma.",
                            os,
                            responsavel);
                    if (criada) novos++;
                    EmailService.StatusEnvio statusEmail = enviarEmailSeNovo(
                            criada, config, "OS " + os.getNumeroOs() + " atrasada",
                            "O prazo venceu em " + os.getDeadline()
                                    + ". Regularize a execução ou atualize o cronograma.");
                    if (statusEmail == EmailService.StatusEnvio.ENVIADO) emailsEnviados++;
                    if (statusEmail == EmailService.StatusEnvio.FALHA) emailsFalhos++;
                } else if (!os.getDeadline().isAfter(agora.plusHours(antecedenciaOsHoras))) {
                    String chave = "OS_PRAZO_24H:" + os.getId() + ":" + os.getDeadline();
                    osProximasDoPrazoAtivas.add(chave);
                    encontrados++;
                    boolean criada = notificacaoService.registrarSeAusente(
                            chave,
                            "OS_PRAZO_24H",
                            "ALERTA",
                            "OS " + os.getNumeroOs() + " próxima do prazo",
                            "Restam menos de " + antecedenciaOsHoras + " horas para o deadline de "
                                    + os.getDeadline() + ".",
                            os,
                            responsavel);
                    if (criada) novos++;
                    EmailService.StatusEnvio statusEmail = enviarEmailSeNovo(
                            criada, config, "OS " + os.getNumeroOs() + " próxima do prazo",
                            "Restam menos de " + antecedenciaOsHoras + " horas para o deadline de "
                                    + os.getDeadline() + ".");
                    if (statusEmail == EmailService.StatusEnvio.ENVIADO) emailsEnviados++;
                    if (statusEmail == EmailService.StatusEnvio.FALHA) emailsFalhos++;
                }
            }
            resolvidos += notificacaoService.resolverAusentesDoTipo(
                    "OS_ATRASADA", osAtrasadasAtivas, "A OS foi regularizada, finalizada ou teve o prazo atualizado.");
            resolvidos += notificacaoService.resolverAusentesDoTipo(
                    "OS_PRAZO_24H", osProximasDoPrazoAtivas,
                    "A OS saiu da janela de aviso, foi finalizada ou passou para a condição de atraso.");
        }

        if (config.isAlertaEstoqueCritico()) {
            Set<String> estoquesCriticosAtivos = new HashSet<>();
            for (SaldoMaterialLocal saldoLocal : saldoMaterialLocalRepository
                    .findAllByOrderByMaterialNomeAscLocalEstoqueNomeAsc()) {
                Material material = saldoLocal.getMaterial();
                if (material == null || saldoLocal.getLocalEstoque() == null
                        || Boolean.FALSE.equals(saldoLocal.getLocalEstoque().getAtivo())) {
                    continue;
                }

                BigDecimal minimo = saldoLocal.getEstoqueMinimo() != null
                        ? saldoLocal.getEstoqueMinimo()
                        : valor(material.getEstoqueMinimo());
                if (minimo.signum() <= 0) {
                    continue;
                }

                BigDecimal saldoLivre = saldoLivre(saldoLocal, material);
                if (saldoLivre.compareTo(minimo) > 0) {
                    continue;
                }

                String local = saldoLocal.getLocalEstoque().getNome();
                String unidade = controlaMetragem(material)
                        ? "m"
                        : material.getUnidadeMedida() == null ? "UNIDADE" : material.getUnidadeMedida().name();
                String chave = "ESTOQUE_CRITICO:" + material.getId() + ":" + saldoLocal.getLocalEstoque().getId();
                estoquesCriticosAtivos.add(chave);
                encontrados++;
                boolean criada = notificacaoService.registrarSeAusente(
                        chave,
                        "ESTOQUE_CRITICO",
                        "ALERTA",
                        "Estoque crítico: " + material.getNome() + " em " + local,
                        "Depósito: " + local + ". Saldo livre: " + formatar(saldoLivre) + " " + unidade
                                + ". Mínimo configurado: " + formatar(minimo) + " " + unidade + ".",
                        null,
                        null);
                if (criada) novos++;
                EmailService.StatusEnvio statusEmail = enviarEmailSeNovo(
                        criada, config, "Estoque crítico: " + material.getNome() + " em " + local,
                        "Depósito: " + local + ". Saldo livre: " + formatar(saldoLivre) + " " + unidade
                                + ". Mínimo configurado: " + formatar(minimo) + " " + unidade + ".");
                if (statusEmail == EmailService.StatusEnvio.ENVIADO) emailsEnviados++;
                if (statusEmail == EmailService.StatusEnvio.FALHA) emailsFalhos++;
            }
            resolvidos += notificacaoService.resolverAusentesDoTipo(
                    "ESTOQUE_CRITICO", estoquesCriticosAtivos,
                    "O saldo livre voltou a ficar acima do estoque mínimo configurado.");
        }

        if (config.isAlertaContratoVencendo()) {
            Set<String> contratosVencendoAtivos = new HashSet<>();
            for (Contrato contrato : contratoRepository.findAll()) {
                if (contrato.getVigenciaFim() == null) {
                    continue;
                }
                long diasParaVencer = ChronoUnit.DAYS.between(hoje, contrato.getVigenciaFim());
                if (diasParaVencer < 0 || diasParaVencer > antecedenciaContratoDias) {
                    continue;
                }
                String chave = "CONTRATO_VENCENDO:" + contrato.getId() + ":" + contrato.getVigenciaFim();
                contratosVencendoAtivos.add(chave);
                encontrados++;
                boolean criada = notificacaoService.registrarSeAusente(
                        chave,
                        "CONTRATO_VENCENDO",
                        "INFORMATIVA",
                        "Contrato próximo do vencimento",
                        "Contrato " + contrato.getContrato() + " de " + contrato.getCliente()
                                + " vence em " + contrato.getVigenciaFim() + ".",
                        null,
                        null);
                if (criada) novos++;
                EmailService.StatusEnvio statusEmail = enviarEmailSeNovo(
                        criada, config, "Contrato próximo do vencimento",
                        "Contrato " + contrato.getContrato() + " de " + contrato.getCliente()
                                + " vence em " + contrato.getVigenciaFim() + ".");
                if (statusEmail == EmailService.StatusEnvio.ENVIADO) emailsEnviados++;
                if (statusEmail == EmailService.StatusEnvio.FALHA) emailsFalhos++;
            }
            resolvidos += notificacaoService.resolverAusentesDoTipo(
                    "CONTRATO_VENCENDO", contratosVencendoAtivos,
                    "O contrato saiu da janela de vencimento ou teve sua vigência atualizada.");
        }

        return new VarreduraResultado(
                encontrados, novos, encontrados - novos, resolvidos, agora, "INTERNO_E_EMAIL",
                emailsEnviados, emailsFalhos, emailService.isHabilitado());
    }

    private EmailService.StatusEnvio enviarEmailSeNovo(
            boolean notificacaoCriada,
            ConfiguracaoNotificacao config,
            String assunto,
            String mensagem) {
        if (!notificacaoCriada) {
            return null;
        }
        return emailService.enviarEmailAlerta(config.getEmailGestor(), assunto, mensagem).status();
    }

    private boolean finalizada(StatusOS status) {
        return status == StatusOS.CONCLUIDA || status == StatusOS.FATURADA;
    }

    private ConfiguracaoNotificacao configuracaoPadrao() {
        return new ConfiguracaoNotificacao(1L, "", "", true, true, true, 24, 30);
    }

    private int inteiroPositivo(Integer valor, int padrao) {
        return valor == null || valor <= 0 ? padrao : valor;
    }

    private BigDecimal saldoLivre(SaldoMaterialLocal saldoLocal, Material material) {
        if (controlaMetragem(material)) {
            return valor(saldoLocal.getMetragemDisponivel())
                    .subtract(valor(saldoLocal.getMetragemReservada()));
        }
        return BigDecimal.valueOf(valor(saldoLocal.getQuantidadeDisponivel())
                - valor(saldoLocal.getQuantidadeReservada()));
    }

    private boolean controlaMetragem(Material material) {
        return TipoControleEstoque.METRAGEM.equals(material.getTipoControle())
                || TipoControleEstoque.BOBINA.equals(material.getTipoControle())
                || TipoControleEstoque.ROLO.equals(material.getTipoControle());
    }

    private int valor(Integer valor) {
        return valor != null ? valor : 0;
    }

    private BigDecimal valor(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private String formatar(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString();
    }

    public record VarreduraResultado(
            int alertasEncontrados,
            int notificacoesCriadas,
            int notificacoesJaExistentes,
            int notificacoesResolvidas,
            LocalDateTime executadaEm,
            String canal,
            int emailsEnviados,
            int emailsFalhos,
            boolean emailHabilitado) {
    }
}
