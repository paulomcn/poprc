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

@Service
@EnableScheduling
@RequiredArgsConstructor
public class AgendadorAlertasService {

    private final OrdemServicoRepository osRepository;
    private final SaldoMaterialLocalRepository saldoMaterialLocalRepository;
    private final ContratoRepository contratoRepository;
    private final ConfiguracaoNotificacaoRepository configRepository;
    private final NotificacaoOperacionalService notificacaoService;

    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public VarreduraResultado executarVarreduraDiariaDeAlertas() {
        ConfiguracaoNotificacao config = configRepository.findById(1L)
                .orElseGet(() -> new ConfiguracaoNotificacao(1L, "", "", true, true, true));
        LocalDateTime agora = LocalDateTime.now();
        LocalDate hoje = agora.toLocalDate();
        int encontrados = 0;
        int novos = 0;

        if (config.isAlertaOsAtrasada()) {
            for (OrdemServico os : osRepository.findAll()) {
                if (os.getDeadline() == null || finalizada(os.getStatus())) {
                    continue;
                }

                Funcionario responsavel = os.getProjeto() == null ? null : os.getProjeto().getResponsavel();
                if (os.getDeadline().isBefore(agora)) {
                    encontrados++;
                    boolean criada = notificacaoService.registrarSeAusente(
                            "OS_ATRASADA:" + os.getId() + ":" + os.getDeadline(),
                            "OS_ATRASADA",
                            "CRITICA",
                            "OS " + os.getNumeroOs() + " atrasada",
                            "O prazo venceu em " + os.getDeadline() + ". Regularize a execução ou atualize o cronograma.",
                            os,
                            responsavel);
                    if (criada) novos++;
                } else if (!os.getDeadline().isAfter(agora.plusHours(24))) {
                    encontrados++;
                    boolean criada = notificacaoService.registrarSeAusente(
                            "OS_PRAZO_24H:" + os.getId() + ":" + os.getDeadline(),
                            "OS_PRAZO_24H",
                            "ALERTA",
                            "OS " + os.getNumeroOs() + " próxima do prazo",
                            "Restam menos de 24 horas para o deadline de " + os.getDeadline() + ".",
                            os,
                            responsavel);
                    if (criada) novos++;
                }
            }
        }

        if (config.isAlertaEstoqueCritico()) {
            for (SaldoMaterialLocal saldoLocal : saldoMaterialLocalRepository
                    .findAllByOrderByMaterialNomeAscLocalEstoqueNomeAsc()) {
                Material material = saldoLocal.getMaterial();
                if (material == null || saldoLocal.getLocalEstoque() == null
                        || Boolean.FALSE.equals(saldoLocal.getLocalEstoque().getAtivo())) {
                    continue;
                }

                BigDecimal minimo = valor(material.getEstoqueMinimo());
                if (minimo.signum() <= 0) {
                    continue;
                }

                BigDecimal saldoLivre = saldoLivre(saldoLocal, material);
                if (saldoLivre.compareTo(minimo) > 0) {
                    continue;
                }

                String local = saldoLocal.getLocalEstoque().getNome();
                String unidade = controlaMetragem(material) ? "m" : material.getUnidadeMedida().name();
                encontrados++;
                boolean criada = notificacaoService.registrarSeAusente(
                        "ESTOQUE_CRITICO:" + material.getId() + ":" + saldoLocal.getLocalEstoque().getId(),
                        "ESTOQUE_CRITICO",
                        "ALERTA",
                        "Estoque crítico: " + material.getNome() + " em " + local,
                        "Depósito: " + local + ". Saldo livre: " + formatar(saldoLivre) + " " + unidade
                                + ". Mínimo configurado: " + formatar(minimo) + " " + unidade + ".",
                        null,
                        null);
                if (criada) novos++;
            }
        }

        if (config.isAlertaContratoVencendo()) {
            for (Contrato contrato : contratoRepository.findAll()) {
                if (contrato.getVigenciaFim() == null) {
                    continue;
                }
                long diasParaVencer = ChronoUnit.DAYS.between(hoje, contrato.getVigenciaFim());
                if (diasParaVencer < 0 || diasParaVencer > 30) {
                    continue;
                }
                encontrados++;
                boolean criada = notificacaoService.registrarSeAusente(
                        "CONTRATO_VENCENDO:" + contrato.getId() + ":" + contrato.getVigenciaFim(),
                        "CONTRATO_VENCENDO",
                        "INFORMATIVA",
                        "Contrato próximo do vencimento",
                        "Contrato " + contrato.getContrato() + " de " + contrato.getCliente()
                                + " vence em " + contrato.getVigenciaFim() + ".",
                        null,
                        null);
                if (criada) novos++;
            }
        }

        return new VarreduraResultado(encontrados, novos, encontrados - novos, agora, "INTERNO");
    }

    private boolean finalizada(StatusOS status) {
        return status == StatusOS.CONCLUIDA || status == StatusOS.FATURADA;
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
            LocalDateTime executadaEm,
            String canal) {
    }
}
