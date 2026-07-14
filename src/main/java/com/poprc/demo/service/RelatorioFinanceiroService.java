package com.poprc.demo.service;

import com.poprc.demo.dto.RelatorioLucratividadeDTO;
import com.poprc.demo.model.Faturamento;
import com.poprc.demo.model.PrestacaoContas;
import com.poprc.demo.model.Projeto;
import com.poprc.demo.repository.FaturamentoRepository;
import com.poprc.demo.repository.PrestacaoContasRepository;
import com.poprc.demo.repository.ProjetoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RelatorioFinanceiroService {

    private final ProjetoRepository projetoRepository;
    private final FaturamentoRepository faturamentoRepository;
    private final PrestacaoContasRepository prestacaoContasRepository;

    @Transactional(readOnly = true)
    public RelatorioLucratividadeDTO gerarRelatorioLucratividade(Long projetoId) {
        Projeto projeto = projetoRepository.findById(projetoId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));

        // 1. FATURAMENTO (Entradas)
        BigDecimal totalFaturado = BigDecimal.ZERO;
        if (projeto.getContrato() != null) {
            List<Faturamento> faturamentos = faturamentoRepository.findByContratoId(projeto.getContrato().getId());
            totalFaturado = faturamentos.stream()
                    .filter(Objects::nonNull)
                    .map(f -> f.getValorMedicao())
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        }

        // 2. CUSTO DAS VIAGENS (Prestação de contas)
        List<PrestacaoContas> prestacoes = prestacaoContasRepository.findByViagemProjetoId(projetoId);
        BigDecimal totalCustoViagens = prestacoes.stream()
                .filter(Objects::nonNull)
                .map(p -> p.getCustoReal())
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

        // Custos de materiais ficam fora do cálculo até existir um valor real cadastrado.
        BigDecimal totalCustoMateriais = null;

        // 3. CONSOLIDAÇÃO PARCIAL COM APENAS CUSTOS REAIS DISPONÍVEIS
        BigDecimal custoTotalAcumulado = totalCustoViagens;
        BigDecimal lucroBruto = totalFaturado.subtract(custoTotalAcumulado);

        // 4. CÁLCULO DA MARGEM E SAÚDE FINANCEIRA PARCIAIS
        BigDecimal margemLucro = BigDecimal.ZERO;
        String saudeFinanceira = "PREJUIZO_CRITICO";

        if (totalFaturado.compareTo(BigDecimal.ZERO) > 0) {
            margemLucro = lucroBruto.divide(totalFaturado, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);

            if (margemLucro.compareTo(new BigDecimal("20.00")) > 0) {
                saudeFinanceira = "LUCRO_SAUDAVEL";
            } else if (margemLucro.compareTo(BigDecimal.ZERO) >= 0) {
                saudeFinanceira = "ALERTA_MARGEM_BAIXA";
            }
        } else if (custoTotalAcumulado.compareTo(BigDecimal.ZERO) == 0) {
            saudeFinanceira = "SEM_MOVIMENTACAO";
        }

        return RelatorioLucratividadeDTO.builder()
                .projetoId(projetoId)
                .nomeProjeto("Projeto Técnico " + projetoId)
                .totalFaturado(totalFaturado)
                .totalCustoViagens(totalCustoViagens)
                .totalCustoMateriais(totalCustoMateriais)
                .custoMateriaisDisponivel(false)
                .resultadoFinanceiroParcial(true)
                .custoTotalAcumulado(custoTotalAcumulado)
                .lucroBruto(lucroBruto)
                .margemLucro(margemLucro)
                .saudeFinanceira(saudeFinanceira)
                .build();
    }
}
