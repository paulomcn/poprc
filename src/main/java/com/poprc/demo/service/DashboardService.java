package com.poprc.demo.service;

import com.poprc.demo.dto.DashboardIndicadoresDTO;
import com.poprc.demo.model.*;
import com.poprc.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ContratoRepository contratoRepository;
    private final FaturamentoRepository faturamentoRepository;
    private final ComarcaRepository comarcaRepository;
    private final PrestacaoContasRepository prestacaoContasRepository;
    private final OrdemServicoRepository ordemServicoRepository;
    private final MaterialRepository materialRepository;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

    @Transactional(readOnly = true)
    public DashboardIndicadoresDTO calcularIndicadores(String filtroContrato, LocalDate inicio, LocalDate fim) {

        // 1. Carrega as coleções da base de dados
        List<Contrato> todosContratos = contratoRepository.findAll();
        List<Faturamento> todosFaturamentos = faturamentoRepository.findAll();
        List<Comarca> todasComarcas = comarcaRepository.findAll();
        List<PrestacaoContas> todasPrestacoes = prestacaoContasRepository.findAll();
        List<OrdemServico> todasOrdens = ordemServicoRepository.findAll();
        List<Material> todosMateriais = materialRepository.findAll();
        List<MovimentacaoEstoque> todasMovimentacoes = movimentacaoEstoqueRepository.findAll();

        List<Contrato> contratosFiltrados = todosContratos.stream()
                .filter(c -> !Boolean.TRUE.equals(c.getArquivado()))
                .filter(c -> filtroContrato == null || filtroContrato.trim().isEmpty()
                        || filtroContrato.equalsIgnoreCase(c.getContrato()))
                .filter(c -> contratoDentroDoPeriodo(c, inicio, fim))
                .toList();
        Set<Long> contratosSelecionados = contratosFiltrados.stream()
                .map(Contrato::getId)
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));

        // ️ 2. Filtro e contagem de Contratos Ativos
        long contratosAtivos = contratosFiltrados.stream()
                .filter(c -> "ATIVO".equalsIgnoreCase(String.valueOf(c.getStatus())))
                .count();

        // 3. Cálculo do Valor Alocado Global (Lambda segura adicionada )
        BigDecimal valorTotalContratado = contratosFiltrados.stream()
                .map(c -> c.getValorGlobal() != null ? c.getValorGlobal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

        List<Faturamento> faturamentosFiltrados = todosFaturamentos.stream()
                .filter(f -> f.getContrato() != null)
                .filter(f -> contratosSelecionados.contains(f.getContrato().getId()))
                .filter(f -> dentroDoPeriodo(f.getDataVencimento(), inicio, fim))
                .toList();

        BigDecimal valorReceitaRegistrada = faturamentosFiltrados.stream()
                .map(f -> valor(f.getValorMedicao()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. Faturamento Dinâmico realizado (Lambda segura adicionada )
        BigDecimal valorFaturado = faturamentosFiltrados.stream()
                .filter(f -> "PAGO".equalsIgnoreCase(String.valueOf(f.getSituacao()))
                        || "LIQUIDADO".equalsIgnoreCase(String.valueOf(f.getSituacao())))
                .map(f -> valor(f.getValorMedicao()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. Faturamento Dinâmico pendente (Lambda segura adicionada )
        BigDecimal valorPendenteFaturamento = faturamentosFiltrados.stream()
                .filter(f -> "PENDENTE".equalsIgnoreCase(String.valueOf(f.getSituacao()))
                        || "A_RECEBER".equalsIgnoreCase(String.valueOf(f.getSituacao()))
                        || "A_FATURAR".equalsIgnoreCase(String.valueOf(f.getSituacao()))
                        || "FATURADO".equalsIgnoreCase(String.valueOf(f.getSituacao()))
                        || "EM_ATRASO".equalsIgnoreCase(String.valueOf(f.getSituacao())))
                .map(f -> valor(f.getValorMedicao()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate hoje = LocalDate.now();
        List<Faturamento> faturamentosAtrasados = faturamentosFiltrados.stream()
                .filter(f -> !"PAGO".equalsIgnoreCase(String.valueOf(f.getSituacao()))
                        && !"LIQUIDADO".equalsIgnoreCase(String.valueOf(f.getSituacao())))
                .filter(f -> "EM_ATRASO".equalsIgnoreCase(String.valueOf(f.getSituacao()))
                        || f.getDataVencimento() != null && f.getDataVencimento().isBefore(hoje))
                .toList();
        BigDecimal valorFaturamentoEmAtraso = faturamentosAtrasados.stream()
                .map(f -> valor(f.getValorMedicao()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ️ 6. Status de Comarcas
        List<OrdemServico> ordensFiltradas = todasOrdens.stream()
                .filter(os -> !Boolean.TRUE.equals(os.getArquivado()))
                .filter(os -> os.getContrato() != null && contratosSelecionados.contains(os.getContrato().getId()))
                .toList();

        List<Comarca> comarcasFiltradas = todasComarcas.stream()
                .filter(c -> !Boolean.TRUE.equals(c.getArquivado()))
                .filter(c -> pertenceAContratoSelecionado(c, contratosSelecionados))
                .toList();

        long totalComarcasConcluidas = comarcasFiltradas.stream()
                .filter(this::obraConcluida)
                .count();

        LocalDateTime agora = LocalDateTime.now();
        long totalComarcasEmAtraso = ordensFiltradas.stream()
                .filter(os -> !ordemConcluida(os))
                .filter(os -> os.getDeadline() != null && os.getDeadline().isBefore(agora))
                .count();

        long ordensProximasPrazo = ordensFiltradas.stream()
                .filter(os -> !ordemConcluida(os))
                .filter(os -> os.getDeadline() != null
                        && !os.getDeadline().isBefore(agora)
                        && !os.getDeadline().isAfter(agora.plusHours(24)))
                .count();

        long ordensAbertas = ordensFiltradas.stream()
                .filter(os -> os.getStatus() == StatusOS.ABERTA
                        || os.getStatus() == StatusOS.AGUARDANDO_VISTORIA
                        || os.getStatus() == StatusOS.AGUARDANDO_RETIRADA)
                .count();
        long ordensEmExecucao = ordensFiltradas.stream()
                .filter(os -> os.getStatus() == StatusOS.EM_EXECUCAO
                        || os.getStatus() == StatusOS.AGUARDANDO_VALIDACAO
                        || os.getStatus() == StatusOS.AGUARDANDO_DEVOLUCAO
                        || os.getStatus() == StatusOS.AGUARDANDO_AUDITORIA
                        || os.getStatus() == StatusOS.AGUARDANDO_ENCERRAMENTO)
                .count();
        long ordensConcluidas = ordensFiltradas.stream()
                .filter(this::ordemConcluida)
                .count();

        long obrasEmVistoria = comarcasFiltradas.stream()
                .filter(c -> !obraConcluida(c))
                .filter(c -> c.getEtapaAtual() == null || c.getEtapaAtual() <= 1)
                .count();
        long obrasEmInfraestrutura = comarcasFiltradas.stream()
                .filter(c -> !obraConcluida(c))
                .filter(c -> Integer.valueOf(2).equals(c.getEtapaAtual()))
                .count();
        long obrasEmViradaRede = comarcasFiltradas.stream()
                .filter(c -> !obraConcluida(c))
                .filter(c -> c.getEtapaAtual() != null && c.getEtapaAtual() >= 3)
                .count();

        // ️ 7. Custos de Viagem Acumulados (Lambda segura adicionada )
        BigDecimal custosAcumuladosViagem = todasPrestacoes.stream()
                .filter(pc -> pc.getViagem() != null && pc.getViagem().getProjeto() != null
                        && pc.getViagem().getProjeto().getContrato() != null)
                .filter(pc -> contratosSelecionados.contains(
                        pc.getViagem().getProjeto().getContrato().getId()))
                .filter(pc -> "APROVADO".equalsIgnoreCase(String.valueOf(pc.getStatus())))
                .map(pc -> pc.getCustoReal() != null ? pc.getCustoReal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

        List<MovimentacaoEstoque> movimentacoesFiltradas = todasMovimentacoes.stream()
                .filter(movimentacao -> pertenceAContratoSelecionado(
                        movimentacao, contratosSelecionados))
                .filter(movimentacao -> movimentacao.getDataMovimentacao() == null
                        || dentroDoPeriodo(movimentacao.getDataMovimentacao().toLocalDate(), inicio, fim))
                .filter(movimentacao -> TipoMovimentacao.RETIRADA_OR.equals(movimentacao.getTipo())
                        || TipoMovimentacao.DEVOLUCAO_OR.equals(movimentacao.getTipo()))
                .toList();
        BigDecimal totalRetirado = somarMovimentacoes(
                movimentacoesFiltradas, TipoMovimentacao.RETIRADA_OR);
        BigDecimal totalDevolvido = somarMovimentacoes(
                movimentacoesFiltradas, TipoMovimentacao.DEVOLUCAO_OR);
        BigDecimal custosMateriaisConsumidos = totalRetirado.subtract(totalDevolvido);
        boolean custosMateriaisEstimados = movimentacoesFiltradas.stream()
                .anyMatch(movimentacao -> Boolean.TRUE.equals(movimentacao.getCustoEstimado()));
        BigDecimal resultadoOperacional = valorReceitaRegistrada
                .subtract(custosMateriaisConsumidos)
                .subtract(custosAcumuladosViagem);
        BigDecimal margemOperacional = calcularMargem(
                resultadoOperacional, valorReceitaRegistrada);

        BigDecimal valorTotalEstoque = todosMateriais.stream()
                .map(Material::getValorTotalEstoque)
                .map(this::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long itensEstoqueCritico = todosMateriais.stream()
                .filter(this::estoqueCritico)
                .count();

        // 8. DTO montado sem warnings
        return new DashboardIndicadoresDTO(
                contratosAtivos,
                valorTotalContratado,
                dinheiro(valorReceitaRegistrada),
                valorFaturado,
                valorPendenteFaturamento,
                (long) faturamentosAtrasados.size(),
                dinheiro(valorFaturamentoEmAtraso),
                dinheiro(custosMateriaisConsumidos),
                custosMateriaisEstimados,
                dinheiro(resultadoOperacional),
                margemOperacional,
                dinheiro(valorTotalEstoque),
                itensEstoqueCritico,
                totalComarcasConcluidas,
                totalComarcasEmAtraso,
                custosAcumuladosViagem,
                (long) ordensFiltradas.size(),
                ordensAbertas,
                ordensEmExecucao,
                ordensConcluidas,
                ordensProximasPrazo,
                obrasEmVistoria,
                obrasEmInfraestrutura,
                obrasEmViradaRede);
    }

    private boolean dentroDoPeriodo(LocalDate data, LocalDate inicio, LocalDate fim) {
        return (inicio == null || data == null || !data.isBefore(inicio))
                && (fim == null || data == null || !data.isAfter(fim));
    }

    private boolean pertenceAContratoSelecionado(
            MovimentacaoEstoque movimentacao, Set<Long> contratosSelecionados) {
        if (movimentacao.getOrdemServico() != null
                && movimentacao.getOrdemServico().getContrato() != null) {
            return contratosSelecionados.contains(
                    movimentacao.getOrdemServico().getContrato().getId());
        }
        return movimentacao.getProjeto() != null
                && movimentacao.getProjeto().getContrato() != null
                && contratosSelecionados.contains(
                        movimentacao.getProjeto().getContrato().getId());
    }

    private BigDecimal somarMovimentacoes(
            List<MovimentacaoEstoque> movimentacoes, TipoMovimentacao tipo) {
        return movimentacoes.stream()
                .filter(movimentacao -> tipo.equals(movimentacao.getTipo()))
                .map(MovimentacaoEstoque::getValorTotalMovimentacao)
                .map(this::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean estoqueCritico(Material material) {
        BigDecimal minimo = valor(material.getEstoqueMinimo());
        BigDecimal disponivel = material.getTipoControle() == TipoControleEstoque.FRACIONADO
                || material.getTipoControle() == TipoControleEstoque.METRAGEM
                || material.getTipoControle() == TipoControleEstoque.BOBINA
                || material.getTipoControle() == TipoControleEstoque.ROLO
                        ? valor(material.getMetragemDisponivel())
                                .subtract(valor(material.getMetragemReservada()))
                        : BigDecimal.valueOf(material.getQuantidadeDisponivel() != null
                                ? material.getQuantidadeDisponivel()
                                : 0)
                                .subtract(BigDecimal.valueOf(material.getQuantidadeReservada() != null
                                        ? material.getQuantidadeReservada()
                                        : 0));
        return disponivel.compareTo(minimo) <= 0;
    }

    private BigDecimal calcularMargem(BigDecimal resultado, BigDecimal receita) {
        if (receita.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return resultado.divide(receita, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal valor(BigDecimal numero) {
        return numero != null ? numero : BigDecimal.ZERO;
    }

    private BigDecimal dinheiro(BigDecimal numero) {
        return valor(numero).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean contratoDentroDoPeriodo(Contrato contrato, LocalDate inicio, LocalDate fim) {
        if (inicio != null && contrato.getVigenciaFim() != null
                && contrato.getVigenciaFim().isBefore(inicio)) {
            return false;
        }
        return fim == null || contrato.getVigenciaInicio() == null
                || !contrato.getVigenciaInicio().isAfter(fim);
    }

    private boolean pertenceAContratoSelecionado(Comarca comarca, Set<Long> contratosSelecionados) {
        if (comarca.getOrdemServico() != null && comarca.getOrdemServico().getContrato() != null) {
            return contratosSelecionados.contains(comarca.getOrdemServico().getContrato().getId());
        }
        return comarca.getProjeto() != null && comarca.getProjeto().getContrato() != null
                && contratosSelecionados.contains(comarca.getProjeto().getContrato().getId());
    }

    private boolean ordemConcluida(OrdemServico ordem) {
        return ordem.getStatus() == StatusOS.CONCLUIDA || ordem.getStatus() == StatusOS.FATURADA;
    }

    private boolean obraConcluida(Comarca comarca) {
        return Boolean.TRUE.equals(comarca.getViradaRedeConcluida())
                || "CONCLUIDA".equalsIgnoreCase(String.valueOf(comarca.getSituacao()))
                || "OBRA_CONCLUIDA".equalsIgnoreCase(String.valueOf(comarca.getSituacao()))
                || (comarca.getPercentualConcluido() != null
                        && comarca.getPercentualConcluido().intValue() >= 100);
    }
}
