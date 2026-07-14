package com.poprc.demo.service;

import com.poprc.demo.model.MaterialProjeto;
import com.poprc.demo.repository.MaterialProjetoRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MaterialProjetoServiceTest {

    @Test
    void deveRetornarSaldoSemInventarCustoDoMaterial() {
        MaterialProjetoRepository repository = mock(MaterialProjetoRepository.class);
        MaterialProjetoService service = new MaterialProjetoService(repository);
        MaterialProjeto materialProjeto = new MaterialProjeto();
        materialProjeto.setQuantidadePrevista(12);
        materialProjeto.setQuantidadeUtilizada(5);
        when(repository.findById(3L)).thenReturn(Optional.of(materialProjeto));

        MaterialProjetoService.MetricasMaterialDTO metricas = service.calcularMetricas(3L);

        assertEquals(7, metricas.getSaldo());
        assertNull(metricas.getCustoAcumulado());
        assertFalse(metricas.isCustoDisponivel());
    }
}
