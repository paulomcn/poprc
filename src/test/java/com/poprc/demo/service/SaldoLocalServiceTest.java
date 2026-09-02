package com.poprc.demo.service;

import com.poprc.demo.model.SaldoMaterialLocal;
import com.poprc.demo.model.Material;
import com.poprc.demo.model.TipoControleEstoque;
import com.poprc.demo.exception.SaldoInsuficienteException;
import com.poprc.demo.repository.LocalEstoqueRepository;
import com.poprc.demo.repository.MaterialRepository;
import com.poprc.demo.repository.SaldoMaterialLocalRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;

class SaldoLocalServiceTest {

    @Test
    void rejeitaReservaMaiorQueSaldoLocalSemSalvarEspelhoParcial() {
        SaldoMaterialLocalRepository repository = mock(SaldoMaterialLocalRepository.class);
        MaterialRepository materiais = mock(MaterialRepository.class);
        SaldoLocalService service = new SaldoLocalService(mock(LocalEstoqueRepository.class), repository, materiais);
        Material material = new Material();
        material.setId(1L);
        material.setNome("Material teste");
        material.setTipoControle(TipoControleEstoque.UNIDADE);
        material.setQuantidadeReservada(6);
        SaldoMaterialLocal saldo = new SaldoMaterialLocal();
        saldo.setMaterial(material);
        saldo.setQuantidadeDisponivel(5);
        when(materiais.findByIdForUpdate(1L)).thenReturn(Optional.of(material));
        when(repository.findByMaterialIdForUpdate(1L)).thenReturn(List.of(saldo));

        assertThrows(SaldoInsuficienteException.class, () -> service.sincronizarReservas(material));

        verify(repository, never()).save(any());
    }

    @Test
    void deveSalvarMinimoLocalEPermitirRetornoAoPadraoGlobal() {
        SaldoMaterialLocalRepository repository = mock(SaldoMaterialLocalRepository.class);
        SaldoLocalService service = new SaldoLocalService(
                mock(LocalEstoqueRepository.class), repository, mock(MaterialRepository.class));
        SaldoMaterialLocal saldo = new SaldoMaterialLocal();
        saldo.setId(8L);
        when(repository.findByIdForUpdate(8L)).thenReturn(Optional.of(saldo));
        when(repository.save(saldo)).thenReturn(saldo);

        service.atualizarEstoqueMinimo(8L, new BigDecimal("12.5"));
        assertEquals(new BigDecimal("12.5"), saldo.getEstoqueMinimo());

        service.atualizarEstoqueMinimo(8L, null);
        assertEquals(null, saldo.getEstoqueMinimo());
        verify(repository, org.mockito.Mockito.times(2)).save(saldo);
    }

    @Test
    void deveRejeitarMinimoLocalNegativo() {
        SaldoMaterialLocalRepository repository = mock(SaldoMaterialLocalRepository.class);
        SaldoLocalService service = new SaldoLocalService(
                mock(LocalEstoqueRepository.class), repository, mock(MaterialRepository.class));
        SaldoMaterialLocal saldo = new SaldoMaterialLocal();
        when(repository.findByIdForUpdate(9L)).thenReturn(Optional.of(saldo));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.atualizarEstoqueMinimo(9L, new BigDecimal("-1")));

        assertEquals("O estoque mínimo local não pode ser negativo.", exception.getMessage());
    }
}
