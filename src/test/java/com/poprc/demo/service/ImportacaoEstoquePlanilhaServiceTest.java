package com.poprc.demo.service;

import com.poprc.demo.dto.ImportacaoEstoquePlanilhaRequest;
import com.poprc.demo.dto.ImportacaoEstoquePlanilhaResultadoDTO;
import com.poprc.demo.model.ImportacaoEstoquePlanilha;
import com.poprc.demo.model.LocalEstoque;
import com.poprc.demo.model.Material;
import com.poprc.demo.model.TipoControleEstoque;
import com.poprc.demo.repository.ImportacaoEstoquePlanilhaRepository;
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
    private MaterialRepository materialRepository;
    private LocalEstoqueRepository localRepository;
    private EstoqueService estoqueService;
    private ImportacaoEstoquePlanilhaService service;

    @BeforeEach
    void setUp() {
        importacaoRepository = mock(ImportacaoEstoquePlanilhaRepository.class);
        materialRepository = mock(MaterialRepository.class);
        localRepository = mock(LocalEstoqueRepository.class);
        estoqueService = mock(EstoqueService.class);
        service = new ImportacaoEstoquePlanilhaService(
                importacaoRepository, materialRepository, localRepository, estoqueService);
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

        when(importacaoRepository.existsByHashSha256(hash)).thenReturn(false);
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
                        new BigDecimal("12.50"))));

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
        when(importacaoRepository.existsByHashSha256(hash)).thenReturn(true);
        ImportacaoEstoquePlanilhaRequest request = new ImportacaoEstoquePlanilhaRequest(
                "estoque.xlsx",
                hash,
                3L,
                List.of(new ImportacaoEstoquePlanilhaRequest.ItemImportacao(
                        "Patch Cord",
                        8,
                        BigDecimal.TEN)));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.importar(request, "gestor"));

        assertEquals(
                "Esta planilha já foi importada. Nenhum saldo foi alterado.",
                exception.getMessage());
    }
}
