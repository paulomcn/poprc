package com.poprc.demo.service;

import com.poprc.demo.dto.RelatorioLucratividadeDTO;
import com.poprc.demo.model.Faturamento;
import com.poprc.demo.model.MaterialProjeto;
import com.poprc.demo.model.PrestacaoContas;
import com.poprc.demo.model.Projeto;
import com.poprc.demo.repository.FaturamentoRepository;
import com.poprc.demo.repository.MaterialProjetoRepository;
import com.poprc.demo.repository.PrestacaoContasRepository;
import com.poprc.demo.repository.ProjetoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RelatorioFinanceiroService {

    private final ProjetoRepository projetoRepository;
    private final FaturamentoRepository faturamentoRepository;
    private final PrestacaoContasRepository prestacaoContasRepository;
    private final MaterialProjetoRepository materialProjetoRepository;

    @Transactional(readOnly = true)
    public RelatorioLucratividadeDTO gerarRelatorioLucratividade(Long projetoId) {
        Projeto projeto = projetoRepository.findById(projetoId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));

        // 1. FATURAMENTO (Entradas)
        // Como o Faturamento é ligado ao Contrato, pegamos o contrato do projeto
        BigDecimal totalFaturado = BigDecimal.ZERO;
        if (projeto.getContrato() != null) {
            List<Faturamento> faturamentos = faturamentoRepository.findByContratoId(projeto.getContrato().getId());
            totalFaturado = faturamentos.stream()
                    .map(Faturamento::getValorMedicao)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // 2. CUSTO DAS VIAGENS (Prestação de contas)
        List<PrestacaoContas> prestacoes = prestacaoContasRepository.findByViagemProjetoId(projetoId);
        BigDecimal totalCustoViagens = prestacoes.stream()
                .map(PrestacaoContas::getCustoReal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. CUSTO DOS MATERIAIS (Mockado a R$ 50,00 por unidade utilizada)
        List<MaterialProjeto> materiais = materialProjetoRepository.findByProjetoId(projetoId);
        BigDecimal precoPadraoMaterial = new BigDecimal("50.00");
        BigDecimal totalCustoMateriais = materiais.stream()
                .map(m -> precoPadraoMaterial.multiply(BigDecimal.valueOf(m.getQuantidadeUtilizada())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. CONSOLIDAÇÃO E LUCRO BRUTO
        BigDecimal custoTotalAcumulado = totalCustoViagens.add(totalCustoMateriais);
        BigDecimal lucroBruto = totalFaturado.subtract(custoTotalAcumulado);

        // 5. CÁLCULO DA MARGEM E SAÚDE FINANCEIRA
        BigDecimal margemLucro = BigDecimal.ZERO;
        String saudeFinanceira = "PREJUIZO_CRITICO";

        if (totalFaturado.compareTo(BigDecimal.ZERO) > 0) {
            // Regra de três: (Lucro / Faturamento) * 100
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
                // Como não temos coluna nome, vai o ID mesmo pra não quebrar
                .nomeProjeto("Projeto Técnico " + projetoId) 
                .totalFaturado(totalFaturado)
                .totalCustoViagens(totalCustoViagens)
                .totalCustoMateriais(totalCustoMateriais)
                .custoTotalAcumulado(custoTotalAcumulado)
                .lucroBruto(lucroBruto)
                .margemLucro(margemLucro)
                .saudeFinanceira(saudeFinanceira)
                .build();
    }
}