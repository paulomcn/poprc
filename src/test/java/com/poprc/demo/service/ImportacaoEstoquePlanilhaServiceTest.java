package com.poprc.demo.service;

import com.poprc.demo.dto.ImportacaoEstoquePlanilhaRequest;
import com.poprc.demo.dto.ImportacaoEstoquePlanilhaResultadoDTO;
import com.poprc.demo.model.ImportacaoEstoquePlanilha;
import com.poprc.demo.model.Comarca;
import com.poprc.demo.model.ImportacaoRetiradaPlanilha;
import com.poprc.demo.model.LocalEstoque;
import com.poprc.demo.model.Material;
import com.poprc.demo.model.TipoControleEstoque;
import com.poprc.demo.repository.ComarcaRepository;
import com.poprc.demo.repository.ImportacaoEstoqueItemPlanilhaRepository;
import com.poprc.demo.repository.ImportacaoEstoquePlanilhaRepository;
import com.poprc.demo.repository.ImportacaoRetiradaPlanilhaRepository;
import com.poprc.demo.repository.LocalEstoqueRepository;
import com.poprc.demo.repository.MaterialRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImportacaoEstoquePlanilhaServiceTest {

    private ImportacaoEstoquePlanilhaRepository importacaoRepository;
    private ImportacaoEstoqueItemPlanilhaRepository itemImportacaoRepository;
    private ImportacaoRetiradaPlanilhaRepository retiradaImportacaoRepository;
    private MaterialRepository materialRepository;
    private LocalEstoqueRepository localRepository;
    private ComarcaRepository comarcaRepository;
    private EstoqueService estoqueService;
    private ImportacaoEstoquePlanilhaService service;

    @BeforeEach
    void setUp() {
        importacaoRepository = mock(ImportacaoEstoquePlanilhaRepository.class);
        itemImportacaoRepository = mock(ImportacaoEstoqueItemPlanilhaRepository.class);
        retiradaImportacaoRepository = mock(ImportacaoRetiradaPlanilhaRepository.class);
        materialRepository = mock(MaterialRepository.class);
        localRepository = mock(LocalEstoqueRepository.class);
        comarcaRepository = mock(ComarcaRepository.class);
        estoqueService = mock(EstoqueService.class);
        service = new ImportacaoEstoquePlanilhaService(
                importacaoRepository,
                itemImportacaoRepository,
                retiradaImportacaoRepository,
                materialRepository,
                localRepository,
                comarcaRepository,
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
}
