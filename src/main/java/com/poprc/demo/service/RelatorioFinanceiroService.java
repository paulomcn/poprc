package com.poprc.demo.service;

import com.poprc.demo.dto.RelatorioLucratividadeDTO;
import com.poprc.demo.dto.RelatorioLucratividadeOsDTO;
import com.poprc.demo.model.Contrato;
import com.poprc.demo.model.Faturamento;
import com.poprc.demo.model.MovimentacaoEstoque;
import com.poprc.demo.model.OrdemServico;
import com.poprc.demo.model.PrestacaoContas;
import com.poprc.demo.model.Projeto;
import com.poprc.demo.model.TipoMovimentacao;
import com.poprc.demo.repository.FaturamentoRepository;
import com.poprc.demo.repository.ContratoRepository;
import com.poprc.demo.repository.MovimentacaoEstoqueRepository;
import com.poprc.demo.repository.OrdemServicoRepository;
import com.poprc.demo.repository.PrestacaoContasRepository;
import com.poprc.demo.repository.ProjetoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RelatorioFinanceiroService {

    private final ProjetoRepository projetoRepository;
    private final ContratoRepository contratoRepository;
    private final FaturamentoRepository faturamentoRepository;
    private final PrestacaoContasRepository prestacaoContasRepository;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    private final OrdemServicoRepository ordemServicoRepository;

    @Transactional(readOnly = true)
    public RelatorioLucratividadeDTO gerarRelatorioLucratividade(Long projetoId) {
        return gerarRelatorioLucratividade(null, projetoId, null);
    }

    @Transactional(readOnly = true)
    public RelatorioLucratividadeDTO gerarRelatorioLucratividade(
            Long contratoId, Long projetoId, Long ordemServicoId) {
        EscopoFinanceiro escopo = resolverEscopo(contratoId, projetoId, ordemServicoId);

        List<Faturamento> faturamentos = escopo.projeto() != null
                ? faturamentoRepository.findByProjetoId(escopo.projeto().getId())
                : faturamentoRepository.findByContratoId(escopo.contrato().getId());
        List<MovimentacaoEstoque> movimentacoes = escopo.projetos().stream()
                .flatMap(projeto -> movimentacaoEstoqueRepository
                        .findByProjetoIdOrderByDataMovimentacaoDesc(projeto.getId()).stream())
                .toList();
        List<PrestacaoContas> prestacoesProjeto = escopo.projetos().stream()
                .flatMap(projeto -> prestacaoContasRepository
                        .findByViagemProjetoId(projeto.getId()).stream())
                .toList();

        if (escopo.ordemServico() != null) {
            Long osId = escopo.ordemServico().getId();
            faturamentos = faturamentos.stream()
                    .filter(faturamento -> faturamento.getOrdemServico() != null
                            && osId.equals(faturamento.getOrdemServico().getId()))
                    .toList();
            movimentacoes = movimentacoes.stream()
                    .filter(movimentacao -> movimentacao.getOrdemServico() != null
                            && osId.equals(movimentacao.getOrdemServico().getId()))
                    .toList();
        }

        BigDecimal totalFaturado = somarFaturamentos(faturamentos);
        BigDecimal totalCustoViagens = escopo.ordemServico() == null
                ? somarPrestacoes(prestacoesProjeto)
                : BigDecimal.ZERO;
        BigDecimal custoViagensNaoAlocado = escopo.ordemServico() != null
                ? somarPrestacoes(prestacoesProjeto)
                : BigDecimal.ZERO;
        TotaisMateriais materiais = calcularMateriais(movimentacoes);
        BigDecimal custoTotalAcumulado = totalCustoViagens.add(materiais.custoLiquido());
        BigDecimal lucroBruto = totalFaturado.subtract(custoTotalAcumulado);

        List<Faturamento> faturamentosConsolidados = faturamentos;
        List<MovimentacaoEstoque> movimentacoesConsolidadas = movimentacoes;
        List<RelatorioLucratividadeOsDTO> detalhesOs = escopo.ordensServico().stream()
                .map(ordem -> criarDetalheOs(
                        ordem, faturamentosConsolidados, movimentacoesConsolidadas))
                .toList();

        return RelatorioLucratividadeDTO.builder()
                .contratoId(escopo.contrato().getId())
                .numeroContrato(escopo.contrato().getContrato())
                .projetoId(escopo.projeto() != null ? escopo.projeto().getId() : null)
                .nomeProjeto(escopo.projeto() != null
                        ? "Projeto #" + escopo.projeto().getId()
                        : "Todos os projetos do contrato")
                .ordemServicoId(escopo.ordemServico() != null ? escopo.ordemServico().getId() : null)
                .numeroOs(escopo.ordemServico() != null ? escopo.ordemServico().getNumeroOs() : null)
                .totalFaturado(dinheiro(totalFaturado))
                .totalCustoViagens(dinheiro(totalCustoViagens))
                .totalCustoMateriais(dinheiro(materiais.custoLiquido()))
                .custoMateriaisDisponivel(materiais.disponivel())
                .custoMateriaisEstimado(materiais.estimado())
                .resultadoFinanceiroParcial(!materiais.disponivel())
                .custoTotalAcumulado(dinheiro(custoTotalAcumulado))
                .lucroBruto(dinheiro(lucroBruto))
                .margemLucro(calcularMargem(lucroBruto, totalFaturado))
                .saudeFinanceira(calcularSaude(lucroBruto, totalFaturado, custoTotalAcumulado))
                .receitaSemOrdemServico(dinheiro(somarFaturamentosSemOs(faturamentos)))
                .custoMateriaisSemOrdemServico(dinheiro(calcularMateriaisSemOs(movimentacoes)))
                .custoViagensNaoAlocado(dinheiro(custoViagensNaoAlocado))
                .ordensServico(detalhesOs)
                .build();
    }

    private EscopoFinanceiro resolverEscopo(Long contratoId, Long projetoId, Long ordemServicoId) {
        OrdemServico ordemServico = ordemServicoId != null
                ? ordemServicoRepository.findById(ordemServicoId)
                        .orElseThrow(() -> new IllegalArgumentException("Ordem de Serviço não encontrada."))
                : null;
        Projeto projeto = ordemServico != null
                ? ordemServico.getProjeto()
                : projetoId != null
                        ? projetoRepository.findById(projetoId)
                                .orElseThrow(() -> new IllegalArgumentException("Projeto não encontrado."))
                        : null;
        Contrato contrato = ordemServico != null && ordemServico.getContrato() != null
                ? ordemServico.getContrato()
                : projeto != null ? projeto.getContrato() : null;

        if (contrato == null && contratoId != null) {
            contrato = contratoRepository.findById(contratoId)
                    .orElseThrow(() -> new IllegalArgumentException("Contrato não encontrado."));
        }
        if (contrato == null) {
            throw new IllegalArgumentException("Informe um contrato, projeto ou Ordem de Serviço.");
        }
        if (contratoId != null && !contratoId.equals(contrato.getId())) {
            throw new IllegalArgumentException("O projeto ou a OS não pertence ao contrato informado.");
        }
        if (projetoId != null && (projeto == null || !projetoId.equals(projeto.getId()))) {
            throw new IllegalArgumentException("A OS não pertence ao projeto informado.");
        }

        List<Projeto> projetos = projeto != null
                ? List.of(projeto)
                : projetoRepository.findByContratoId(contrato.getId());
        List<OrdemServico> ordens = ordemServico != null
                ? List.of(ordemServico)
                : projeto != null
                        ? ordemServicoRepository.findByProjetoId(projeto.getId())
                        : ordemServicoRepository.findByContratoId(contrato.getId());
        return new EscopoFinanceiro(contrato, projeto, ordemServico, projetos, ordens);
    }

    private RelatorioLucratividadeOsDTO criarDetalheOs(OrdemServico ordem,
            List<Faturamento> faturamentos, List<MovimentacaoEstoque> movimentacoes) {
        List<Faturamento> faturamentosOs = faturamentos.stream()
                .filter(faturamento -> faturamento.getOrdemServico() != null
                        && ordem.getId().equals(faturamento.getOrdemServico().getId()))
                .toList();
        List<MovimentacaoEstoque> movimentacoesOs = movimentacoes.stream()
                .filter(movimentacao -> movimentacao.getOrdemServico() != null
                        && ordem.getId().equals(movimentacao.getOrdemServico().getId()))
                .toList();
        BigDecimal receita = somarFaturamentos(faturamentosOs);
        TotaisMateriais materiais = calcularMateriais(movimentacoesOs);
        BigDecimal lucro = receita.subtract(materiais.custoLiquido());
        return RelatorioLucratividadeOsDTO.builder()
                .ordemServicoId(ordem.getId())
                .numeroOs(ordem.getNumeroOs())
                .projetoId(ordem.getProjeto() != null ? ordem.getProjeto().getId() : null)
                .status(ordem.getStatus() != null ? ordem.getStatus().name() : null)
                .totalFaturado(dinheiro(receita))
                .totalCustoMateriais(dinheiro(materiais.custoLiquido()))
                .lucroOperacional(dinheiro(lucro))
                .margemLucro(calcularMargem(lucro, receita))
                .custoMateriaisDisponivel(materiais.disponivel())
                .custoMateriaisEstimado(materiais.estimado())
                .build();
    }

    private TotaisMateriais calcularMateriais(List<MovimentacaoEstoque> movimentacoes) {
        List<MovimentacaoEstoque> retiradas = movimentacoes.stream()
                .filter(movimentacao -> TipoMovimentacao.RETIRADA_OR.equals(movimentacao.getTipo()))
                .toList();
        BigDecimal totalRetirado = somarMovimentacoes(retiradas, TipoMovimentacao.RETIRADA_OR);
        BigDecimal totalDevolvido = somarMovimentacoes(movimentacoes, TipoMovimentacao.DEVOLUCAO_OR);
        boolean disponivel = retiradas.stream()
                .noneMatch(movimentacao -> valor(movimentacao.getValorTotalMovimentacao()).signum() <= 0);
        boolean estimado = movimentacoes.stream()
                .filter(movimentacao -> TipoMovimentacao.RETIRADA_OR.equals(movimentacao.getTipo())
                        || TipoMovimentacao.DEVOLUCAO_OR.equals(movimentacao.getTipo()))
                .anyMatch(movimentacao -> Boolean.TRUE.equals(movimentacao.getCustoEstimado()));
        return new TotaisMateriais(totalRetirado.subtract(totalDevolvido), disponivel, estimado);
    }

    private BigDecimal somarFaturamentos(List<Faturamento> faturamentos) {
        return faturamentos.stream()
                .filter(Objects::nonNull)
                .map(Faturamento::getValorMedicao)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal somarPrestacoes(List<PrestacaoContas> prestacoes) {
        return prestacoes.stream()
                .filter(Objects::nonNull)
                .map(PrestacaoContas::getCustoReal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal somarMovimentacoes(List<MovimentacaoEstoque> movimentacoes, TipoMovimentacao tipo) {
        return movimentacoes.stream()
                .filter(Objects::nonNull)
                .filter(movimentacao -> tipo.equals(movimentacao.getTipo()))
                .map(MovimentacaoEstoque::getValorTotalMovimentacao)
                .map(this::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal somarFaturamentosSemOs(List<Faturamento> faturamentos) {
        return faturamentos.stream()
                .filter(faturamento -> faturamento.getOrdemServico() == null)
                .map(Faturamento::getValorMedicao)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularMateriaisSemOs(List<MovimentacaoEstoque> movimentacoes) {
        return calcularMateriais(movimentacoes.stream()
                .filter(movimentacao -> movimentacao.getOrdemServico() == null)
                .toList()).custoLiquido();
    }

    private BigDecimal calcularMargem(BigDecimal lucro, BigDecimal receita) {
        if (receita.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return lucro.divide(receita, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String calcularSaude(BigDecimal lucro, BigDecimal receita, BigDecimal custo) {
        if (receita.signum() <= 0 && custo.signum() == 0) {
            return "SEM_MOVIMENTACAO";
        }
        BigDecimal margem = calcularMargem(lucro, receita);
        if (margem.compareTo(new BigDecimal("20.00")) > 0) {
            return "LUCRO_SAUDAVEL";
        }
        if (margem.signum() >= 0 && receita.signum() > 0) {
            return "ALERTA_MARGEM_BAIXA";
        }
        return "PREJUIZO_CRITICO";
    }

    private BigDecimal valor(BigDecimal numero) {
        return numero != null ? numero : BigDecimal.ZERO;
    }

    private BigDecimal dinheiro(BigDecimal numero) {
        return valor(numero).setScale(2, RoundingMode.HALF_UP);
    }

    private record TotaisMateriais(BigDecimal custoLiquido, boolean disponivel, boolean estimado) {
    }

    private record EscopoFinanceiro(
            Contrato contrato,
            Projeto projeto,
            OrdemServico ordemServico,
            List<Projeto> projetos,
            List<OrdemServico> ordensServico) {
    }
}
