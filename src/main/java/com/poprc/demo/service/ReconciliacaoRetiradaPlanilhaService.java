package com.poprc.demo.service;

import com.poprc.demo.dto.ReconciliacaoRetiradasPlanilhaDTO;
import com.poprc.demo.dto.ReconciliacaoRetiradasPlanilhaRequest;
import com.poprc.demo.dto.EdicaoRetiradaHistoricaRequest;
import com.poprc.demo.model.ImportacaoRetiradaPlanilha;
import com.poprc.demo.model.MaterialItem;
import com.poprc.demo.model.OrdemRetiradaItem;
import com.poprc.demo.model.ReconciliacaoRetiradaPlanilha;
import com.poprc.demo.repository.ImportacaoRetiradaPlanilhaRepository;
import com.poprc.demo.repository.MaterialItemRepository;
import com.poprc.demo.repository.OrdemRetiradaItemRepository;
import com.poprc.demo.repository.ReconciliacaoRetiradaPlanilhaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReconciliacaoRetiradaPlanilhaService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(3);

    private final ImportacaoRetiradaPlanilhaRepository retiradaRepository;
    private final ReconciliacaoRetiradaPlanilhaRepository reconciliacaoRepository;
    private final OrdemRetiradaItemRepository ordemRetiradaItemRepository;
    private final MaterialItemRepository materialItemRepository;

    @Transactional
    public ReconciliacaoRetiradasPlanilhaDTO.Resultado reconciliar(
            ReconciliacaoRetiradasPlanilhaRequest request,
            String usuario) {
        validarRequest(request);
        Set<Long> idsRecebidos = new HashSet<>();
        List<Alteracao> alteracoes = request.itens().stream()
                .map(item -> prepararAlteracao(item, idsRecebidos))
                .filter(Alteracao::divergente)
                .toList();
        List<ReconciliacaoRetiradasPlanilhaDTO.Divergencia> divergencias = alteracoes.stream()
                .map(this::mapearDivergencia)
                .toList();

        if (request.confirmar()) {
            String responsavel = usuario == null || usuario.isBlank()
                    ? "Usuário autenticado"
                    : usuario.trim();
            alteracoes.forEach(alteracao -> aplicar(
                    alteracao,
                    request.nomeArquivo().trim(),
                    request.hashSha256().toLowerCase(),
                    responsavel,
                    "PLANILHA",
                    "Reconciliação confirmada a partir da planilha de origem."));
        }

        return new ReconciliacaoRetiradasPlanilhaDTO.Resultado(
                request.confirmar(),
                alteracoes.size(),
                divergencias);
    }

    @Transactional(readOnly = true)
    public List<ReconciliacaoRetiradasPlanilhaDTO.Evento> listarHistorico() {
        return reconciliacaoRepository.findTop100ByOrderByReconciliadoEmDesc().stream()
                .map(evento -> new ReconciliacaoRetiradasPlanilhaDTO.Evento(
                        evento.getId(),
                        evento.getRetiradaImportada().getId(),
                        evento.getAbaOrigem(),
                        evento.getMaterial().getNome(),
                        evento.getNomeArquivo(),
                        evento.getOrigem(),
                        evento.getMotivo(),
                        evento.getQuantidadeAnterior(),
                        evento.getQuantidadeNova(),
                        evento.getReconciliadoPor(),
                        evento.getReconciliadoEm()))
                .toList();
    }

    @Transactional
    public ReconciliacaoRetiradasPlanilhaDTO.Evento editarHistorico(
            Long retiradaId,
            EdicaoRetiradaHistoricaRequest request,
            String usuario) {
        if (retiradaId == null) {
            throw new IllegalArgumentException("Informe a retirada histórica.");
        }
        if (request == null) {
            throw new IllegalArgumentException("Informe os dados da correção.");
        }
        BigDecimal retirada = quantidade(request.quantidadeRetirada(), "quantidade retirada");
        String motivo = validarMotivo(request.motivo());
        ImportacaoRetiradaPlanilha atual = retiradaRepository.findById(retiradaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Retirada histórica não encontrada: " + retiradaId + "."));
        BigDecimal saldoInicial = atual.getSaldoInicial().setScale(3, RoundingMode.HALF_UP);
        BigDecimal saldoFinal = saldoInicial.subtract(retirada).setScale(3, RoundingMode.HALF_UP);
        Alteracao alteracao = new Alteracao(
                atual,
                saldoInicial,
                retirada,
                saldoFinal,
                saldoFinal.signum() < 0 ? saldoFinal.abs() : ZERO,
                request.dataRetirada());
        if (!alteracao.divergente()) {
            throw new IllegalArgumentException("Nenhuma alteração foi informada para esta retirada.");
        }
        String responsavel = usuario == null || usuario.isBlank()
                ? "Usuário autenticado"
                : usuario.trim();
        ReconciliacaoRetiradaPlanilha evento = aplicar(
                alteracao,
                "Edição manual do histórico",
                gerarHashManual(atual.getId(), retirada, responsavel),
                responsavel,
                "EDICAO_MANUAL",
                motivo);
        return mapearEvento(evento);
    }

    private Alteracao prepararAlteracao(
            ReconciliacaoRetiradasPlanilhaRequest.Item item,
            Set<Long> idsRecebidos) {
        if (item == null || item.retiradaImportadaId() == null
                || !idsRecebidos.add(item.retiradaImportadaId())) {
            throw new IllegalArgumentException(
                    "Cada retirada histórica deve ser informada uma única vez.");
        }
        ImportacaoRetiradaPlanilha atual = retiradaRepository
                .findById(item.retiradaImportadaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Retirada histórica não encontrada: " + item.retiradaImportadaId() + "."));
        BigDecimal saldoInicial = quantidade(item.saldoInicial(), "saldo inicial");
        BigDecimal retirada = quantidade(item.quantidadeRetirada(), "quantidade retirada");
        BigDecimal saldoFinal = numero(item.saldoFinal(), "saldo final");
        BigDecimal saldoCalculado = saldoInicial.subtract(retirada).setScale(3, RoundingMode.HALF_UP);
        if (saldoCalculado.compareTo(saldoFinal) != 0) {
            throw new IllegalArgumentException(
                    "O saldo final de " + atual.getMaterial().getNome()
                            + " não corresponde ao saldo inicial menos a retirada.");
        }
        return new Alteracao(
                atual,
                saldoInicial,
                retirada,
                saldoFinal,
                saldoFinal.signum() < 0 ? saldoFinal.abs() : ZERO,
                item.dataRetirada());
    }

    private ReconciliacaoRetiradaPlanilha aplicar(
            Alteracao alteracao,
            String nomeArquivo,
            String hash,
            String usuario,
            String origem,
            String motivo) {
        ImportacaoRetiradaPlanilha atual = alteracao.atual();
        if (reconciliacaoRepository.existsByRetiradaImportadaIdAndHashOrigem(
                atual.getId(), hash)) {
            throw new IllegalArgumentException(
                    "Esta correção já foi aplicada para " + atual.getMaterial().getNome() + ".");
        }

        BigDecimal efetivaAnterior = quantidadeEfetiva(
                atual.getSaldoInicial(), atual.getQuantidadeRetirada());
        BigDecimal efetivaNova = quantidadeEfetiva(
                alteracao.saldoInicial(), alteracao.quantidadeRetirada());
        BigDecimal deltaPrevisto = alteracao.quantidadeRetirada()
                .subtract(atual.getQuantidadeRetirada());
        BigDecimal deltaAuditado = efetivaNova.subtract(efetivaAnterior);

        ReconciliacaoRetiradaPlanilha evento = new ReconciliacaoRetiradaPlanilha();
        evento.setRetiradaImportada(atual);
        evento.setMaterial(atual.getMaterial());
        evento.setAbaOrigem(atual.getAbaOrigem());
        evento.setNomeArquivo(nomeArquivo);
        evento.setHashOrigem(hash);
        evento.setOrigem(origem);
        evento.setMotivo(motivo);
        evento.setQuantidadeAnterior(atual.getQuantidadeRetirada());
        evento.setQuantidadeNova(alteracao.quantidadeRetirada());
        evento.setSaldoInicialAnterior(atual.getSaldoInicial());
        evento.setSaldoInicialNovo(alteracao.saldoInicial());
        evento.setSaldoFinalAnterior(atual.getSaldoFinal());
        evento.setSaldoFinalNovo(alteracao.saldoFinal());
        evento.setFaltaAnterior(atual.getQuantidadeFaltante());
        evento.setFaltaNova(alteracao.falta());
        evento.setDataRetiradaAnterior(atual.getDataRetirada());
        evento.setDataRetiradaNova(alteracao.dataRetirada());
        evento.setReconciliadoPor(usuario);
        evento.setReconciliadoEm(LocalDateTime.now());
        reconciliacaoRepository.save(evento);

        if (atual.getOrdemRetirada() != null) {
            OrdemRetiradaItem itemOr = ordemRetiradaItemRepository
                    .findByOrdemRetiradaIdAndMaterialId(
                            atual.getOrdemRetirada().getId(), atual.getMaterial().getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "O item correspondente não foi encontrado na OR histórica."));
            itemOr.setQuantidadeSolicitada(alteracao.quantidadeRetirada());
            itemOr.setQuantidadeRetirada(efetivaNova);
            ordemRetiradaItemRepository.save(itemOr);

            MaterialItem itemObra = itemOr.getMaterialItem();
            if (itemObra != null) {
                itemObra.setQuantidadePrevista(
                        naoNegativo(itemObra.getQuantidadePrevista().add(deltaPrevisto)));
                itemObra.setQuantidadeAuditada(
                        naoNegativo(itemObra.getQuantidadeAuditada().add(deltaAuditado)));
                materialItemRepository.save(itemObra);
            }
        }

        atual.setSaldoInicial(alteracao.saldoInicial());
        atual.setQuantidadeRetirada(alteracao.quantidadeRetirada());
        atual.setSaldoFinal(alteracao.saldoFinal());
        atual.setQuantidadeFaltante(alteracao.falta());
        atual.setDataRetirada(alteracao.dataRetirada());
        retiradaRepository.save(atual);
        return evento;
    }

    private ReconciliacaoRetiradasPlanilhaDTO.Evento mapearEvento(
            ReconciliacaoRetiradaPlanilha evento) {
        return new ReconciliacaoRetiradasPlanilhaDTO.Evento(
                evento.getId(),
                evento.getRetiradaImportada().getId(),
                evento.getAbaOrigem(),
                evento.getMaterial().getNome(),
                evento.getNomeArquivo(),
                evento.getOrigem(),
                evento.getMotivo(),
                evento.getQuantidadeAnterior(),
                evento.getQuantidadeNova(),
                evento.getReconciliadoPor(),
                evento.getReconciliadoEm());
    }

    private ReconciliacaoRetiradasPlanilhaDTO.Divergencia mapearDivergencia(Alteracao alteracao) {
        ImportacaoRetiradaPlanilha atual = alteracao.atual();
        return new ReconciliacaoRetiradasPlanilhaDTO.Divergencia(
                atual.getId(),
                atual.getAbaOrigem(),
                atual.getMaterial().getId(),
                atual.getMaterial().getNome(),
                atual.getQuantidadeRetirada(),
                alteracao.quantidadeRetirada(),
                atual.getSaldoInicial(),
                alteracao.saldoInicial(),
                atual.getSaldoFinal(),
                alteracao.saldoFinal(),
                atual.getQuantidadeFaltante(),
                alteracao.falta());
    }

    private void validarRequest(ReconciliacaoRetiradasPlanilhaRequest request) {
        if (request == null || request.nomeArquivo() == null || request.nomeArquivo().isBlank()) {
            throw new IllegalArgumentException("Informe o nome da planilha de origem.");
        }
        if (request.nomeArquivo().length() > 255) {
            throw new IllegalArgumentException("O nome da planilha é muito longo.");
        }
        if (request.hashSha256() == null
                || !request.hashSha256().matches("(?i)^[a-f0-9]{64}$")) {
            throw new IllegalArgumentException("O hash SHA-256 da planilha é inválido.");
        }
        if (request.itens() == null || request.itens().isEmpty()) {
            throw new IllegalArgumentException("Nenhuma retirada foi informada para comparação.");
        }
        if (request.itens().size() > 5000) {
            throw new IllegalArgumentException("A planilha excede o limite de retiradas por operação.");
        }
    }

    private String validarMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Informe o motivo da correção histórica.");
        }
        String valor = motivo.trim();
        if (valor.length() < 5) {
            throw new IllegalArgumentException("Descreva o motivo da correção com pelo menos 5 caracteres.");
        }
        if (valor.length() > 500) {
            throw new IllegalArgumentException("O motivo da correção deve ter no máximo 500 caracteres.");
        }
        return valor;
    }

    private String gerarHashManual(Long retiradaId, BigDecimal quantidade, String usuario) {
        String origem = "MANUAL|" + retiradaId + "|" + quantidade.toPlainString()
                + "|" + usuario + "|" + LocalDateTime.now() + "|" + java.util.UUID.randomUUID();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(origem.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 não está disponível no servidor.", exception);
        }
    }

    private BigDecimal quantidade(BigDecimal valor, String campo) {
        BigDecimal numero = numero(valor, campo);
        if (numero.signum() < 0) {
            throw new IllegalArgumentException("O campo " + campo + " não pode ser negativo.");
        }
        return numero;
    }

    private BigDecimal numero(BigDecimal valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException("Informe o campo " + campo + ".");
        }
        return valor.setScale(3, RoundingMode.HALF_UP);
    }

    private BigDecimal quantidadeEfetiva(BigDecimal saldoInicial, BigDecimal retirada) {
        return retirada.min(saldoInicial.max(BigDecimal.ZERO)).max(BigDecimal.ZERO);
    }

    private BigDecimal naoNegativo(BigDecimal valor) {
        if (valor.signum() < 0) {
            throw new IllegalArgumentException(
                    "A correção produziria uma quantidade negativa no consolidado da obra.");
        }
        return valor;
    }

    private record Alteracao(
            ImportacaoRetiradaPlanilha atual,
            BigDecimal saldoInicial,
            BigDecimal quantidadeRetirada,
            BigDecimal saldoFinal,
            BigDecimal falta,
            java.time.LocalDate dataRetirada) {

        private boolean divergente() {
            return atual.getSaldoInicial().compareTo(saldoInicial) != 0
                    || atual.getQuantidadeRetirada().compareTo(quantidadeRetirada) != 0
                    || atual.getSaldoFinal().compareTo(saldoFinal) != 0
                    || atual.getQuantidadeFaltante().compareTo(falta) != 0
                    || !java.util.Objects.equals(atual.getDataRetirada(), dataRetirada);
        }
    }
}
