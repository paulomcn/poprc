package com.poprc.demo.integration;

import com.poprc.demo.model.Material;
import com.poprc.demo.model.MovimentacaoEstoque;
import com.poprc.demo.model.SaldoMaterialLocal;
import com.poprc.demo.model.TipoControleEstoque;
import com.poprc.demo.model.TipoMovimentacao;
import com.poprc.demo.model.UnidadeMedida;
import com.poprc.demo.repository.MaterialRepository;
import com.poprc.demo.repository.MovimentacaoEstoqueRepository;
import com.poprc.demo.service.EstoqueService;
import com.poprc.demo.service.SaldoLocalService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class EstoqueConcorrenciaIntegrationTest {

    @Autowired
    private EstoqueService estoqueService;
    @Autowired
    private SaldoLocalService saldoLocalService;
    @Autowired
    private MaterialRepository materialRepository;
    @Autowired
    private MovimentacaoEstoqueRepository movimentacaoRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private EntityManager entityManager;

    @Test
    void impedeDoisDebitosSimultaneosDeConsumiremOMesmoSaldo() throws Exception {
        Material material = novoMaterialComSaldoDez();
        Long materialId = material.getId();
        SaldoMaterialLocal saldo = saldoLocalService.listarSaldos(materialId).getFirst();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        try {
            Future<Boolean> primeira = executor.submit(
                    () -> tentarDebitar(materialId, saldo.getLocalEstoque().getId(), prontas, iniciar));
            Future<Boolean> segunda = executor.submit(
                    () -> tentarDebitar(materialId, saldo.getLocalEstoque().getId(), prontas, iniciar));

            assertTrue(prontas.await(5, TimeUnit.SECONDS));
            iniciar.countDown();

            int sucessos = (primeira.get(10, TimeUnit.SECONDS) ? 1 : 0)
                    + (segunda.get(10, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, sucessos);
            assertEquals(3, materialRepository.findById(materialId).orElseThrow().getQuantidadeDisponivel());
            assertEquals(3, saldoLocalService.listarSaldos(materialId).getFirst().getQuantidadeDisponivel());

            List<MovimentacaoEstoque> ajustes = movimentacaoRepository.findAll().stream()
                    .filter(movimento -> movimento.getMaterial().getId().equals(materialId))
                    .filter(movimento -> TipoMovimentacao.AJUSTE_NEGATIVO.equals(movimento.getTipo()))
                    .toList();
            assertEquals(1, ajustes.size());
        } finally {
            iniciar.countDown();
            executor.shutdownNow();
            limparMaterial(materialId);
        }
    }

    private boolean tentarDebitar(Long materialId, Long localId, CountDownLatch prontas, CountDownLatch iniciar) {
        prontas.countDown();
        try {
            iniciar.await(5, TimeUnit.SECONDS);
            estoqueService.registrarAjuste(materialId, localId, BigDecimal.valueOf(7),
                    TipoMovimentacao.AJUSTE_NEGATIVO, "Teste concorrente", "JUnit", "JUnit");
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private Material novoMaterialComSaldoDez() {
        Material material = new Material();
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        material.setNome("Material concorrência " + sufixo);
        material.setPartNumber("CONC-" + sufixo);
        material.setCategoria("MATERIAL_CONSUMO");
        material.setTipoControle(TipoControleEstoque.UNIDADE);
        material.setUnidadeMedida(UnidadeMedida.UNIDADE);
        material.setQuantidadeDisponivel(10);
        material.setLocalizacao("Estoque Concorrência");
        return estoqueService.cadastrarMaterial(material);
    }

    private void limparMaterial(Long materialId) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            entityManager.createQuery("delete from MovimentacaoEstoque m where m.material.id = :materialId")
                    .setParameter("materialId", materialId)
                    .executeUpdate();
            entityManager.createQuery("delete from SaldoMaterialLocal s where s.material.id = :materialId")
                    .setParameter("materialId", materialId)
                    .executeUpdate();
            materialRepository.deleteById(materialId);
        });
    }
}
