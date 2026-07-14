package com.poprc.demo.service;

import com.poprc.demo.dto.RelatorioLucratividadeDTO;
import com.poprc.demo.model.PrestacaoContas;
import com.poprc.demo.model.Projeto;
import com.poprc.demo.repository.FaturamentoRepository;
import com.poprc.demo.repository.PrestacaoContasRepository;
import com.poprc.demo.repository.ProjetoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RelatorioFinanceiroServiceTest {

    private ProjetoRepository projetoRepository;
    private PrestacaoContasRepository prestacaoContasRepository;
    private RelatorioFinanceiroService service;

    @BeforeEach
    void setUp() {
        projetoRepository = mock(ProjetoRepository.class);
        FaturamentoRepository faturamentoRepository = mock(FaturamentoRepository.class);
        prestacaoContasRepository = mock(PrestacaoContasRepository.class);
        service = new RelatorioFinanceiroService(
                projetoRepository,
                faturamentoRepository,
                prestacaoContasRepository);
    }

    @Test
    void deveExcluirCustoFicticioDeMateriaisDosTotais() {
        Projeto projeto = new Projeto();
        projeto.setId(10L);
        PrestacaoContas prestacao = new PrestacaoContas();
        prestacao.setCustoReal(new BigDecimal("125.50"));

        when(projetoRepository.findById(10L)).thenReturn(Optional.of(projeto));
        when(prestacaoContasRepository.findByViagemProjetoId(10L)).thenReturn(List.of(prestacao));

        RelatorioLucratividadeDTO relatorio = service.gerarRelatorioLucratividade(10L);

        assertNull(relatorio.getTotalCustoMateriais());
        assertFalse(relatorio.getCustoMateriaisDisponivel());
        assertTrue(relatorio.getResultadoFinanceiroParcial());
        assertEquals(new BigDecimal("125.50"), relatorio.getTotalCustoViagens());
        assertEquals(new BigDecimal("125.50"), relatorio.getCustoTotalAcumulado());
        assertEquals(new BigDecimal("-125.50"), relatorio.getLucroBruto());
    }
}
