package com.poprc.demo.service;

import com.poprc.demo.model.Comarca;
import com.poprc.demo.model.MaterialItem;
import com.poprc.demo.model.MaterialProjeto;
import com.poprc.demo.model.OrdemServico;
import com.poprc.demo.model.StatusOS;
import com.poprc.demo.repository.ComarcaRepository;
import com.poprc.demo.repository.MaterialItemRepository;
import com.poprc.demo.repository.MaterialProjetoRepository;
import com.poprc.demo.repository.OrdemServicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrdemServicoService {

    private final OrdemServicoRepository ordemServicoRepository;
    private final ComarcaRepository comarcaRepository;
    private final MaterialProjetoRepository materialProjetoRepository;
    private final MaterialItemRepository materialItemRepository;

    @Transactional
    public OrdemServico criar(OrdemServico ordemServico) {
        if (ordemServico.getNumeroOs() == null || ordemServico.getNumeroOs().isBlank()) {
            throw new IllegalArgumentException("Código da OS é obrigatório.");
        }
        if (ordemServico.getProjeto() == null || ordemServico.getProjeto().getId() == null) {
            throw new IllegalArgumentException("Projeto/Comarca alvo é obrigatório para criar a OS.");
        }

        ordemServico.setStatus(StatusOS.ABERTA);
        OrdemServico ordemSalva = ordemServicoRepository.save(ordemServico);
        vincularComarcaEPrepararAuditoria(ordemSalva);
        return ordemSalva;
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

    @Transactional
    public Map<String, Object> repararVinculosComarcas() {
        Map<String, Object> resultado = new HashMap<>();
        int ordensVinculadas = 0;
        int materiaisCriados = 0;
        int materiaisAtualizados = 0;
        int ordensSemProjeto = 0;
        int ordensSemComarca = 0;
        int conflitos = 0;

        for (OrdemServico ordem : ordemServicoRepository.findAll()) {
            if (ordem.getProjeto() == null || ordem.getProjeto().getId() == null) {
                ordensSemProjeto++;
                continue;
            }

            Optional<Comarca> comarcaOpt = comarcaRepository.findByProjetoId(ordem.getProjeto().getId());
            if (comarcaOpt.isEmpty()) {
                ordensSemComarca++;
                continue;
            }

            Comarca comarca = comarcaOpt.get();
            if (comarca.getOrdemServico() != null && !comarca.getOrdemServico().getId().equals(ordem.getId())) {
                conflitos++;
                continue;
            }

            if (comarca.getOrdemServico() == null) {
                comarca.setOrdemServico(ordem);
                ordensVinculadas++;
            }

            SincronizacaoMateriais sync = sincronizarMateriaisPrevistos(comarca);
            materiaisCriados += sync.criados();
            materiaisAtualizados += sync.atualizados();
            comarcaRepository.save(comarca);
        }

        resultado.put("ordensVinculadas", ordensVinculadas);
        resultado.put("materiaisCriados", materiaisCriados);
        resultado.put("materiaisAtualizados", materiaisAtualizados);
        resultado.put("ordensSemProjeto", ordensSemProjeto);
        resultado.put("ordensSemComarca", ordensSemComarca);
        resultado.put("conflitos", conflitos);
        return resultado;
    }

    private void vincularComarcaEPrepararAuditoria(OrdemServico ordemServico) {
        Comarca comarca = comarcaRepository.findByProjetoId(ordemServico.getProjeto().getId())
                .orElseThrow(() -> new IllegalArgumentException("Nenhuma comarca foi encontrada para o projeto informado."));

        if (comarca.getOrdemServico() != null
                && !comarca.getOrdemServico().getId().equals(ordemServico.getId())) {
            throw new IllegalArgumentException("Esta comarca já possui uma OS vinculada.");
        }

        comarca.setOrdemServico(ordemServico);
        sincronizarMateriaisPrevistos(comarca);
        comarcaRepository.save(comarca);
    }

    private SincronizacaoMateriais sincronizarMateriaisPrevistos(Comarca comarca) {
        List<MaterialProjeto> materiaisProjeto = materialProjetoRepository.findByProjetoId(comarca.getProjeto().getId());
        Set<String> nomesJaSincronizados = comarca.getMateriais() == null
                ? new java.util.HashSet<>()
                : comarca.getMateriais().stream()
                        .map(MaterialItem::getNomeMaterial)
                        .filter(nome -> nome != null && !nome.isBlank())
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet());
        int criados = 0;
        int atualizados = 0;

        for (MaterialProjeto materialProjeto : materiaisProjeto) {
            if (materialProjeto.getMaterial() == null) {
                continue;
            }

            String nomeMaterial = materialProjeto.getMaterial().getNome();
            if (nomeMaterial == null || nomeMaterial.isBlank()) {
                continue;
            }
            Optional<MaterialItem> materialExistente = comarca.getMateriais() == null
                    ? Optional.empty()
                    : comarca.getMateriais().stream()
                            .filter(item -> item.getNomeMaterial() != null
                                    && item.getNomeMaterial().equalsIgnoreCase(nomeMaterial))
                            .findFirst();

            if (materialExistente.isPresent()) {
                MaterialItem item = materialExistente.get();
                item.setMaterial(materialProjeto.getMaterial());
                item.setQuantidadePrevista(valorOuZero(materialProjeto.getQuantidadePrevista()));
                if (item.getQuantidadeAuditada() == 0 && materialProjeto.getQuantidadeUtilizada() != null) {
                    item.setQuantidadeAuditada(materialProjeto.getQuantidadeUtilizada());
                }
                materialItemRepository.save(item);
                atualizados++;
                continue;
            }

            if (nomesJaSincronizados.contains(nomeMaterial.toLowerCase())) {
                continue;
            }

            MaterialItem novoItem = new MaterialItem();
            novoItem.setComarca(comarca);
            novoItem.setMaterial(materialProjeto.getMaterial());
            novoItem.setNomeMaterial(nomeMaterial);
            novoItem.setQuantidadePrevista(valorOuZero(materialProjeto.getQuantidadePrevista()));
            novoItem.setQuantidadeAuditada(valorOuZero(materialProjeto.getQuantidadeUtilizada()));
            novoItem.setEstoqueBaixado(false);
            materialItemRepository.save(novoItem);
            nomesJaSincronizados.add(nomeMaterial.toLowerCase());
            criados++;
        }

        return new SincronizacaoMateriais(criados, atualizados);
    }

    private int valorOuZero(Integer valor) {
        return valor != null ? valor : 0;
    }

    private record SincronizacaoMateriais(int criados, int atualizados) {
    }
}
