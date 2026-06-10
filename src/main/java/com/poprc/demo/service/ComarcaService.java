package com.poprc.demo.service;

import com.poprc.demo.model.Comarca;
import com.poprc.demo.repository.ComarcaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ComarcaService {

    private final ComarcaRepository comarcaRepository;

    public Optional<Comarca> obterPorId(Long id) {
        return comarcaRepository.findById(id);
    }

    public Comarca atualizarProgresso(Long id, BigDecimal percentualConcluido, String situacao) {
        Optional<Comarca> comarcaOpt = comarcaRepository.findById(id);
        if (comarcaOpt.isPresent()) {
            Comarca comarca = comarcaOpt.get();
            if (percentualConcluido != null) {
                comarca.setPercentualConcluido(percentualConcluido);
            }
            if (situacao != null) {
                comarca.setSituacao(situacao);
            }
            return comarcaRepository.save(comarca);
        }
        return null;
    }

    public Comarca atualizarPendencias(Long id, String pendencias) {
        Optional<Comarca> comarcaOpt = comarcaRepository.findById(id);
        if (comarcaOpt.isPresent()) {
            Comarca comarca = comarcaOpt.get();
            if (pendencias != null) {
                comarca.setPendencias(pendencias);
            }
            return comarcaRepository.save(comarca);
        }
        return null;
    }
}
