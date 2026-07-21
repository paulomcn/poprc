package com.poprc.demo.service;

import com.poprc.demo.dto.DashboardIndicadoresDTO;
import com.poprc.demo.model.*;
import com.poprc.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    @Transactional(readOnly = true)
    public DashboardIndicadoresDTO calcularIndicadores(String filtroContrato, LocalDate inicio, LocalDate fim) {

        // 1. Carrega as coleções da base de dados
        List<Contrato> todosContratos = contratoRepository.findAll();
        List<Faturamento> todosFaturamentos = faturamentoRepository.findAll();
        List<Comarca> todasComarcas = comarcaRepository.findAll();
        List<PrestacaoContas> todasPrestacoes = prestacaoContasRepository.findAll();
        List<OrdemServico> todasOrdens = ordemServicoRepository.findAll();

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

        // 4. Faturamento Dinâmico realizado (Lambda segura adicionada )
        BigDecimal valorFaturado = todosFaturamentos.stream()
                .filter(f -> f.getContrato() != null)
                .filter(f -> contratosSelecionados.contains(f.getContrato().getId()))
                .filter(f -> inicio == null || f.getDataVencimento() == null || !f.getDataVencimento().isBefore(inicio))
                .filter(f -> fim == null || f.getDataVencimento() == null || !f.getDataVencimento().isAfter(fim))
                .filter(f -> "PAGO".equalsIgnoreCase(String.valueOf(f.getSituacao()))
                        || "LIQUIDADO".equalsIgnoreCase(String.valueOf(f.getSituacao())))
                .map(f -> f.getValorMedicao() != null ? f.getValorMedicao() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

        // 5. Faturamento Dinâmico pendente (Lambda segura adicionada )
        BigDecimal valorPendenteFaturamento = todosFaturamentos.stream()
                .filter(f -> f.getContrato() != null)
                .filter(f -> contratosSelecionados.contains(f.getContrato().getId()))
                .filter(f -> inicio == null || f.getDataVencimento() == null || !f.getDataVencimento().isBefore(inicio))
                .filter(f -> fim == null || f.getDataVencimento() == null || !f.getDataVencimento().isAfter(fim))
                .filter(f -> "PENDENTE".equalsIgnoreCase(String.valueOf(f.getSituacao()))
                        || "A_RECEBER".equalsIgnoreCase(String.valueOf(f.getSituacao()))
                        || "A_FATURAR".equalsIgnoreCase(String.valueOf(f.getSituacao()))
                        || "FATURADO".equalsIgnoreCase(String.valueOf(f.getSituacao()))
                        || "EM_ATRASO".equalsIgnoreCase(String.valueOf(f.getSituacao())))
                .map(f -> f.getValorMedicao() != null ? f.getValorMedicao() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

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

        // 8. DTO montado sem warnings
        return new DashboardIndicadoresDTO(
                contratosAtivos,
                valorTotalContratado,
                valorFaturado,
                valorPendenteFaturamento,
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
