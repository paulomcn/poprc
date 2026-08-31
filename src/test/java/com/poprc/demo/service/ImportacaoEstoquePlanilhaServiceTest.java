package com.poprc.demo.service;

import com.poprc.demo.dto.ImportacaoEstoquePlanilhaRequest;
import com.poprc.demo.dto.AtualizacaoCustosPlanilhaRequest;
import com.poprc.demo.dto.ImportacaoEstoquePlanilhaResultadoDTO;
import com.poprc.demo.dto.SincronizacaoSaldosPlanilhaRequest;
import com.poprc.demo.model.ImportacaoEstoquePlanilha;
import com.poprc.demo.model.Comarca;
import com.poprc.demo.model.ImportacaoEstoqueItemPlanilha;
import com.poprc.demo.model.ImportacaoEntradaPlanilha;
import com.poprc.demo.model.ImportacaoRetiradaPlanilha;
import com.poprc.demo.model.LocalEstoque;
import com.poprc.demo.model.Material;
import com.poprc.demo.model.TipoControleEstoque;
import com.poprc.demo.repository.ComarcaRepository;
import com.poprc.demo.repository.ImportacaoEstoqueItemPlanilhaRepository;
import com.poprc.demo.repository.ImportacaoEntradaPlanilhaRepository;
import com.poprc.demo.repository.ImportacaoEstoquePlanilhaRepository;
import com.poprc.demo.repository.ImportacaoRetiradaPlanilhaRepository;
import com.poprc.demo.repository.ImportacaoRetornoPlanilhaRepository;
import com.poprc.demo.repository.LocalEstoqueRepository;
import com.poprc.demo.repository.MaterialRepository;
import com.poprc.demo.repository.ContratoRepository;
import com.poprc.demo.repository.FuncionarioRepository;
import com.poprc.demo.repository.ProjetoRepository;
import com.poprc.demo.repository.OrdemServicoRepository;
import com.poprc.demo.repository.OrdemRetiradaRepository;
import com.poprc.demo.repository.OrdemRetiradaItemRepository;
import com.poprc.demo.repository.MaterialItemRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ImportacaoEstoquePlanilhaServiceTest {

    private ImportacaoEstoquePlanilhaRepository importacaoRepository;
    private ImportacaoEstoqueItemPlanilhaRepository itemImportacaoRepository;
    private ImportacaoEntradaPlanilhaRepository entradaImportacaoRepository;
    private ImportacaoRetiradaPlanilhaRepository retiradaImportacaoRepository;
    private ImportacaoRetornoPlanilhaRepository retornoImportacaoRepository;
    private MaterialRepository materialRepository;
    private LocalEstoqueRepository localRepository;
    private ComarcaRepository comarcaRepository;
    private ContratoRepository contratoRepository;
    private FuncionarioRepository funcionarioRepository;
    private ProjetoRepository projetoRepository;
    private OrdemServicoRepository ordemServicoRepository;
    private OrdemRetiradaRepository ordemRetiradaRepository;
    private OrdemRetiradaItemRepository ordemRetiradaItemRepository;
    private MaterialItemRepository materialItemRepository;
    private EstoqueService estoqueService;
    private ImportacaoEstoquePlanilhaService service;

    @BeforeEach
    void setUp() {
        importacaoRepository = mock(ImportacaoEstoquePlanilhaRepository.class);
        itemImportacaoRepository = mock(ImportacaoEstoqueItemPlanilhaRepository.class);
        entradaImportacaoRepository = mock(ImportacaoEntradaPlanilhaRepository.class);
        retiradaImportacaoRepository = mock(ImportacaoRetiradaPlanilhaRepository.class);
        retornoImportacaoRepository = mock(ImportacaoRetornoPlanilhaRepository.class);
        materialRepository = mock(MaterialRepository.class);
        localRepository = mock(LocalEstoqueRepository.class);
        comarcaRepository = mock(ComarcaRepository.class);
        contratoRepository = mock(ContratoRepository.class);
        funcionarioRepository = mock(FuncionarioRepository.class);
        projetoRepository = mock(ProjetoRepository.class);
        ordemServicoRepository = mock(OrdemServicoRepository.class);
        ordemRetiradaRepository = mock(OrdemRetiradaRepository.class);
        ordemRetiradaItemRepository = mock(OrdemRetiradaItemRepository.class);
        materialItemRepository = mock(MaterialItemRepository.class);
        estoqueService = mock(EstoqueService.class);
        service = new ImportacaoEstoquePlanilhaService(
                importacaoRepository,
                itemImportacaoRepository,
                entradaImportacaoRepository,
                retiradaImportacaoRepository,
                retornoImportacaoRepository,
                materialRepository,
                localRepository,
                comarcaRepository,
                contratoRepository,
                funcionarioRepository,
                projetoRepository,
                ordemServicoRepository,
                ordemRetiradaRepository,
                ordemRetiradaItemRepository,
                materialItemRepository,
                estoqueService);
    }

    @Test
    void deveAtualizarMaterialExistenteERegistrarResumoDaImportacao() {
        String hash = "a".repeat(64);
        LocalEstoque local = new LocalEstoque();
        local.setId(3L);
        local.setNome("Estoque Principal");
        local.setAtivo(true);
        Material material = new Material();
        material.setId(7L);
        material.setNome("Patch Cord");
        material.setTipoControle(TipoControleEstoque.UNIDADE);
        material.setQuantidadeDisponivel(10);

        when(importacaoRepository.findByHashSha256(hash)).thenReturn(Optional.empty());
        when(localRepository.findById(3L)).thenReturn(Optional.of(local));
        when(materialRepository.findAll()).thenReturn(List.of(material));
        when(importacaoRepository.saveAndFlush(any(ImportacaoEstoquePlanilha.class)))
                .thenAnswer(invocacao -> {
                    ImportacaoEstoquePlanilha importacao = invocacao.getArgument(0);
                    importacao.setId(11L);
                    return importacao;
                });
        when(importacaoRepository.save(any(ImportacaoEstoquePlanilha.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        ImportacaoEstoquePlanilhaRequest request = new ImportacaoEstoquePlanilhaRequest(
                "estoque.xlsx",
                hash,
                3L,
                List.of(new ImportacaoEstoquePlanilhaRequest.ItemImportacao(
                        "PATCH CORD",
                        8,
                        new BigDecimal("12.50"))),
                List.of());

        ImportacaoEstoquePlanilhaResultadoDTO resultado = service.importar(request, "gestor");

        assertEquals(11L, resultado.importacaoId());
        assertEquals(1, resultado.itensProcessados());
        assertEquals(0, resultado.materiaisCriados());
        assertEquals(1, resultado.materiaisAtualizados());
        assertEquals(0, resultado.ajustesPositivos());
        assertEquals(1, resultado.ajustesNegativos());
        assertEquals(new BigDecimal("100.00"), resultado.valorTotalImportado());
        verify(estoqueService).reconciliarSaldoPlanilha(
                7L,
                3L,
                8,
                new BigDecimal("12.50"),
                "Inventário importado de estoque.xlsx",
                "gestor");
    }

    @Test
    void deveAtualizarSomenteOCustoEPreservarQuantidade() {
        String hash = "9".repeat(64);
        Material material = new Material();
        material.setId(7L);
        material.setNome("Patch Cord");
        material.setAtivo(true);
        material.setTipoControle(TipoControleEstoque.UNIDADE);
        material.setQuantidadeDisponivel(10);
        material.setQuantidadeReservada(2);
        material.setCustoMedio(new BigDecimal("5.0000"));

        when(importacaoRepository.existsByHashSha256(hash)).thenReturn(false);
        when(importacaoRepository.saveAndFlush(any(ImportacaoEstoquePlanilha.class)))
                .thenAnswer(invocacao -> {
                    ImportacaoEstoquePlanilha importacao = invocacao.getArgument(0);
                    importacao.setId(31L);
                    return importacao;
                });
        when(importacaoRepository.save(any(ImportacaoEstoquePlanilha.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        when(materialRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(material));
        when(materialRepository.save(any(Material.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        when(materialRepository.findByAtivoTrueOrderByNomeAsc()).thenReturn(List.of(material));

        AtualizacaoCustosPlanilhaRequest request = new AtualizacaoCustosPlanilhaRequest(
                "custos.xlsx",
                hash,
                List.of(new AtualizacaoCustosPlanilhaRequest.ItemCusto(
                        7L, "PATCH CORD", new BigDecimal("7.5000"), 2)));

        ImportacaoEstoquePlanilhaResultadoDTO resultado = service.atualizarCustos(request, "gestor");

        assertEquals(10, material.getQuantidadeDisponivel());
        assertEquals(2, material.getQuantidadeReservada());
        assertEquals(new BigDecimal("7.5000"), material.getCustoMedio());
        assertEquals(1, resultado.materiaisAtualizados());
        assertEquals(0, resultado.ajustesPositivos());
        assertEquals(0, resultado.ajustesNegativos());
        assertEquals(new BigDecimal("75.00"), resultado.valorTotalImportado());

        ArgumentCaptor<ImportacaoEstoqueItemPlanilha> itemCaptor =
                ArgumentCaptor.forClass(ImportacaoEstoqueItemPlanilha.class);
        verify(itemImportacaoRepository).save(itemCaptor.capture());
        assertEquals(new BigDecimal("5.0000"), itemCaptor.getValue().getCustoAnterior());
        assertEquals(new BigDecimal("7.5000"), itemCaptor.getValue().getCustoUnitario());
        assertEquals(itemCaptor.getValue().getSaldoAnterior(), itemCaptor.getValue().getSaldoImportado());
        assertEquals("CUSTO_ATUALIZADO", itemCaptor.getValue().getAcao());
    }

    @Test
    void deveSincronizarSomenteSaldoEPreservarCustoEReserva() {
        String hashOriginal = "8".repeat(64);
        LocalEstoque local = new LocalEstoque();
        local.setId(3L);
        local.setNome("Estoque Principal");
        local.setAtivo(true);
        Material material = new Material();
        material.setId(7L);
        material.setNome("Patch Cord");
        material.setAtivo(true);
        material.setTipoControle(TipoControleEstoque.UNIDADE);
        material.setQuantidadeDisponivel(10);
        material.setQuantidadeReservada(2);
        material.setCustoMedio(new BigDecimal("5.0000"));

        when(localRepository.findById(3L)).thenReturn(Optional.of(local));
        when(materialRepository.findByAtivoTrueOrderByNomeAsc()).thenReturn(List.of(material));
        when(materialRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(material));
        when(importacaoRepository.saveAndFlush(any(ImportacaoEstoquePlanilha.class)))
                .thenAnswer(invocacao -> {
                    ImportacaoEstoquePlanilha importacao = invocacao.getArgument(0);
                    importacao.setId(32L);
                    return importacao;
                });
        when(importacaoRepository.save(any(ImportacaoEstoquePlanilha.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        when(estoqueService.reconciliarSaldoPlanilha(
                7L,
                3L,
                12,
                new BigDecimal("5.0000"),
                "Sincronização da aba ESTOQUE ATUAL de saldos.xlsx",
                "gestor"))
                .thenAnswer(invocacao -> {
                    material.setQuantidadeDisponivel(12);
                    return null;
                });

        SincronizacaoSaldosPlanilhaRequest request = new SincronizacaoSaldosPlanilhaRequest(
                "saldos.xlsx",
                hashOriginal,
                3L,
                List.of(new SincronizacaoSaldosPlanilhaRequest.ItemSaldo(
                        7L, "PATCH CORD", new BigDecimal("12"), 5)));

        ImportacaoEstoquePlanilhaResultadoDTO resultado = service.sincronizarSaldos(request, "gestor");

        assertEquals(12, material.getQuantidadeDisponivel());
        assertEquals(2, material.getQuantidadeReservada());
        assertEquals(new BigDecimal("5.0000"), material.getCustoMedio());
        assertEquals(1, resultado.materiaisAtualizados());
        assertEquals(1, resultado.ajustesPositivos());
        assertEquals(0, resultado.ajustesNegativos());
        assertEquals(new BigDecimal("60.00"), resultado.valorTotalImportado());
        assertEquals("SALDOS_SINCRONIZADOS", resultado.resultado());

        ArgumentCaptor<ImportacaoEstoqueItemPlanilha> itemCaptor =
                ArgumentCaptor.forClass(ImportacaoEstoqueItemPlanilha.class);
        verify(itemImportacaoRepository).save(itemCaptor.capture());
        assertEquals(new BigDecimal("10"), itemCaptor.getValue().getSaldoAnterior());
        assertEquals(new BigDecimal("12"), itemCaptor.getValue().getSaldoImportado());
        assertEquals(itemCaptor.getValue().getCustoAnterior(), itemCaptor.getValue().getCustoUnitario());
        assertEquals("SALDO_AUMENTADO", itemCaptor.getValue().getAcao());
    }

    @Test
    void devePreservarEntradaHistoricaSemSomarNovamenteAoSaldoConsolidado() {
        String hash = "7".repeat(64);
        LocalEstoque local = new LocalEstoque();
        local.setId(3L);
        local.setNome("Estoque Principal");
        local.setAtivo(true);
        Material material = new Material();
        material.setId(7L);
        material.setNome("Patch Cord");
        material.setTipoControle(TipoControleEstoque.UNIDADE);
        material.setQuantidadeDisponivel(10);

        when(importacaoRepository.findByHashSha256(hash)).thenReturn(Optional.empty());
        when(localRepository.findById(3L)).thenReturn(Optional.of(local));
        when(materialRepository.findAll()).thenReturn(List.of(material));
        when(importacaoRepository.saveAndFlush(any(ImportacaoEstoquePlanilha.class)))
                .thenAnswer(invocacao -> {
                    ImportacaoEstoquePlanilha importacao = invocacao.getArgument(0);
                    importacao.setId(12L);
                    return importacao;
                });
        when(importacaoRepository.save(any(ImportacaoEstoquePlanilha.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        ImportacaoEstoquePlanilhaRequest request = new ImportacaoEstoquePlanilhaRequest(
                "estoque.xlsx",
                hash,
                3L,
                true,
                false,
                null,
                null,
                BigDecimal.TEN,
                List.of(new ImportacaoEstoquePlanilhaRequest.ItemImportacao(
                        "Patch Cord", null, BigDecimal.TEN, new BigDecimal("12.50"), 5)),
                List.of(new ImportacaoEstoquePlanilhaRequest.EntradaImportacao(
                        "ADICAO",
                        "Fornecedor | 20/08/26",
                        "Fornecedor",
                        java.time.LocalDate.of(2026, 8, 20),
                        "Patch Cord",
                        new BigDecimal("2"),
                        new BigDecimal("12.50"),
                        5,
                        6)),
                List.of(),
                List.of(),
                List.of(),
                List.of());

        ImportacaoEstoquePlanilhaResultadoDTO resultado = service.importar(request, "gestor");

        assertEquals(1, resultado.entradasImportadas());
        verify(entradaImportacaoRepository).save(any(ImportacaoEntradaPlanilha.class));
        verify(estoqueService).reconciliarSaldoPlanilha(
                7L,
                3L,
                10,
                new BigDecimal("12.50"),
                "Inventário importado de estoque.xlsx",
                "gestor");
        verifyNoMoreInteractions(estoqueService);
    }

    @Test
    void saldoConsolidadoNaoDeveDebitarRetiradasHistoricasNovamente() {
        String hash = "c".repeat(64);
        LocalEstoque local = new LocalEstoque();
        local.setId(3L);
        local.setNome("Estoque Principal");
        local.setAtivo(true);
        Material material = new Material();
        material.setId(7L);
        material.setNome("Patch Cord");
        material.setTipoControle(TipoControleEstoque.UNIDADE);
        material.setQuantidadeDisponivel(10);
        Comarca comarca = new Comarca();
        comarca.setId(9L);
        comarca.setNomeComarca("Cuité");

        when(importacaoRepository.findByHashSha256(hash)).thenReturn(Optional.empty());
        when(localRepository.findById(3L)).thenReturn(Optional.of(local));
        when(materialRepository.findAll()).thenReturn(List.of(material));
        when(comarcaRepository.findById(9L)).thenReturn(Optional.of(comarca));
        when(importacaoRepository.saveAndFlush(any(ImportacaoEstoquePlanilha.class)))
                .thenAnswer(invocacao -> {
                    ImportacaoEstoquePlanilha importacao = invocacao.getArgument(0);
                    importacao.setId(21L);
                    return importacao;
                });
        when(importacaoRepository.save(any(ImportacaoEstoquePlanilha.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        ImportacaoEstoquePlanilhaRequest request = new ImportacaoEstoquePlanilhaRequest(
                "estoque-consolidado.xlsx",
                hash,
                3L,
                true,
                false,
                null,
                null,
                BigDecimal.TEN,
                List.of(new ImportacaoEstoquePlanilhaRequest.ItemImportacao(
                        "Patch Cord", null, new BigDecimal("8"), new BigDecimal("12.50"), 5)),
                List.of(),
                List.of(new ImportacaoEstoquePlanilhaRequest.RetiradaImportacao(
                        "ORDEM DE RETIRADA - CUITÉ",
                        9L,
                        null,
                        "Patch Cord",
                        new BigDecimal("10"),
                        new BigDecimal("2"),
                        new BigDecimal("8"),
                        new BigDecimal("12.50"),
                        null,
                        8)),
                List.of(),
                List.of(),
                List.of());

        service.importar(request, "gestor");

        verify(estoqueService).reconciliarSaldoPlanilha(
                7L,
                3L,
                8,
                new BigDecimal("12.50"),
                "Inventário importado de estoque-consolidado.xlsx",
                "gestor");
        verifyNoMoreInteractions(estoqueService);
    }

    @Test
    void deveBloquearArquivoJaImportado() {
        String hash = "b".repeat(64);
        ImportacaoEstoquePlanilha existente = new ImportacaoEstoquePlanilha();
        existente.setId(5L);
        when(importacaoRepository.findByHashSha256(hash)).thenReturn(Optional.of(existente));
        ImportacaoEstoquePlanilhaRequest request = new ImportacaoEstoquePlanilhaRequest(
                "estoque.xlsx",
                hash,
                3L,
                List.of(new ImportacaoEstoquePlanilhaRequest.ItemImportacao(
                        "Patch Cord",
                        8,
                        BigDecimal.TEN)),
                List.of());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.importar(request, "gestor"));

        assertEquals(
                "Esta planilha já foi importada. Nenhum saldo foi alterado.",
                exception.getMessage());
    }

    @Test
    void deveComplementarArquivoExistenteComRetiradaEFaltaEReconciliarSaldoFinal() {
        String hash = "c".repeat(64);
        LocalEstoque local = new LocalEstoque();
        local.setId(3L);
        local.setNome("Estoque Principal");
        Material material = new Material();
        material.setId(7L);
        material.setNome("Terminal");
        material.setTipoControle(TipoControleEstoque.UNIDADE);
        material.setQuantidadeDisponivel(10);
        Comarca comarca = new Comarca();
        comarca.setId(9L);
        comarca.setNomeComarca("Esperança");
        ImportacaoEstoquePlanilha existente = new ImportacaoEstoquePlanilha();
        existente.setId(5L);
        existente.setNomeArquivo("estoque.xlsx");
        existente.setHashSha256(hash);
        existente.setImportadoPor("gestor");
        existente.setDataImportacao(java.time.LocalDateTime.now());
        existente.setLocalEstoque(local);
        existente.setItensProcessados(1);

        when(importacaoRepository.findByHashSha256(hash)).thenReturn(Optional.of(existente));
        when(materialRepository.findAll()).thenReturn(List.of(material));
        when(itemImportacaoRepository.existsByImportacaoId(5L)).thenReturn(true);
        when(itemImportacaoRepository.findByImportacaoIdOrderByNomePlanilhaAsc(5L))
                .thenReturn(List.of(itemOriginal(existente, material, "Terminal", 10, "8.09")));
        when(comarcaRepository.findById(9L)).thenReturn(Optional.of(comarca));
        when(retiradaImportacaoRepository.save(any(ImportacaoRetiradaPlanilha.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        ImportacaoEstoquePlanilhaRequest request = new ImportacaoEstoquePlanilhaRequest(
                "estoque.xlsx",
                hash,
                3L,
                List.of(new ImportacaoEstoquePlanilhaRequest.ItemImportacao(
                        "Terminal", 10, new BigDecimal("8.09"))),
                List.of(new ImportacaoEstoquePlanilhaRequest.RetiradaImportacao(
                        "ESPERANÇA",
                        9L,
                        "Terminal",
                        BigDecimal.TEN,
                        new BigDecimal("12"),
                        new BigDecimal("-2"),
                        new BigDecimal("8.09"),
                        null)));

        ImportacaoEstoquePlanilhaResultadoDTO resultado = service.importar(request, "auditor");

        assertEquals("COMPLEMENTADA", resultado.resultado());
        assertEquals(1, resultado.abasRetiradaProcessadas());
        assertEquals(1, resultado.retiradasImportadas());
        assertEquals(1, resultado.faltasIdentificadas());
        verify(retiradaImportacaoRepository).save(any(ImportacaoRetiradaPlanilha.class));
        verify(estoqueService).reconciliarSaldoPlanilha(
                7L,
                3L,
                0,
                new BigDecimal("8.09"),
                "Saldo final após retiradas importadas de estoque.xlsx",
                "auditor");
    }

    @Test
    void deveBloquearImportacaoParcialQuandoParserEncontrouLinhaInvalida() {
        ImportacaoEstoquePlanilhaRequest request = new ImportacaoEstoquePlanilhaRequest(
                "estoque.xlsx",
                "d".repeat(64),
                3L,
                List.of(new ImportacaoEstoquePlanilhaRequest.ItemImportacao(
                        "Patch Cord", 8, BigDecimal.TEN, 4)),
                List.of(),
                List.of("Linha 5: quantidade inválida para Conector."));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.importar(request, "gestor"));

        assertTrue(exception.getMessage().contains("Linha 5"));
        verifyNoInteractions(importacaoRepository, estoqueService);
    }

    @Test
    void deveBloquearSaldoFinalQueNaoCorrespondeARetirada() {
        ImportacaoEstoquePlanilhaRequest request = requestComRetiradas(
                List.of(retirada("ESPERANÇA", "Terminal", "10", "3", "9", 8)));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.importar(request, "gestor"));

        assertTrue(exception.getMessage().contains("deveria ser 7"));
        assertTrue(exception.getMessage().contains("linha 8"));
        verifyNoInteractions(importacaoRepository, estoqueService);
    }

    @Test
    void deveBloquearQuebraDeSequenciaEntreAbas() {
        ImportacaoEstoquePlanilhaRequest request = requestComRetiradas(List.of(
                retirada("ESPERANÇA", "Terminal", "10", "3", "7", 8),
                retirada("CUITÉ", "Terminal", "8", "2", "6", 9)));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.importar(request, "gestor"));

        assertTrue(exception.getMessage().contains("esperado 7, recebido 8"));
        verifyNoInteractions(importacaoRepository, estoqueService);
    }

    @Test
    void deveBloquearRetiradaDeMaterialAusenteDoInventarioBase() {
        ImportacaoEstoquePlanilhaRequest request = requestComRetiradas(
                List.of(retirada("ESPERANÇA", "Conector", "0", "2", "-2", 11)));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.importar(request, "gestor"));

        assertTrue(exception.getMessage().contains("não existe no inventário-base"));
        verifyNoInteractions(importacaoRepository, estoqueService);
    }

    @Test
    void deveBloquearComplementacaoEmDepositoDiferente() {
        String hash = "e".repeat(64);
        LocalEstoque local = new LocalEstoque();
        local.setId(3L);
        local.setNome("Estoque Principal");
        ImportacaoEstoquePlanilha existente = importacaoExistente(5L, hash, local);
        when(importacaoRepository.findByHashSha256(hash)).thenReturn(Optional.of(existente));

        ImportacaoEstoquePlanilhaRequest request = new ImportacaoEstoquePlanilhaRequest(
                "estoque.xlsx",
                hash,
                4L,
                List.of(new ImportacaoEstoquePlanilhaRequest.ItemImportacao(
                        "Terminal", 10, new BigDecimal("8.09"), 3)),
                List.of(retirada("ESPERANÇA", "Terminal", "10", "2", "8", 8)));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.importar(request, "gestor"));

        assertTrue(exception.getMessage().contains("mesmo depósito"));
        verifyNoInteractions(estoqueService);
    }

    @Test
    void deveBloquearComplementacaoComInventarioBaseAlterado() {
        String hash = "f".repeat(64);
        LocalEstoque local = new LocalEstoque();
        local.setId(3L);
        local.setNome("Estoque Principal");
        Material material = new Material();
        material.setId(7L);
        material.setNome("Terminal");
        ImportacaoEstoquePlanilha existente = importacaoExistente(5L, hash, local);
        when(importacaoRepository.findByHashSha256(hash)).thenReturn(Optional.of(existente));
        when(itemImportacaoRepository.findByImportacaoIdOrderByNomePlanilhaAsc(5L))
                .thenReturn(List.of(itemOriginal(existente, material, "Terminal", 10, "8.09")));

        ImportacaoEstoquePlanilhaRequest request = new ImportacaoEstoquePlanilhaRequest(
                "estoque.xlsx",
                hash,
                3L,
                List.of(new ImportacaoEstoquePlanilhaRequest.ItemImportacao(
                        "Terminal", 11, new BigDecimal("8.09"), 3)),
                List.of(retirada("ESPERANÇA", "Terminal", "11", "2", "9", 8)));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.importar(request, "gestor"));

        assertTrue(exception.getMessage().contains("inventário-base foi alterado"));
        verifyNoInteractions(estoqueService);
    }

    @Test
    void deveBloquearSegundaComplementacaoDoMesmoArquivo() {
        String hash = "1".repeat(64);
        LocalEstoque local = new LocalEstoque();
        local.setId(3L);
        local.setNome("Estoque Principal");
        ImportacaoEstoquePlanilha existente = importacaoExistente(5L, hash, local);
        when(importacaoRepository.findByHashSha256(hash)).thenReturn(Optional.of(existente));
        when(retiradaImportacaoRepository.existsByImportacaoId(5L)).thenReturn(true);

        ImportacaoEstoquePlanilhaRequest request = new ImportacaoEstoquePlanilhaRequest(
                "estoque.xlsx",
                hash,
                3L,
                List.of(new ImportacaoEstoquePlanilhaRequest.ItemImportacao(
                        "Terminal", 10, new BigDecimal("8.09"), 3)),
                List.of(retirada("ESPERANÇA", "Terminal", "10", "2", "8", 8)));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.importar(request, "gestor"));

        assertEquals(
                "O estoque e as retiradas desta planilha já foram importados.",
                exception.getMessage());
        verifyNoInteractions(estoqueService);
    }

    private ImportacaoEstoquePlanilhaRequest requestComRetiradas(
            List<ImportacaoEstoquePlanilhaRequest.RetiradaImportacao> retiradas) {
        return new ImportacaoEstoquePlanilhaRequest(
                "estoque.xlsx",
                "9".repeat(64),
                3L,
                List.of(new ImportacaoEstoquePlanilhaRequest.ItemImportacao(
                        "Terminal", 10, new BigDecimal("8.09"), 3)),
                retiradas);
    }

    private ImportacaoEstoquePlanilhaRequest.RetiradaImportacao retirada(
            String aba,
            String material,
            String saldoInicial,
            String quantidade,
            String saldoFinal,
            int linha) {
        return new ImportacaoEstoquePlanilhaRequest.RetiradaImportacao(
                aba,
                9L,
                material,
                new BigDecimal(saldoInicial),
                new BigDecimal(quantidade),
                new BigDecimal(saldoFinal),
                new BigDecimal("8.09"),
                null,
                linha);
    }

    private ImportacaoEstoquePlanilha importacaoExistente(
            Long id, String hash, LocalEstoque local) {
        ImportacaoEstoquePlanilha importacao = new ImportacaoEstoquePlanilha();
        importacao.setId(id);
        importacao.setNomeArquivo("estoque.xlsx");
        importacao.setHashSha256(hash);
        importacao.setImportadoPor("gestor");
        importacao.setDataImportacao(java.time.LocalDateTime.now());
        importacao.setLocalEstoque(local);
        importacao.setItensProcessados(1);
        return importacao;
    }

    private ImportacaoEstoqueItemPlanilha itemOriginal(
            ImportacaoEstoquePlanilha importacao,
            Material material,
            String nome,
            int quantidade,
            String custo) {
        ImportacaoEstoqueItemPlanilha item = new ImportacaoEstoqueItemPlanilha();
        item.setImportacao(importacao);
        item.setMaterial(material);
        item.setNomePlanilha(nome);
        item.setSaldoAnterior(BigDecimal.ZERO);
        item.setSaldoImportado(BigDecimal.valueOf(quantidade));
        item.setCustoUnitario(new BigDecimal(custo));
        item.setAcao("ATUALIZADO");
        return item;
    }
}
