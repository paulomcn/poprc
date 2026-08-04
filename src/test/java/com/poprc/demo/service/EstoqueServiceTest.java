package com.poprc.demo.service;

import com.poprc.demo.model.Funcionario;
import com.poprc.demo.model.LocalEstoque;
import com.poprc.demo.model.Material;
import com.poprc.demo.model.MovimentacaoEstoque;
import com.poprc.demo.model.TipoControleEstoque;
import com.poprc.demo.model.TipoMovimentacao;
import com.poprc.demo.model.UnidadeMedida;
import com.poprc.demo.repository.FuncionarioRepository;
import com.poprc.demo.repository.MaterialRepository;
import com.poprc.demo.repository.MovimentacaoEstoqueRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EstoqueServiceTest {

    private MaterialRepository materialRepository;
    private MovimentacaoEstoqueRepository movimentacaoRepository;
    private FuncionarioRepository funcionarioRepository;
    private SaldoLocalService saldoLocalService;
    private EstoqueService service;

    @BeforeEach
    void setUp() {
        materialRepository = mock(MaterialRepository.class);
        movimentacaoRepository = mock(MovimentacaoEstoqueRepository.class);
        funcionarioRepository = mock(FuncionarioRepository.class);
        saldoLocalService = mock(SaldoLocalService.class);
        service = new EstoqueService(
                materialRepository, movimentacaoRepository, funcionarioRepository, saldoLocalService);
    }

    @Test
    void deveRecalcularCustoMedioPonderadoNaEntrada() {
        Material material = new Material();
        material.setId(1L);
        material.setNome("Switch");
        material.setTipoControle(TipoControleEstoque.UNIDADE);
        material.setUnidadeMedida(UnidadeMedida.UNIDADE);
        material.setQuantidadeDisponivel(10);
        material.setCustoMedio(new BigDecimal("10.0000"));

        Funcionario funcionario = new Funcionario();
        funcionario.setId(2L);
        funcionario.setNome("Responsável pelo estoque");
        LocalEstoque local = new LocalEstoque();
        local.setId(3L);
        local.setNome("Estoque Principal");

        when(materialRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(material));
        when(funcionarioRepository.findById(2L)).thenReturn(Optional.of(funcionario));
        when(materialRepository.save(any(Material.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        when(saldoLocalService.creditar(material, 3L, BigDecimal.TEN)).thenReturn(local);
        when(movimentacaoRepository.save(any(MovimentacaoEstoque.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        MovimentacaoEstoque movimentacao = service.registrarEntrada(
                1L, 10, null, new BigDecimal("20.0000"), 2L, 3L);

        assertEquals(20, material.getQuantidadeDisponivel());
        assertEquals(new BigDecimal("15.0000"), material.getCustoMedio());
        assertEquals(new BigDecimal("300.00"), material.getValorTotalEstoque());
        assertEquals(new BigDecimal("10"), movimentacao.getSaldoAnterior());
        assertEquals(new BigDecimal("20"), movimentacao.getSaldoPosterior());
        assertEquals(new BigDecimal("20.0000"), movimentacao.getCustoUnitario());
        assertEquals(new BigDecimal("200.0000"), movimentacao.getValorTotalMovimentacao());
    }

    @Test
    void deveRemoverMaterialSemSaldoPreservandoRegistro() {
        Material material = new Material();
        material.setId(1L);
        material.setAtivo(true);
        material.setQuantidadeDisponivel(0);
        material.setQuantidadeReservada(0);
        material.setMetragemDisponivel(BigDecimal.ZERO);
        when(materialRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(material));
        when(materialRepository.save(any(Material.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        service.removerMaterial(1L, "Administrador");

        assertFalse(material.getAtivo());
        assertEquals("Administrador", material.getRemovidoPor());
    }

    @Test
    void deveBloquearRemocaoDeMaterialComSaldo() {
        Material material = new Material();
        material.setId(1L);
        material.setAtivo(true);
        material.setQuantidadeDisponivel(2);
        material.setQuantidadeReservada(0);
        material.setMetragemDisponivel(BigDecimal.ZERO);
        when(materialRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(material));

        IllegalArgumentException erro = assertThrows(
                IllegalArgumentException.class,
                () -> service.removerMaterial(1L, "Administrador"));

        assertEquals(
                "Zere o saldo disponível, a metragem e as reservas antes de remover o material.",
                erro.getMessage());
    }

    @Test
    void deveReconciliarReducaoDaPlanilhaComMovimentacaoAuditavel() {
        Material material = new Material();
        material.setId(1L);
        material.setNome("Switch");
        material.setTipoControle(TipoControleEstoque.UNIDADE);
        material.setUnidadeMedida(UnidadeMedida.UNIDADE);
        material.setQuantidadeDisponivel(10);
        material.setCustoMedio(new BigDecimal("8.0000"));
        LocalEstoque local = new LocalEstoque();
        local.setId(3L);
        local.setNome("Estoque Principal");

        when(materialRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(material));
        when(materialRepository.save(any(Material.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        when(saldoLocalService.debitarDistribuido(material, new BigDecimal("4")))
                .thenReturn(List.of(new SaldoLocalService.MovimentoLocal(
                        local, new BigDecimal("4"))));
        when(saldoLocalService.descreverMovimentos(any()))
                .thenReturn("Estoque Principal (4)");
        when(movimentacaoRepository.save(any(MovimentacaoEstoque.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        MovimentacaoEstoque movimentacao = service.reconciliarSaldoPlanilha(
                1L,
                3L,
                6,
                new BigDecimal("3.5000"),
                "Inventário importado",
                "gestor");

        assertEquals(6, material.getQuantidadeDisponivel());
        assertEquals(new BigDecimal("3.5000"), material.getCustoMedio());
        assertEquals(TipoMovimentacao.AJUSTE_NEGATIVO, movimentacao.getTipo());
        assertEquals(new BigDecimal("10"), movimentacao.getSaldoAnterior());
        assertEquals(new BigDecimal("6"), movimentacao.getSaldoPosterior());
        assertEquals(new BigDecimal("14.0000"), movimentacao.getValorTotalMovimentacao());
        assertEquals("Estoque Principal (4)", movimentacao.getEstoqueOrigem());
    }
}
