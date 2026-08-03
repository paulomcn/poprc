package com.poprc.demo.service;

import com.poprc.demo.dto.RelatorioLucratividadeDTO;
import com.poprc.demo.model.Contrato;
import com.poprc.demo.model.Faturamento;
import com.poprc.demo.model.MovimentacaoEstoque;
import com.poprc.demo.model.OrdemServico;
import com.poprc.demo.model.PrestacaoContas;
import com.poprc.demo.model.Projeto;
import com.poprc.demo.model.StatusOS;
import com.poprc.demo.model.TipoMovimentacao;
import com.poprc.demo.repository.ContratoRepository;
import com.poprc.demo.repository.FaturamentoRepository;
import com.poprc.demo.repository.MovimentacaoEstoqueRepository;
import com.poprc.demo.repository.OrdemServicoRepository;
import com.poprc.demo.repository.PrestacaoContasRepository;
import com.poprc.demo.repository.ProjetoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RelatorioFinanceiroServiceTest {

    private ProjetoRepository projetoRepository;
    private ContratoRepository contratoRepository;
    private FaturamentoRepository faturamentoRepository;
    private PrestacaoContasRepository prestacaoContasRepository;
    private MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    private OrdemServicoRepository ordemServicoRepository;
    private RelatorioFinanceiroService service;

    @BeforeEach
    void setUp() {
        projetoRepository = mock(ProjetoRepository.class);
        contratoRepository = mock(ContratoRepository.class);
        faturamentoRepository = mock(FaturamentoRepository.class);
        prestacaoContasRepository = mock(PrestacaoContasRepository.class);
        movimentacaoEstoqueRepository = mock(MovimentacaoEstoqueRepository.class);
        ordemServicoRepository = mock(OrdemServicoRepository.class);
        service = new RelatorioFinanceiroService(
                projetoRepository,
                contratoRepository,
                faturamentoRepository,
                prestacaoContasRepository,
                movimentacaoEstoqueRepository,
                ordemServicoRepository);
    }

    @Test
    void deveCalcularConsumoLiquidoComCustoHistoricoDaMovimentacao() {
        Projeto projeto = new Projeto();
        projeto.setId(10L);
        projeto.setContrato(new Contrato());
        PrestacaoContas prestacao = new PrestacaoContas();
        prestacao.setCustoReal(new BigDecimal("125.50"));
        Faturamento faturamento = new Faturamento();
        faturamento.setValorMedicao(new BigDecimal("1000.00"));
        MovimentacaoEstoque retirada = movimentacao(TipoMovimentacao.RETIRADA_OR, "200.0000", false);
        MovimentacaoEstoque devolucao = movimentacao(TipoMovimentacao.DEVOLUCAO_OR, "50.0000", false);

        when(projetoRepository.findById(10L)).thenReturn(Optional.of(projeto));
        when(faturamentoRepository.findByProjetoId(10L)).thenReturn(List.of(faturamento));
        when(prestacaoContasRepository.findByViagemProjetoId(10L)).thenReturn(List.of(prestacao));
        when(movimentacaoEstoqueRepository.findByProjetoIdOrderByDataMovimentacaoDesc(10L))
                .thenReturn(List.of(devolucao, retirada));

        RelatorioLucratividadeDTO relatorio = service.gerarRelatorioLucratividade(10L);

        assertEquals(new BigDecimal("150.00"), relatorio.getTotalCustoMateriais());
        assertTrue(relatorio.getCustoMateriaisDisponivel());
        assertEquals(false, relatorio.getCustoMateriaisEstimado());
        assertEquals(false, relatorio.getResultadoFinanceiroParcial());
        assertEquals(new BigDecimal("125.50"), relatorio.getTotalCustoViagens());
        assertEquals(new BigDecimal("275.50"), relatorio.getCustoTotalAcumulado());
        assertEquals(new BigDecimal("724.50"), relatorio.getLucroBruto());
        assertEquals(new BigDecimal("72.45"), relatorio.getMargemLucro());
    }

    @Test
    void deveSinalizarCustoLegadoEstimadoOuMaterialSemValor() {
        Projeto projeto = new Projeto();
        projeto.setId(11L);
        projeto.setContrato(new Contrato());
        MovimentacaoEstoque retiradaEstimada = movimentacao(TipoMovimentacao.RETIRADA_OR, "80.0000", true);
        MovimentacaoEstoque retiradaSemCusto = movimentacao(TipoMovimentacao.RETIRADA_OR, "0.0000", false);

        when(projetoRepository.findById(11L)).thenReturn(Optional.of(projeto));
        when(prestacaoContasRepository.findByViagemProjetoId(11L)).thenReturn(List.of());
        when(movimentacaoEstoqueRepository.findByProjetoIdOrderByDataMovimentacaoDesc(11L))
                .thenReturn(List.of(retiradaEstimada, retiradaSemCusto));

        RelatorioLucratividadeDTO relatorio = service.gerarRelatorioLucratividade(11L);

        assertEquals(new BigDecimal("80.00"), relatorio.getTotalCustoMateriais());
        assertEquals(false, relatorio.getCustoMateriaisDisponivel());
        assertTrue(relatorio.getCustoMateriaisEstimado());
        assertTrue(relatorio.getResultadoFinanceiroParcial());
    }

    @Test
    void deveDetalharOsSemRatearCustoDeViagemDoProjeto() {
        Contrato contrato = new Contrato();
        contrato.setId(1L);
        contrato.setContrato("Contrato 01");
        Projeto projeto = new Projeto();
        projeto.setId(10L);
        projeto.setContrato(contrato);
        OrdemServico ordem = new OrdemServico();
        ordem.setId(20L);
        ordem.setNumeroOs("Contrato 01 - OS 01");
        ordem.setContrato(contrato);
        ordem.setProjeto(projeto);
        ordem.setStatus(StatusOS.EM_EXECUCAO);
        Faturamento faturamento = new Faturamento();
        faturamento.setValorMedicao(new BigDecimal("1000.00"));
        faturamento.setOrdemServico(ordem);
        MovimentacaoEstoque retirada = movimentacao(TipoMovimentacao.RETIRADA_OR, "100.0000", false);
        retirada.setOrdemServico(ordem);
        PrestacaoContas prestacao = new PrestacaoContas();
        prestacao.setCustoReal(new BigDecimal("30.00"));

        when(ordemServicoRepository.findById(20L)).thenReturn(Optional.of(ordem));
        when(faturamentoRepository.findByProjetoId(10L)).thenReturn(List.of(faturamento));
        when(movimentacaoEstoqueRepository.findByProjetoIdOrderByDataMovimentacaoDesc(10L))
                .thenReturn(List.of(retirada));
        when(prestacaoContasRepository.findByViagemProjetoId(10L)).thenReturn(List.of(prestacao));

        RelatorioLucratividadeDTO relatorio = service.gerarRelatorioLucratividade(1L, 10L, 20L);

        assertEquals(new BigDecimal("0.00"), relatorio.getTotalCustoViagens());
        assertEquals(new BigDecimal("30.00"), relatorio.getCustoViagensNaoAlocado());
        assertEquals(new BigDecimal("900.00"), relatorio.getLucroBruto());
        assertEquals(1, relatorio.getOrdensServico().size());
        assertEquals(new BigDecimal("900.00"),
                relatorio.getOrdensServico().getFirst().getLucroOperacional());
    }

    private MovimentacaoEstoque movimentacao(TipoMovimentacao tipo, String total, boolean estimado) {
        MovimentacaoEstoque movimentacao = new MovimentacaoEstoque();
        movimentacao.setTipo(tipo);
        movimentacao.setValorTotalMovimentacao(new BigDecimal(total));
        movimentacao.setCustoEstimado(estimado);
        return movimentacao;
    }
}
