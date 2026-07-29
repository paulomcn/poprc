package com.poprc.demo.service;

import com.poprc.demo.model.MaterialProjeto;
import com.poprc.demo.repository.MaterialProjetoRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class MaterialProjetoService {

    private final MaterialProjetoRepository repository;

    @Transactional
    public MaterialProjeto salvar(MaterialProjeto mp) {
        return repository.save(mp);
    }

    @Transactional(readOnly = true)
    public MetricasMaterialDTO calcularMetricas(Long id) {
        MaterialProjeto mp = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vínculo de Material e Projeto não encontrado"));

        int saldo = mp.getQuantidadePrevista() - mp.getQuantidadeUtilizada();
        BigDecimal custoMedio = mp.getMaterial() != null ? mp.getMaterial().getCustoMedio() : null;
        boolean custoDisponivel = custoMedio != null && custoMedio.signum() > 0;
        BigDecimal custoAcumulado = custoDisponivel
                ? custoMedio.multiply(BigDecimal.valueOf(mp.getQuantidadeUtilizada()))
                : null;

        return new MetricasMaterialDTO(saldo, custoAcumulado, custoDisponivel);
    }

    // DTO interno para devolver as métricas limpas pro Controller
    @Data
    @AllArgsConstructor
    public static class MetricasMaterialDTO {
        private int saldo;
        private BigDecimal custoAcumulado;
        private boolean custoDisponivel;
    }
}
