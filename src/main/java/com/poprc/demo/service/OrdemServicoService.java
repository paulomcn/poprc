package com.poprc.demo.service;

import com.poprc.demo.model.OrdemServico;
import com.poprc.demo.model.StatusOS;
import com.poprc.demo.repository.OrdemServicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrdemServicoService {

    private final OrdemServicoRepository ordemServicoRepository;

    public OrdemServico criar(OrdemServico ordemServico) {
        ordemServico.setStatus(StatusOS.ABERTA);
        return ordemServicoRepository.save(ordemServico);
    }

    public OrdemServico atualizarStatus(Long id, StatusOS novoStatus) {
        Optional<OrdemServico> ordemOpt = ordemServicoRepository.findById(id);
        if (ordemOpt.isPresent()) {
            OrdemServico ordem = ordemOpt.get();
            ordem.setStatus(novoStatus);
            return ordemServicoRepository.save(ordem);
        }
        return null;
    }

    public OrdemServico atualizarChecklist(Long id, String checklist) {
        Optional<OrdemServico> ordemOpt = ordemServicoRepository.findById(id);
        if (ordemOpt.isPresent()) {
            OrdemServico ordem = ordemOpt.get();
            ordem.setChecklist(checklist);
            return ordemServicoRepository.save(ordem);
        }
        return null;
    }
}
