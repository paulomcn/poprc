package com.poprc.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.poprc.demo.dto.ReconciliacaoRetiradasPlanilhaRequest;
import com.poprc.demo.dto.EdicaoRetiradaHistoricaRequest;
import com.poprc.demo.model.ImportacaoRetiradaPlanilha;
import com.poprc.demo.model.Material;
import com.poprc.demo.model.MaterialItem;
import com.poprc.demo.model.OrdemRetirada;
import com.poprc.demo.model.OrdemRetiradaItem;
import com.poprc.demo.model.ReconciliacaoRetiradaPlanilha;
import com.poprc.demo.repository.ImportacaoRetiradaPlanilhaRepository;
import com.poprc.demo.repository.MaterialItemRepository;
import com.poprc.demo.repository.OrdemRetiradaItemRepository;
import com.poprc.demo.repository.ReconciliacaoRetiradaPlanilhaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ReconciliacaoRetiradaPlanilhaServiceTest {

    private ImportacaoRetiradaPlanilhaRepository retiradaRepository;
    private ReconciliacaoRetiradaPlanilhaRepository reconciliacaoRepository;
    private OrdemRetiradaItemRepository ordemItemRepository;
    private MaterialItemRepository materialItemRepository;
    private ReconciliacaoRetiradaPlanilhaService service;

    @BeforeEach
    void setUp() {
        retiradaRepository = mock(ImportacaoRetiradaPlanilhaRepository.class);
        reconciliacaoRepository = mock(ReconciliacaoRetiradaPlanilhaRepository.class);
        ordemItemRepository = mock(OrdemRetiradaItemRepository.class);
        materialItemRepository = mock(MaterialItemRepository.class);
        service = new ReconciliacaoRetiradaPlanilhaService(
                retiradaRepository,
                reconciliacaoRepository,
                ordemItemRepository,
                materialItemRepository);
    }

    @Test
    void previaNaoDeveAlterarHistoricoNemEstoqueOperacional() {
        ImportacaoRetiradaPlanilha retirada = retiradaHistorica();
        when(retiradaRepository.findById(8L)).thenReturn(Optional.of(retirada));

        var resultado = service.reconciliar(request(false), "Paulo");

        assertEquals(1, resultado.divergencias());
        assertEquals(new BigDecimal("19.000"), resultado.itens().getFirst().quantidadeAnterior());
        assertEquals(new BigDecimal("20.000"), resultado.itens().getFirst().quantidadeNova());
        assertEquals(new BigDecimal("19.000"), retirada.getQuantidadeRetirada());
        verify(reconciliacaoRepository, never()).save(any());
        verify(retiradaRepository, never()).save(any());
    }

    @Test
    void confirmacaoDeveAtualizarOrEObraSemMovimentarSaldoDoEstoque() {
        ImportacaoRetiradaPlanilha retirada = retiradaHistorica();
        MaterialItem itemObra = new MaterialItem();
        itemObra.setQuantidadePrevista(new BigDecimal("100.000"));
        itemObra.setQuantidadeAuditada(new BigDecimal("100.000"));
        OrdemRetiradaItem itemOr = new OrdemRetiradaItem();
        itemOr.setMaterialItem(itemObra);
        itemOr.setQuantidadeSolicitada(new BigDecimal("19.000"));
        itemOr.setQuantidadeRetirada(new BigDecimal("19.000"));

        when(retiradaRepository.findById(8L)).thenReturn(Optional.of(retirada));
        when(reconciliacaoRepository.existsByRetiradaImportadaIdAndHashOrigem(8L, "a".repeat(64)))
                .thenReturn(false);
        when(ordemItemRepository.findByOrdemRetiradaIdAndMaterialId(4L, 7L))
                .thenReturn(Optional.of(itemOr));

        var resultado = service.reconciliar(request(true), "Paulo");

        assertEquals(1, resultado.divergencias());
        assertEquals(new BigDecimal("20.000"), retirada.getQuantidadeRetirada());
        assertEquals(new BigDecimal("113.000"), retirada.getSaldoFinal());
        assertEquals(new BigDecimal("20.000"), itemOr.getQuantidadeSolicitada());
        assertEquals(new BigDecimal("20.000"), itemOr.getQuantidadeRetirada());
        assertEquals(new BigDecimal("101.000"), itemObra.getQuantidadePrevista());
        assertEquals(new BigDecimal("101.000"), itemObra.getQuantidadeAuditada());

        ArgumentCaptor<ReconciliacaoRetiradaPlanilha> evento =
                ArgumentCaptor.forClass(ReconciliacaoRetiradaPlanilha.class);
        verify(reconciliacaoRepository).save(evento.capture());
        assertEquals(new BigDecimal("19.000"), evento.getValue().getQuantidadeAnterior());
        assertEquals(new BigDecimal("20.000"), evento.getValue().getQuantidadeNova());
        assertEquals("Paulo", evento.getValue().getReconciliadoPor());
        verify(ordemItemRepository).save(itemOr);
        verify(materialItemRepository).save(itemObra);
        verify(retiradaRepository).save(retirada);
    }

    @Test
    void edicaoManualDeveRecalcularFaltaERegistrarJustificativa() {
        ImportacaoRetiradaPlanilha retirada = retiradaHistorica();
        MaterialItem itemObra = new MaterialItem();
        itemObra.setQuantidadePrevista(new BigDecimal("100.000"));
        itemObra.setQuantidadeAuditada(new BigDecimal("100.000"));
        OrdemRetiradaItem itemOr = new OrdemRetiradaItem();
        itemOr.setMaterialItem(itemObra);
        itemOr.setQuantidadeSolicitada(new BigDecimal("19.000"));
        itemOr.setQuantidadeRetirada(new BigDecimal("19.000"));

        when(retiradaRepository.findById(8L)).thenReturn(Optional.of(retirada));
        when(ordemItemRepository.findByOrdemRetiradaIdAndMaterialId(4L, 7L))
                .thenReturn(Optional.of(itemOr));

        var resultado = service.editarHistorico(
                8L,
                new EdicaoRetiradaHistoricaRequest(
                        new BigDecimal("140"),
                        LocalDate.of(2026, 8, 25),
                        "Conferido com a OR física"),
                "Paulo");

        assertEquals("EDICAO_MANUAL", resultado.origem());
        assertEquals("Conferido com a OR física", resultado.motivo());
        assertEquals(new BigDecimal("140.000"), retirada.getQuantidadeRetirada());
        assertEquals(new BigDecimal("-7.000"), retirada.getSaldoFinal());
        assertEquals(new BigDecimal("7.000"), retirada.getQuantidadeFaltante());
        assertEquals(new BigDecimal("133.000"), itemOr.getQuantidadeRetirada());

        ArgumentCaptor<ReconciliacaoRetiradaPlanilha> evento =
                ArgumentCaptor.forClass(ReconciliacaoRetiradaPlanilha.class);
        verify(reconciliacaoRepository).save(evento.capture());
        assertEquals("EDICAO_MANUAL", evento.getValue().getOrigem());
        assertEquals("Conferido com a OR física", evento.getValue().getMotivo());
    }

    private ReconciliacaoRetiradasPlanilhaRequest request(boolean confirmar) {
        return new ReconciliacaoRetiradasPlanilhaRequest(
                "CONTROLE_ESTOQUE.xlsx",
                "a".repeat(64),
                confirmar,
                List.of(new ReconciliacaoRetiradasPlanilhaRequest.Item(
                        8L,
                        new BigDecimal("133"),
                        new BigDecimal("20"),
                        new BigDecimal("113"),
                        LocalDate.of(2026, 8, 24))));
    }

    private ImportacaoRetiradaPlanilha retiradaHistorica() {
        Material material = new Material();
        material.setId(7L);
        material.setNome("Terminal");
        OrdemRetirada ordem = new OrdemRetirada();
        ordem.setId(4L);
        ImportacaoRetiradaPlanilha retirada = new ImportacaoRetiradaPlanilha();
        retirada.setId(8L);
        retirada.setMaterial(material);
        retirada.setOrdemRetirada(ordem);
        retirada.setAbaOrigem("ORDEM DE RETIRADA - CAAPORÃ2");
        retirada.setSaldoInicial(new BigDecimal("133.000"));
        retirada.setQuantidadeRetirada(new BigDecimal("19.000"));
        retirada.setSaldoFinal(new BigDecimal("114.000"));
        retirada.setQuantidadeFaltante(BigDecimal.ZERO.setScale(3));
        retirada.setCustoUnitario(new BigDecimal("7.8300"));
        retirada.setDataRetirada(LocalDate.of(2026, 8, 24));
        return retirada;
    }
}
