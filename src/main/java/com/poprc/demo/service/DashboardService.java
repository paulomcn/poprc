package com.poprc.demo.service;

import com.poprc.demo.dto.DashboardIndicadoresDTO;
import com.poprc.demo.model.*;
import com.poprc.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ContratoRepository contratoRepository;
    private final FaturamentoRepository faturamentoRepository;
    private final ComarcaRepository comarcaRepository;
    private final PrestacaoContasRepository prestacaoContasRepository;

    public DashboardIndicadoresDTO calcularIndicadores(String filtroContrato, LocalDate inicio, LocalDate fim) {

        // 1. Carrega as coleções da base de dados
        List<Contrato> todosContratos = contratoRepository.findAll();
        List<Faturamento> todosFaturamentos = faturamentoRepository.findAll();
        List<Comarca> todasComarcas = comarcaRepository.findAll();
        List<PrestacaoContas> todasPrestacoes = prestacaoContasRepository.findAll();

        // ️ 2. Filtro e contagem de Contratos Ativos
        long contratosAtivos = todosContratos.stream()
                .filter(c -> "ATIVO".equalsIgnoreCase(String.valueOf(c.getStatus())))
                .filter(c -> filtroContrato == null || filtroContrato.trim().isEmpty()
                        || c.getContrato().equalsIgnoreCase(filtroContrato))
                .filter(c -> {
                    if (inicio != null && c.getVigenciaFim() != null && c.getVigenciaFim().isBefore(inicio))
                        return false;
                    if (fim != null && c.getVigenciaInicio() != null && c.getVigenciaInicio().isAfter(fim))
                        return false;
                    return true;
                })
                .count();

        // 3. Cálculo do Valor Alocado Global (Lambda segura adicionada )
        BigDecimal valorTotalContratado = todosContratos.stream()
                .filter(c -> filtroContrato == null || filtroContrato.trim().isEmpty()
                        || c.getContrato().equalsIgnoreCase(filtroContrato))
                .filter(c -> {
                    if (inicio != null && c.getVigenciaFim() != null && c.getVigenciaFim().isBefore(inicio))
                        return false;
                    if (fim != null && c.getVigenciaInicio() != null && c.getVigenciaInicio().isAfter(fim))
                        return false;
                    return true;
                })
                .map(c -> c.getValorGlobal() != null ? c.getValorGlobal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

        // 4. Faturamento Dinâmico realizado (Lambda segura adicionada )
        BigDecimal valorFaturado = todosFaturamentos.stream()
                .filter(f -> f.getContrato() != null)
                .filter(f -> filtroContrato == null || filtroContrato.trim().isEmpty()
                        || f.getContrato().getContrato().equalsIgnoreCase(filtroContrato))
                .filter(f -> inicio == null || f.getDataVencimento() == null || !f.getDataVencimento().isBefore(inicio))
                .filter(f -> fim == null || f.getDataVencimento() == null || !f.getDataVencimento().isAfter(fim))
                .filter(f -> "PAGO".equalsIgnoreCase(String.valueOf(f.getSituacao()))
                        || "LIQUIDADO".equalsIgnoreCase(String.valueOf(f.getSituacao())))
                .map(f -> f.getValorMedicao() != null ? f.getValorMedicao() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

        // 5. Faturamento Dinâmico pendente (Lambda segura adicionada )
        BigDecimal valorPendenteFaturamento = todosFaturamentos.stream()
                .filter(f -> f.getContrato() != null)
                .filter(f -> filtroContrato == null || filtroContrato.trim().isEmpty()
                        || f.getContrato().getContrato().equalsIgnoreCase(filtroContrato))
                .filter(f -> inicio == null || f.getDataVencimento() == null || !f.getDataVencimento().isBefore(inicio))
                .filter(f -> fim == null || f.getDataVencimento() == null || !f.getDataVencimento().isAfter(fim))
                .filter(f -> "PENDENTE".equalsIgnoreCase(String.valueOf(f.getSituacao()))
                        || "A_RECEBER".equalsIgnoreCase(String.valueOf(f.getSituacao())))
                .map(f -> f.getValorMedicao() != null ? f.getValorMedicao() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

        // ️ 6. Status de Comarcas
        long totalComarcasConcluidas = todasComarcas.stream()
                .filter(c -> "CONCLUIDA".equalsIgnoreCase(String.valueOf(c.getSituacao()))
                        || (c.getPercentualConcluido() != null && c.getPercentualConcluido().intValue() == 100))
                .count();

        long totalComarcasEmAtraso = todasComarcas.stream()
                .filter(c -> "EM_ATRASO".equalsIgnoreCase(String.valueOf(c.getSituacao()))
                        || "ATRASADA".equalsIgnoreCase(String.valueOf(c.getSituacao())))
                .count();

        // ️ 7. Custos de Viagem Acumulados (Lambda segura adicionada )
        BigDecimal custosAcumuladosViagem = todasPrestacoes.stream()
                .filter(pc -> pc.getViagem() != null && pc.getViagem().getProjeto() != null
                        && pc.getViagem().getProjeto().getContrato() != null)
                .filter(pc -> filtroContrato == null || filtroContrato.trim().isEmpty()
                        || pc.getViagem().getProjeto().getContrato().getContrato().equalsIgnoreCase(filtroContrato))
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
                custosAcumuladosViagem);
    }
}