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
        
        // Mock de custo: Como a entidade Material não possui valor monetário ainda, 
        // multiplicamos por um valor fixo de R$ 50.00 como placeholder. 
        // Substitua por mp.getMaterial().getPreco() quando atualizar o banco!
        BigDecimal custoUnitarioMock = BigDecimal.valueOf(50.00);
        BigDecimal custoAcumulado = custoUnitarioMock.multiply(BigDecimal.valueOf(mp.getQuantidadeUtilizada()));

        return new MetricasMaterialDTO(saldo, custoAcumulado);
    }

    // DTO interno para devolver as métricas limpas pro Controller
    @Data
    @AllArgsConstructor
    public static class MetricasMaterialDTO {
        private int saldo;
        private BigDecimal custoAcumulado;  
    }
}