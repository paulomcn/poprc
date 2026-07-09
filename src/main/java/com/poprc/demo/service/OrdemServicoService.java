package com.poprc.demo.service;

import com.poprc.demo.dto.CriarOrdemServicoRequest;
import com.poprc.demo.model.Comarca;
import com.poprc.demo.model.Contrato;
import com.poprc.demo.model.Material;
import com.poprc.demo.model.MaterialItem;
import com.poprc.demo.model.MaterialProjeto;
import com.poprc.demo.model.OrdemRetirada;
import com.poprc.demo.model.OrdemServico;
import com.poprc.demo.model.Projeto;
import com.poprc.demo.model.StatusOS;
import com.poprc.demo.repository.ComarcaRepository;
import com.poprc.demo.repository.ContratoRepository;
import com.poprc.demo.repository.MaterialItemRepository;
import com.poprc.demo.repository.MaterialProjetoRepository;
import com.poprc.demo.repository.MaterialRepository;
import com.poprc.demo.repository.OrdemServicoRepository;
import com.poprc.demo.repository.ProjetoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final ContratoRepository contratoRepository;
    private final ProjetoRepository projetoRepository;
    private final MaterialRepository materialRepository;
    private final MaterialProjetoRepository materialProjetoRepository;
    private final MaterialItemRepository materialItemRepository;
    private final ComarcaService comarcaService;
    private final OrdemRetiradaPort ordemRetiradaPort;

    @Transactional
    public OrdemServico criar(CriarOrdemServicoRequest request) {
        if (request.getProjetoId() == null) {
            throw new IllegalArgumentException("Projeto/Comarca alvo é obrigatório para criar a OS.");
        }
        if (request.getContratoId() == null) {
            throw new IllegalArgumentException("Contrato é obrigatório para criar a OS.");
        }
        validarDatasObrigatorias(request.getDataHoraInicio(), request.getDataHoraFim(), request.getDeadline());
        if (request.getMateriais() == null || request.getMateriais().isEmpty()) {
            throw new IllegalArgumentException("Defina ao menos um material previsto para emitir a OS.");
        }

        Contrato contrato = contratoRepository.findById(request.getContratoId())
                .orElseThrow(() -> new IllegalArgumentException("Contrato não encontrado."));
        Projeto projeto = projetoRepository.findById(request.getProjetoId())
                .orElseThrow(() -> new IllegalArgumentException("Projeto/Comarca alvo não encontrado."));

        OrdemServico ordemServico = new OrdemServico();
        ordemServico.setNumeroOs(gerarNumeroOs(contrato));
        ordemServico.setContrato(contrato);
        ordemServico.setProjeto(projeto);
        ordemServico.setDescricao(request.getDescricao());
        ordemServico.setDataHoraInicio(request.getDataHoraInicio());
        ordemServico.setDataHoraFim(request.getDataHoraFim());
        ordemServico.setDeadline(request.getDeadline());
        ordemServico.setDataExecucao(request.getDataHoraInicio().toLocalDate());
        ordemServico.setStatus(StatusOS.ABERTA);

        OrdemServico ordemSalva = ordemServicoRepository.save(ordemServico);
        vincularComarcaEPrepararAuditoria(ordemSalva, request.getMateriais());
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

    private void vincularComarcaEPrepararAuditoria(OrdemServico ordemServico,
            List<CriarOrdemServicoRequest.MaterialPrevistoRequest> materiaisPrevistos) {
        Comarca comarca = comarcaRepository.findByProjetoId(ordemServico.getProjeto().getId())
                .orElseThrow(() -> new IllegalArgumentException("Nenhuma comarca foi encontrada para o projeto informado."));

        if (comarca.getOrdemServico() != null
                && !comarca.getOrdemServico().getId().equals(ordemServico.getId())) {
            throw new IllegalArgumentException("Esta comarca já possui uma OS vinculada.");
        }

        comarca.setOrdemServico(ordemServico);
        comarcaRepository.save(comarca);
        cadastrarMateriaisDaOs(comarca, materiaisPrevistos);
        Comarca comarcaComMateriais = comarcaRepository.findById(comarca.getId())
                .orElseThrow(() -> new IllegalArgumentException("Comarca não encontrada após criação da OS."));
        ordemRetiradaPort.criarParaOrdemServico(ordemServico, comarcaComMateriais, "Sistema");
    }

    private void cadastrarMateriaisDaOs(Comarca comarca,
            List<CriarOrdemServicoRequest.MaterialPrevistoRequest> materiaisPrevistos) {
        for (CriarOrdemServicoRequest.MaterialPrevistoRequest materialPrevisto : materiaisPrevistos) {
            if (materialPrevisto.getMaterialId() == null) {
                throw new IllegalArgumentException("Todos os materiais previstos precisam estar vinculados ao estoque.");
            }
            if (materialPrevisto.getQuantidadePrevista() == null || materialPrevisto.getQuantidadePrevista() <= 0) {
                throw new IllegalArgumentException("A quantidade prevista dos materiais deve ser maior que zero.");
            }

            Material material = materialRepository.findById(materialPrevisto.getMaterialId())
                    .orElseThrow(() -> new IllegalArgumentException("Material do estoque não encontrado."));
            comarcaService.adicionarMaterialPrevisto(
                    comarca.getId(),
                    material.getId(),
                    material.getNome(),
                    materialPrevisto.getQuantidadePrevista());
        }
    }

    private void validarDatasObrigatorias(LocalDateTime inicio, LocalDateTime fim, LocalDateTime deadline) {
        if (inicio == null) {
            throw new IllegalArgumentException("Data e Hora de Início é obrigatória.");
        }
        if (fim == null) {
            throw new IllegalArgumentException("Data e Hora de Fim é obrigatória.");
        }
        if (deadline == null) {
            throw new IllegalArgumentException("Prazo limite é obrigatório.");
        }
        if (fim.isBefore(inicio)) {
            throw new IllegalArgumentException("Data e Hora de Fim não pode ser anterior ao início.");
        }
    }

    private String gerarNumeroOs(Contrato contrato) {
        String numeroContrato = contrato.getContrato() != null && !contrato.getContrato().isBlank()
                ? contrato.getContrato().trim()
                : "Contrato " + contrato.getId();
        long sequencial = ordemServicoRepository.countByContratoId(contrato.getId()) + 1;
        String numeroOs;
        do {
            numeroOs = numeroContrato + " - OS " + String.format("%02d", sequencial);
            sequencial++;
        } while (ordemServicoRepository.existsByNumeroOs(numeroOs));
        return numeroOs;
    }

    private SincronizacaoMateriais sincronizarMateriaisPrevistos(Comarca comarca) {
        List<MaterialProjeto> materiaisProjeto = materialProjetoRepository.findByProjetoId(comarca.getProjeto().getId());
        Set<String> nomesJaSincronizados = comarca.getMateriais() == null
                ? new java.util.HashSet<>()
                : comarca.getMateriais().stream()
                        .map(MaterialItem::getNomeMaterial)
                        .filter(nome -> nome != null && !nome.isBlank())
                        .map(nome -> nome.toLowerCase())
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

interface OrdemRetiradaPort {
    OrdemRetirada criarParaOrdemServico(OrdemServico ordemServico, Comarca comarca, String geradoPor);
}
