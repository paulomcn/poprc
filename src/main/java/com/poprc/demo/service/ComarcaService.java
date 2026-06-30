package com.poprc.demo.service;

import com.poprc.demo.model.Comarca;
import com.poprc.demo.model.Projeto;
import com.poprc.demo.repository.ComarcaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public Comarca atualizarComarca(Long id, BigDecimal percentualConcluido, String pendencias) {
        Optional<Comarca> comarcaOpt = comarcaRepository.findById(id);
        if (comarcaOpt.isPresent()) {
            Comarca comarca = comarcaOpt.get();
            if (percentualConcluido != null) {
                comarca.setPercentualConcluido(percentualConcluido);
            }
            // Permite limpar o campo de pendências (textarea vazia = resolvido)
            comarca.setPendencias(pendencias);
            return comarcaRepository.save(comarca);
        }
        return null;
    }

    @Transactional
    public Comarca criarOuVincularComarcaParaProjeto(Projeto projeto) {
        return comarcaRepository.findByProjetoId(projeto.getId())
                .orElseGet(() -> {
                    Comarca nova = new Comarca();
                    nova.setNomeComarca(determinarNomeComarca(projeto));
                    nova.setProjeto(projeto);
                    nova.setQuantidadePontos(0);
                    nova.setPercentualConcluido(BigDecimal.ZERO);
                    nova.setSituacao("EM_ANDAMENTO");
                    return comarcaRepository.save(nova);
                });
    }

    private String determinarNomeComarca(Projeto projeto) {
        if (projeto.getNomeComarcaVinculada() != null && !projeto.getNomeComarcaVinculada().isBlank()) {
            return projeto.getNomeComarcaVinculada();
        }
        if (projeto.getContrato() != null && projeto.getContrato().getCliente() != null) {
            return projeto.getContrato().getCliente();
        }
        return "Comarca do Projeto #" + projeto.getId();
    }
}
