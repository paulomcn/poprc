package com.poprc.demo.integration;

import com.poprc.demo.model.Comarca;
import com.poprc.demo.model.Material;
import com.poprc.demo.model.SaldoMaterialLocal;
import com.poprc.demo.model.TipoControleEstoque;
import com.poprc.demo.model.UnidadeMedida;
import com.poprc.demo.repository.ComarcaRepository;
import com.poprc.demo.repository.MaterialRepository;
import com.poprc.demo.service.ComarcaService;
import com.poprc.demo.service.EstoqueService;
import com.poprc.demo.service.SaldoLocalService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SaldoLocalReservaIntegrationTest {
    @Autowired private EstoqueService estoque;
    @Autowired private SaldoLocalService saldos;
    @Autowired private ComarcaService obras;
    @Autowired private ComarcaRepository comarcas;
    @Autowired private MaterialRepository materiais;
    @Autowired private EntityManager entityManager;

    @ParameterizedTest
    @EnumSource(value = TipoControleEstoque.class, names = {"UNIDADE", "FRACIONADO", "METRAGEM"})
    void espelhaReservaAlteracaoTransferenciaELiberacaoEntreDepositos(TipoControleEstoque tipo) {
        String sufixo = UUID.randomUUID().toString();
        boolean decimal = tipo != TipoControleEstoque.UNIDADE;
        BigDecimal saldoInicial = new BigDecimal(decimal ? "10.500" : "10");
        Material material = new Material();
        material.setNome("Material reservas " + sufixo);
        material.setPartNumber(sufixo);
        material.setTipoControle(tipo);
        material.setUnidadeMedida(decimal ? UnidadeMedida.METRO : UnidadeMedida.UNIDADE);
        material.setLocalizacao("Origem " + sufixo);
        material.setQuantidadeDisponivel(decimal ? 0 : 10);
        material.setMetragemDisponivel(decimal ? saldoInicial : BigDecimal.ZERO);
        Long id = estoque.cadastrarMaterial(material).getId();
        Long origem = saldos.listarSaldos(id).getFirst().getLocalEstoque().getId();
        Long destino = saldos.cadastrarLocal("Destino " + sufixo, "Teste").getId();
        estoque.transferirLocalizacao(id, origem, destino, new BigDecimal("8"), "Teste", "JUnit", "JUnit");

        Comarca comarca = new Comarca();
        comarca.setNomeComarca("Obra reservas " + sufixo);
        Long comarcaId = comarcas.saveAndFlush(comarca).getId();
        BigDecimal reserva = new BigDecimal(decimal ? "7.250" : "7");
        obras.adicionarMaterialPrevisto(comarcaId, id, material.getNome(), reserva);
        conferir(id, saldoInicial, reserva, decimal);
        Long itemId = obras.listarMateriaisPrevistos(comarcaId).getFirst().getId();

        reserva = new BigDecimal(decimal ? "6.125" : "6");
        obras.atualizarMaterialPrevisto(itemId, id, material.getNome(), reserva);
        conferir(id, saldoInicial, reserva, decimal);
        estoque.transferirLocalizacao(id, origem, destino, BigDecimal.ONE, "Teste", "JUnit", "JUnit");
        conferir(id, saldoInicial, reserva, decimal);
        assertThat(saldos.listarSaldos(id).stream()
                .filter(s -> s.getLocalEstoque().getId().equals(origem)).map(s -> reserva(s, decimal)).toList())
                .singleElement().satisfies(valor -> assertThat(valor).isEqualByComparingTo(decimal ? "1.500" : "1"));

        obras.removerMaterialPrevisto(itemId);
        conferir(id, saldoInicial, BigDecimal.ZERO, decimal);
    }

    private void conferir(Long id, BigDecimal saldo, BigDecimal reserva, boolean decimal) {
        entityManager.flush();
        entityManager.clear();
        Material material = materiais.findById(id).orElseThrow();
        assertThat(decimal ? material.getMetragemReservada() : BigDecimal.valueOf(material.getQuantidadeReservada()))
                .isEqualByComparingTo(reserva);
        var locais = saldos.listarSaldos(id);
        assertThat(locais).hasSize(2).allSatisfy(s ->
                assertThat(reserva(s, decimal)).isBetween(BigDecimal.ZERO, saldo(s, decimal)));
        assertThat(locais.stream().map(s -> reserva(s, decimal)).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo(reserva);
        assertThat(locais.stream().map(s -> saldo(s, decimal)).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo(saldo);
    }

    private BigDecimal saldo(SaldoMaterialLocal saldo, boolean decimal) {
        return decimal ? saldo.getMetragemDisponivel() : BigDecimal.valueOf(saldo.getQuantidadeDisponivel());
    }

    private BigDecimal reserva(SaldoMaterialLocal saldo, boolean decimal) {
        return decimal ? saldo.getMetragemReservada() : BigDecimal.valueOf(saldo.getQuantidadeReservada());
    }
}
