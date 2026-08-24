package com.poprc.demo.integration;

import com.poprc.demo.dto.ImportacaoEstoquePlanilhaRequest;
import com.poprc.demo.model.LocalEstoque;
import com.poprc.demo.model.Material;
import com.poprc.demo.model.TipoControleEstoque;
import com.poprc.demo.model.UnidadeMedida;
import com.poprc.demo.repository.ImportacaoEstoquePlanilhaRepository;
import com.poprc.demo.repository.LocalEstoqueRepository;
import com.poprc.demo.repository.MaterialRepository;
import com.poprc.demo.service.ImportacaoEstoquePlanilhaService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class ImportacaoEstoquePlanilhaRollbackIntegrationTest {

    @Autowired
    private ImportacaoEstoquePlanilhaService importacaoService;
    @Autowired
    private ImportacaoEstoquePlanilhaRepository importacaoRepository;
    @Autowired
    private LocalEstoqueRepository localRepository;
    @Autowired
    private MaterialRepository materialRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private EntityManager entityManager;

    @Test
    void desfazTodaAImportacaoQuandoUmItemPosteriorFalha() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        String hash = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        LocalEstoque local = novoLocal("Depósito rollback " + sufixo);
        Material unidade = novoMaterial("Material unidade " + sufixo, "RB-U-" + sufixo,
                TipoControleEstoque.UNIDADE, 5);
        Material metragem = novoMaterial("Material metragem " + sufixo, "RB-M-" + sufixo,
                TipoControleEstoque.METRAGEM, 0);

        try {
            ImportacaoEstoquePlanilhaRequest request = new ImportacaoEstoquePlanilhaRequest(
                    "rollback.xlsx",
                    hash,
                    local.getId(),
                    List.of(
                            new ImportacaoEstoquePlanilhaRequest.ItemImportacao(
                                    unidade.getNome(), 8, BigDecimal.TEN, 2),
                            new ImportacaoEstoquePlanilhaRequest.ItemImportacao(
                                    metragem.getNome(), 3, BigDecimal.ONE, 3)),
                    List.of());

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> importacaoService.importar(request, "JUnit"));

            assertTrue(exception.getMessage().contains("controle por metragem/bobina"));
            assertEquals(5, materialRepository.findById(unidade.getId()).orElseThrow()
                    .getQuantidadeDisponivel());
            assertTrue(importacaoRepository.findByHashSha256(hash).isEmpty());
            long movimentos = entityManager.createQuery(
                            "select count(m) from MovimentacaoEstoque m where m.material.id = :materialId",
                            Long.class)
                    .setParameter("materialId", unidade.getId())
                    .getSingleResult();
            assertEquals(0L, movimentos);
        } finally {
            limpar(local.getId(), List.of(unidade.getId(), metragem.getId()));
        }
    }

    private LocalEstoque novoLocal(String nome) {
        LocalEstoque local = new LocalEstoque();
        local.setNome(nome);
        local.setAtivo(true);
        return localRepository.saveAndFlush(local);
    }

    private Material novoMaterial(
            String nome,
            String partNumber,
            TipoControleEstoque tipo,
            int quantidade) {
        Material material = new Material();
        material.setNome(nome);
        material.setPartNumber(partNumber);
        material.setCategoria("MATERIAL_CONSUMO");
        material.setTipoControle(tipo);
        material.setUnidadeMedida(
                TipoControleEstoque.UNIDADE.equals(tipo) ? UnidadeMedida.UNIDADE : UnidadeMedida.METRO);
        material.setQuantidadeDisponivel(quantidade);
        material.setQuantidadeReservada(0);
        material.setCustoMedio(BigDecimal.ZERO);
        material.setEstoqueMinimo(BigDecimal.ZERO);
        return materialRepository.saveAndFlush(material);
    }

    private void limpar(Long localId, List<Long> materiaisIds) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            for (Long materialId : materiaisIds) {
                entityManager.createQuery("delete from MovimentacaoEstoque m where m.material.id = :id")
                        .setParameter("id", materialId)
                        .executeUpdate();
                entityManager.createQuery("delete from SaldoMaterialLocal s where s.material.id = :id")
                        .setParameter("id", materialId)
                        .executeUpdate();
            }
            materialRepository.deleteAllById(materiaisIds);
            localRepository.deleteById(localId);
        });
    }
}
