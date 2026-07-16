package com.poprc.demo.service;

import com.poprc.demo.model.SaldoMaterialLocal;
import com.poprc.demo.repository.LocalEstoqueRepository;
import com.poprc.demo.repository.SaldoMaterialLocalRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaldoLocalServiceTest {

    @Test
    void deveSalvarMinimoLocalEPermitirRetornoAoPadraoGlobal() {
        SaldoMaterialLocalRepository repository = mock(SaldoMaterialLocalRepository.class);
        SaldoLocalService service = new SaldoLocalService(mock(LocalEstoqueRepository.class), repository);
        SaldoMaterialLocal saldo = new SaldoMaterialLocal();
        saldo.setId(8L);
        when(repository.findById(8L)).thenReturn(Optional.of(saldo));
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
        SaldoLocalService service = new SaldoLocalService(mock(LocalEstoqueRepository.class), repository);
        SaldoMaterialLocal saldo = new SaldoMaterialLocal();
        when(repository.findById(9L)).thenReturn(Optional.of(saldo));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.atualizarEstoqueMinimo(9L, new BigDecimal("-1")));

        assertEquals("O estoque mínimo local não pode ser negativo.", exception.getMessage());
    }
}
