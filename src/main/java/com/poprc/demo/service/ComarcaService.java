package com.poprc.demo.service;

import com.poprc.demo.exception.ArquivoInvalidoException;
import com.poprc.demo.exception.SaldoInsuficienteException;
import com.poprc.demo.model.Comarca;
import com.poprc.demo.model.Material;
import com.poprc.demo.model.MaterialItem;
import com.poprc.demo.model.MovimentacaoEstoque;
import com.poprc.demo.model.Projeto;
import com.poprc.demo.model.TipoMovimentacao;
import com.poprc.demo.repository.ComarcaRepository;
import com.poprc.demo.repository.MaterialItemRepository;
import com.poprc.demo.repository.MaterialRepository;
import com.poprc.demo.repository.MovimentacaoEstoqueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ComarcaService {

    private static final Set<String> EXTENSOES_FOTO_VISTORIA = Set.of("jpg", "jpeg", "png");
    private static final Set<String> MIME_FOTO_VISTORIA = Set.of("image/jpeg", "image/jpg", "image/png");
    private static final String DIRETORIO_FOTO_VISTORIA = "rc_uploads/comarcas/vistoria";
    private static final String DIRETORIO_PROVA_VIRADA_REDE = "rc_uploads/comarcas/virada-rede";
    private static final String AS_BUILT_PENDENTE = "PENDENTE";
    private static final String AS_BUILT_DIVERGENTE = "DIVERGENTE";
    private static final String AS_BUILT_HOMOLOGADO = "HOMOLOGADO";
    private static final String AS_BUILT_HOMOLOGADO_COM_DIVERGENCIA = "HOMOLOGADO_COM_DIVERGENCIA";
    private static final String AS_BUILT_REABERTO = "REABERTO_PARA_AJUSTE";

    private final ComarcaRepository comarcaRepository;
    private final MaterialItemRepository materialItemRepository;
    private final MaterialRepository materialRepository;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

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
    public Comarca salvarFotoVistoria(Long id, MultipartFile foto) {
        if (foto == null || foto.isEmpty()) {
            throw new ArquivoInvalidoException("A foto da vistoria é obrigatória.");
        }

        Comarca comarca = comarcaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Comarca não encontrada."));

        String fotoUrl = salvarArquivoFotoVistoria(foto);
        comarca.setFotoVistoriaUrl(fotoUrl);
        sincronizarPercentualVistoria(comarca);
        return comarcaRepository.save(comarca);
    }

    @Transactional
    public Comarca salvarAssinaturaVistoria(Long id, String assinaturaBase64) {
        if (assinaturaBase64 == null || assinaturaBase64.isBlank()) {
            throw new IllegalArgumentException("A assinatura da vistoria é obrigatória.");
        }
        if (!assinaturaBase64.startsWith("data:image/png;base64,")) {
            throw new IllegalArgumentException("A assinatura deve ser enviada em Base64 PNG.");
        }

        Comarca comarca = comarcaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Comarca não encontrada."));

        comarca.setAssinaturaBase64(assinaturaBase64);
        sincronizarPercentualVistoria(comarca);
        return comarcaRepository.save(comarca);
    }

    @Transactional
    public Comarca salvarProvaViradaRede(Long id, MultipartFile foto) {
        if (foto == null || foto.isEmpty()) {
            throw new ArquivoInvalidoException("A prova de funcionamento da Virada de Rede é obrigatória.");
        }

        Comarca comarca = comarcaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Comarca não encontrada."));

        comarca.setViradaRedeProvasFuncionamento(salvarArquivoImagem(foto, DIRETORIO_PROVA_VIRADA_REDE,
                "/uploads/comarcas/virada-rede/"));
        return comarcaRepository.save(comarca);
    }

    @Transactional
    public Comarca avancarParaInfraestrutura(Long id) {
        Comarca comarca = comarcaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Comarca não encontrada."));

        Integer etapaAtual = comarca.getEtapaAtual() != null ? comarca.getEtapaAtual() : 1;
        if (etapaAtual <= 1) {
            if (comarca.getFotoVistoriaUrl() == null || comarca.getFotoVistoriaUrl().isBlank()) {
                throw new IllegalArgumentException("Upload da foto da vistoria é obrigatório para liberar a infraestrutura.");
            }

            if (comarca.getAssinaturaBase64() == null || comarca.getAssinaturaBase64().isBlank()) {
                throw new IllegalArgumentException("Assinatura do responsável é obrigatória para liberar a infraestrutura.");
            }

            comarca.setEtapaAtual(2);
            comarca.setPercentualConcluido(BigDecimal.valueOf(70));
            comarca.setSituacao("INFRAESTRUTURA_LIBERADA");
            return comarcaRepository.save(comarca);
        }

        comarca.setEtapaAtual(3);
        comarca.setPercentualConcluido(BigDecimal.valueOf(Boolean.TRUE.equals(comarca.getViradaRedeConcluida()) ? 100 : 85));
        comarca.setSituacao("VIRADA_DE_REDE");
        return comarcaRepository.save(comarca);
    }

    @Transactional
    public Comarca salvarViradaRede(Long id, String provasFuncionamento, String checklist, Boolean concluida) {
        Comarca comarca = comarcaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Comarca não encontrada."));

        String provaAtual = provasFuncionamento != null && !provasFuncionamento.isBlank()
                ? provasFuncionamento
                : comarca.getViradaRedeProvasFuncionamento();
        if (Boolean.TRUE.equals(concluida)
                && (provaAtual == null || provaAtual.isBlank()
                        || checklist == null || checklist.isBlank())) {
            throw new IllegalArgumentException(
                    "Informe as provas de funcionamento e o checklist antes de concluir a Virada de Rede.");
        }

        comarca.setViradaRedeProvasFuncionamento(provaAtual);
        comarca.setViradaRedeChecklist(checklist);
        comarca.setViradaRedeConcluida(Boolean.TRUE.equals(concluida));
        if (Boolean.TRUE.equals(concluida)) {
            comarca.setEtapaAtual(3);
            comarca.setPercentualConcluido(BigDecimal.valueOf(100));
            comarca.setSituacao("VIRADA_DE_REDE_CONCLUIDA");
        } else if (comarca.getEtapaAtual() != null && comarca.getEtapaAtual() >= 3) {
            comarca.setPercentualConcluido(BigDecimal.valueOf(85));
            comarca.setSituacao("VIRADA_DE_REDE_EM_ANDAMENTO");
        }
        return comarcaRepository.save(comarca);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> buscarAuditoriaPorComarca(Long id) {
        Comarca comarca = comarcaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Comarca não encontrada."));
        return montarAuditoriaComarca(comarca);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> buscarAuditoriaPorNumeroOs(String numeroOs) {
        if (numeroOs == null || numeroOs.isBlank()) {
            throw new IllegalArgumentException("Número da OS é obrigatório.");
        }

        Comarca comarca = comarcaRepository.findByOrdemServicoNumeroOs(numeroOs)
                .orElseThrow(() -> new IllegalArgumentException("Comarca não encontrada para a OS informada."));
        return montarAuditoriaComarca(comarca);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> buscarRastreabilidadePorComarca(Long id) {
        Comarca comarca = comarcaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Comarca não encontrada."));
        return montarRastreabilidadeComarca(comarca);
    }

    @Transactional
    public Map<String, Object> atualizarQuantidadeAuditada(Long materialId, Integer quantidadeAuditada) {
        if (quantidadeAuditada == null || quantidadeAuditada < 0) {
            throw new IllegalArgumentException("A quantidade auditada deve ser maior ou igual a zero.");
        }

        MaterialItem material = materialItemRepository.findById(materialId)
                .orElseThrow(() -> new IllegalArgumentException("Material da comarca não encontrado."));
        if (Boolean.TRUE.equals(material.getEstoqueBaixado())) {
            throw new IllegalArgumentException("Material já baixado no estoque não pode ter a auditoria alterada.");
        }

        material.setQuantidadeAuditada(quantidadeAuditada);
        MaterialItem materialSalvo = materialItemRepository.save(material);
        sincronizarStatusAsBuilt(materialSalvo.getComarca());
        return montarMaterialAuditoria(materialSalvo);
    }

    @Transactional(readOnly = true)
    public List<MaterialItem> listarMateriaisPrevistos(Long comarcaId) {
        Comarca comarca = comarcaRepository.findById(comarcaId)
                .orElseThrow(() -> new IllegalArgumentException("Comarca não encontrada."));
        return materiaisDaComarca(comarca);
    }

    @Transactional
    public Comarca adicionarMaterialPrevisto(Long comarcaId, Long materialId, String nomeMaterial, Integer quantidadePrevista) {
        validarMaterialPrevisto(nomeMaterial, quantidadePrevista);

        Comarca comarca = comarcaRepository.findById(comarcaId)
                .orElseThrow(() -> new IllegalArgumentException("Comarca não encontrada."));
        Material materialEstoque = obterMaterialEstoque(materialId);
        validarSaldoReserva(materialEstoque, quantidadePrevista, 0);

        MaterialItem material = new MaterialItem();
        material.setComarca(comarca);
        material.setMaterial(materialEstoque);
        material.setNomeMaterial(materialEstoque != null ? materialEstoque.getNome() : nomeMaterial.trim());
        material.setQuantidadePrevista(quantidadePrevista);
        material.setQuantidadeAuditada(0);
        material.setEstoqueReservado(false);
        material.setEstoqueBaixado(false);
        material.setDataHoraSolicitacao(LocalDateTime.now());
        MaterialItem materialSalvo = materialItemRepository.save(material);
        reservarEstoque(comarca, materialSalvo, quantidadePrevista);

        comarca.setAsBuiltStatus(AS_BUILT_PENDENTE);
        sincronizarStatusAsBuilt(comarca);
        return recarregarComarca(comarcaId);
    }

    @Transactional
    public Comarca atualizarMaterialPrevisto(Long materialItemId, Long materialId, String nomeMaterial, Integer quantidadePrevista) {
        validarMaterialPrevisto(nomeMaterial, quantidadePrevista);

        MaterialItem material = materialItemRepository.findById(materialItemId)
                .orElseThrow(() -> new IllegalArgumentException("Material previsto não encontrado."));
        if (Boolean.TRUE.equals(material.getEstoqueBaixado())) {
            throw new IllegalArgumentException("Material já baixado no estoque não pode ser alterado.");
        }

        Comarca comarca = material.getComarca();
        liberarReservaEstoque(comarca, material, material.getQuantidadePrevista());

        Material materialEstoque = obterMaterialEstoque(materialId);
        validarSaldoReserva(materialEstoque, quantidadePrevista, 0);
        material.setMaterial(materialEstoque);
        material.setNomeMaterial(materialEstoque != null ? materialEstoque.getNome() : nomeMaterial.trim());
        material.setQuantidadePrevista(quantidadePrevista);
        material.setEstoqueReservado(false);
        MaterialItem materialSalvo = materialItemRepository.save(material);
        reservarEstoque(comarca, materialSalvo, quantidadePrevista);

        comarca.setAsBuiltStatus(AS_BUILT_PENDENTE);
        sincronizarStatusAsBuilt(comarca);
        return recarregarComarca(comarca.getId());
    }

    @Transactional
    public Comarca atualizarMateriaisFaltantes(Long comarcaId, Boolean faltouMaterial, List<Long> materialItemIds,
            String descricao) {
        Comarca comarca = comarcaRepository.findById(comarcaId)
                .orElseThrow(() -> new IllegalArgumentException("Comarca não encontrada."));

        boolean marcouFalta = Boolean.TRUE.equals(faltouMaterial);
        if (marcouFalta && (descricao == null || descricao.isBlank())) {
            throw new IllegalArgumentException("Descreva o que está faltando para registrar a pendência de material.");
        }

        List<MaterialItem> materiais = materiaisDaComarca(comarca);
        materiais.forEach(material -> {
            boolean selecionado = materialItemIds != null && materialItemIds.contains(material.getId());
            material.setMaterialFaltante(marcouFalta && selecionado);
            material.setDescricaoFaltante(marcouFalta && selecionado ? descricao : null);
            materialItemRepository.save(material);
        });

        comarca.setFaltouMaterial(marcouFalta);
        comarca.setDescricaoMaterialFaltante(marcouFalta ? descricao : null);
        if (marcouFalta) {
            comarca.setPendencias(descricao);
        }
        return comarcaRepository.save(comarca);
    }

    @Transactional
    public Comarca adicionarItemAdicional(Long comarcaId, Long materialId, String nomeMaterial, Integer quantidadePrevista) {
        validarMaterialPrevisto(nomeMaterial, quantidadePrevista);

        Comarca comarca = comarcaRepository.findById(comarcaId)
                .orElseThrow(() -> new IllegalArgumentException("Comarca não encontrada."));
        Material materialEstoque = obterMaterialEstoque(materialId);
        validarSaldoReserva(materialEstoque, quantidadePrevista, 0);

        MaterialItem material = new MaterialItem();
        material.setComarca(comarca);
        material.setMaterial(materialEstoque);
        material.setNomeMaterial(materialEstoque != null ? materialEstoque.getNome() : nomeMaterial.trim());
        material.setQuantidadePrevista(quantidadePrevista);
        material.setQuantidadeAuditada(0);
        material.setEstoqueReservado(false);
        material.setEstoqueBaixado(false);
        material.setItemAdicional(true);
        material.setDataHoraSolicitacao(LocalDateTime.now());
        MaterialItem materialSalvo = materialItemRepository.save(material);
        reservarEstoque(comarca, materialSalvo, quantidadePrevista);
        return recarregarComarca(comarcaId);
    }

    @Transactional
    public Comarca atualizarTimelineMaterial(Long materialItemId, LocalDateTime dataHoraSolicitacao,
            LocalDateTime dataHoraRetirada, LocalDateTime dataHoraUso) {
        MaterialItem material = materialItemRepository.findById(materialItemId)
                .orElseThrow(() -> new IllegalArgumentException("Material previsto não encontrado."));
        material.setDataHoraSolicitacao(dataHoraSolicitacao);
        material.setDataHoraRetirada(dataHoraRetirada);
        material.setDataHoraUso(dataHoraUso);
        MaterialItem materialSalvo = materialItemRepository.save(material);
        return recarregarComarca(materialSalvo.getComarca().getId());
    }

    @Transactional
    public Comarca removerMaterialPrevisto(Long materialId) {
        MaterialItem material = materialItemRepository.findById(materialId)
                .orElseThrow(() -> new IllegalArgumentException("Material previsto não encontrado."));
        if (Boolean.TRUE.equals(material.getEstoqueBaixado())) {
            throw new IllegalArgumentException("Material já baixado no estoque não pode ser removido.");
        }
        Comarca comarca = material.getComarca();
        Long comarcaId = comarca.getId();
        liberarReservaEstoque(comarca, material, material.getQuantidadePrevista());

        if (comarca.getMateriais() != null) {
            comarca.getMateriais().removeIf(item -> item.getId().equals(materialId));
        }
        materialItemRepository.delete(material);

        comarca.setAsBuiltStatus(AS_BUILT_PENDENTE);
        sincronizarStatusAsBuilt(comarca);
        return recarregarComarca(comarcaId);
    }

    @Transactional
    public Map<String, Object> homologarAsBuilt(Long id) {
        Comarca comarca = comarcaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Comarca não encontrada."));
        List<MaterialItem> materiais = materiaisDaComarca(comarca);

        if (materiais.isEmpty()) {
            throw new IllegalArgumentException("Não há materiais previstos para homologar o As-Built desta comarca.");
        }

        boolean conciliado = materiaisConciliados(materiais);
        validarMateriaisVinculadosAoEstoque(materiais);
        baixarEstoqueDaComarca(comarca, materiais);

        comarca.setAsBuiltStatus(conciliado ? AS_BUILT_HOMOLOGADO : AS_BUILT_HOMOLOGADO_COM_DIVERGENCIA);
        comarca.setSituacao(conciliado ? "AS_BUILT_HOMOLOGADO" : "AS_BUILT_HOMOLOGADO_COM_DIVERGENCIA");
        Comarca comarcaSalva = comarcaRepository.save(comarca);
        return montarAuditoriaComarca(comarcaSalva);
    }

    @Transactional
    public Map<String, Object> reabrirAsBuilt(Long id) {
        Comarca comarca = comarcaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Comarca não encontrada."));

        if (!AS_BUILT_HOMOLOGADO.equals(comarca.getAsBuiltStatus())
                && !AS_BUILT_HOMOLOGADO_COM_DIVERGENCIA.equals(comarca.getAsBuiltStatus())) {
            throw new IllegalArgumentException("Somente As-Built homologado pode ser reaberto para ajuste.");
        }

        List<MaterialItem> materiais = materiaisDaComarca(comarca);
        estornarBaixaDaComarca(comarca, materiais);

        comarca.setAsBuiltStatus(AS_BUILT_REABERTO);
        comarca.setSituacao("AS_BUILT_REABERTO_PARA_AJUSTE");
        Comarca comarcaSalva = comarcaRepository.save(comarca);
        return montarAuditoriaComarca(comarcaSalva);
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

    private Map<String, Object> montarAuditoriaComarca(Comarca comarca) {
        Map<String, Object> response = new HashMap<>();
        List<MaterialItem> materiais = materiaisDaComarca(comarca);
        boolean conciliado = materiaisConciliados(materiais);

        response.put("comarcaId", comarca.getId());
        response.put("nomeComarca", comarca.getNomeComarca());
        response.put("numeroOs", comarca.getOrdemServico() != null ? comarca.getOrdemServico().getNumeroOs() : null);
        response.put("etapaAtual", comarca.getEtapaAtual());
        response.put("conciliado", conciliado);
        response.put("asBuiltStatus", resolverStatusAsBuilt(comarca, conciliado, !materiais.isEmpty()));
        response.put("materiais", materiais.stream().map(this::montarMaterialAuditoria).toList());

        return response;
    }

    private Map<String, Object> montarMaterialAuditoria(MaterialItem material) {
        Map<String, Object> materialMap = new HashMap<>();
        materialMap.put("id", material.getId());
        materialMap.put("nome", material.getNomeMaterial());
        materialMap.put("previsto", material.getQuantidadePrevista());
        materialMap.put("utilizado", material.getQuantidadeAuditada());
        materialMap.put("estoqueReservado", Boolean.TRUE.equals(material.getEstoqueReservado()));
        materialMap.put("estoqueBaixado", Boolean.TRUE.equals(material.getEstoqueBaixado()));
        materialMap.put("materialFaltante", Boolean.TRUE.equals(material.getMaterialFaltante()));
        materialMap.put("itemAdicional", Boolean.TRUE.equals(material.getItemAdicional()));
        materialMap.put("descricaoFaltante", material.getDescricaoFaltante());
        materialMap.put("dataHoraSolicitacao", material.getDataHoraSolicitacao());
        materialMap.put("dataHoraRetirada", material.getDataHoraRetirada());
        materialMap.put("dataHoraUso", material.getDataHoraUso());
        if (material.getMaterial() != null) {
            materialMap.put("materialId", material.getMaterial().getId());
            materialMap.put("partNumber", material.getMaterial().getPartNumber());
            materialMap.put("estoqueDisponivel", material.getMaterial().getQuantidadeDisponivel());
            materialMap.put("estoqueReservadoTotal", quantidadeReservada(material.getMaterial()));
            materialMap.put("estoqueLivre", saldoLivreParaPlanejamento(material.getMaterial()));
        }
        return materialMap;
    }

    private Map<String, Object> montarRastreabilidadeComarca(Comarca comarca) {
        Map<String, Object> response = new HashMap<>();
        List<MaterialItem> materiais = materiaisDaComarca(comarca);
        List<MovimentacaoEstoque> movimentacoes = movimentacaoEstoqueRepository
                .findByComarcaIdOrderByDataMovimentacaoDesc(comarca.getId());

        List<Map<String, Object>> itens = materiais.stream()
                .map(item -> montarItemRastreabilidade(item, movimentacoes))
                .toList();

        int totalPrevisto = somarCampo(itens, "previsto");
        int totalReservado = somarCampo(itens, "reservado");
        int totalAuditado = somarCampo(itens, "auditado");
        int totalBaixado = somarCampo(itens, "baixado");

        response.put("comarcaId", comarca.getId());
        response.put("nomeComarca", comarca.getNomeComarca());
        response.put("numeroOs", comarca.getOrdemServico() != null ? comarca.getOrdemServico().getNumeroOs() : null);
        response.put("projetoId", resolverProjetoDaComarca(comarca) != null ? resolverProjetoDaComarca(comarca).getId() : null);
        response.put("asBuiltStatus", comarca.getAsBuiltStatus());
        response.put("totais", Map.of(
                "previsto", totalPrevisto,
                "reservado", totalReservado,
                "auditado", totalAuditado,
                "baixado", totalBaixado,
                "saldoPrevistoBaixado", totalPrevisto - totalBaixado,
                "saldoAuditadoBaixado", totalAuditado - totalBaixado));
        response.put("itens", itens);
        response.put("movimentacoes", movimentacoes.stream().map(this::montarMovimentacaoRastreabilidade).toList());
        return response;
    }

    private Map<String, Object> montarItemRastreabilidade(MaterialItem item, List<MovimentacaoEstoque> movimentacoes) {
        List<MovimentacaoEstoque> movimentacoesDoMaterial = movimentacoes.stream()
                .filter(movimentacao -> movimentacao.getMaterial() != null
                        && item.getMaterial() != null
                        && movimentacao.getMaterial().getId().equals(item.getMaterial().getId()))
                .toList();

        int baixado = movimentacoesDoMaterial.stream()
                .filter(movimentacao -> TipoMovimentacao.BAIXA.equals(movimentacao.getTipo())
                        || TipoMovimentacao.SAIDA.equals(movimentacao.getTipo())
                        || TipoMovimentacao.ESTORNO_BAIXA.equals(movimentacao.getTipo()))
                .mapToInt(movimentacao -> TipoMovimentacao.ESTORNO_BAIXA.equals(movimentacao.getTipo())
                        ? -quantidadeMovimentacao(movimentacao)
                        : quantidadeMovimentacao(movimentacao))
                .sum();
        int reservado = Boolean.TRUE.equals(item.getEstoqueReservado()) && !Boolean.TRUE.equals(item.getEstoqueBaixado())
                ? item.getQuantidadePrevista()
                : 0;
        int previsto = item.getQuantidadePrevista();
        int auditado = item.getQuantidadeAuditada();

        Map<String, Object> itemMap = new HashMap<>();
        itemMap.put("materialItemId", item.getId());
        itemMap.put("materialId", item.getMaterial() != null ? item.getMaterial().getId() : null);
        itemMap.put("nome", item.getNomeMaterial());
        itemMap.put("partNumber", item.getMaterial() != null ? item.getMaterial().getPartNumber() : null);
        itemMap.put("previsto", previsto);
        itemMap.put("reservado", reservado);
        itemMap.put("auditado", auditado);
        itemMap.put("baixado", baixado);
        itemMap.put("saldoPrevistoBaixado", previsto - baixado);
        itemMap.put("saldoAuditadoBaixado", auditado - baixado);
        itemMap.put("estoqueBaixado", Boolean.TRUE.equals(item.getEstoqueBaixado()));
        itemMap.put("materialFaltante", Boolean.TRUE.equals(item.getMaterialFaltante()));
        itemMap.put("itemAdicional", Boolean.TRUE.equals(item.getItemAdicional()));
        itemMap.put("descricaoFaltante", item.getDescricaoFaltante());
        itemMap.put("dataHoraSolicitacao", item.getDataHoraSolicitacao());
        itemMap.put("dataHoraRetirada", item.getDataHoraRetirada());
        itemMap.put("dataHoraUso", item.getDataHoraUso());
        itemMap.put("movimentacoes", movimentacoesDoMaterial.stream()
                .map(this::montarMovimentacaoRastreabilidade)
                .collect(Collectors.toList()));
        return itemMap;
    }

    private Map<String, Object> montarMovimentacaoRastreabilidade(MovimentacaoEstoque movimentacao) {
        Map<String, Object> movimentacaoMap = new HashMap<>();
        movimentacaoMap.put("id", movimentacao.getId());
        movimentacaoMap.put("tipo", movimentacao.getTipo());
        movimentacaoMap.put("quantidade", movimentacao.getQuantidade());
        movimentacaoMap.put("dataMovimentacao", movimentacao.getDataMovimentacao());
        movimentacaoMap.put("observacao", movimentacao.getObservacao());
        movimentacaoMap.put("materialId", movimentacao.getMaterial() != null ? movimentacao.getMaterial().getId() : null);
        movimentacaoMap.put("materialNome", movimentacao.getMaterial() != null ? movimentacao.getMaterial().getNome() : null);
        return movimentacaoMap;
    }

    private int somarCampo(List<Map<String, Object>> itens, String campo) {
        return itens.stream()
                .map(item -> item.get(campo))
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .mapToInt(Number::intValue)
                .sum();
    }

    private void validarMaterialPrevisto(String nomeMaterial, Integer quantidadePrevista) {
        if (nomeMaterial == null || nomeMaterial.isBlank()) {
            throw new IllegalArgumentException("Nome do material é obrigatório.");
        }
        if (quantidadePrevista == null || quantidadePrevista <= 0) {
            throw new IllegalArgumentException("A quantidade prevista deve ser maior que zero.");
        }
    }

    private Material obterMaterialEstoque(Long materialId) {
        if (materialId == null) {
            throw new IllegalArgumentException("Selecione um material cadastrado no estoque.");
        }

        return materialRepository.findById(materialId)
                .orElseThrow(() -> new IllegalArgumentException("Material do estoque não encontrado."));
    }

    private void validarSaldoReserva(Material material, Integer quantidadePrevista, int creditoReservaAtual) {
        int saldoLivre = saldoLivreParaPlanejamento(material) + creditoReservaAtual;
        if (quantidadePrevista > saldoLivre) {
            throw new SaldoInsuficienteException(
                    "Estoque insuficiente para " + material.getNome() + ". Disponível: "
                            + saldoLivre + ", previsto: " + quantidadePrevista + ".");
        }
    }

    private void reservarEstoque(Comarca comarca, MaterialItem item, int quantidade) {
        Material material = item.getMaterial();
        material.setQuantidadeReservada(quantidadeReservada(material) + quantidade);
        materialRepository.save(material);

        registrarMovimentacaoEstoque(comarca, material, quantidade, TipoMovimentacao.RESERVA,
                montarObservacaoReserva(comarca, item));

        item.setEstoqueReservado(true);
        materialItemRepository.save(item);
    }

    private void liberarReservaEstoque(Comarca comarca, MaterialItem item, int quantidade) {
        if (!Boolean.TRUE.equals(item.getEstoqueReservado()) || item.getMaterial() == null) {
            return;
        }

        Material material = item.getMaterial();
        material.setQuantidadeReservada(Math.max(0, quantidadeReservada(material) - quantidade));
        materialRepository.save(material);

        registrarMovimentacaoEstoque(comarca, material, quantidade, TipoMovimentacao.ESTORNO_RESERVA,
                montarObservacaoReserva(comarca, item));

        item.setEstoqueReservado(false);
        materialItemRepository.save(item);
    }

    private void registrarMovimentacaoEstoque(Comarca comarca, Material material, int quantidade, TipoMovimentacao tipo,
            String observacao) {
        MovimentacaoEstoque movimentacao = new MovimentacaoEstoque();
        movimentacao.setMaterial(material);
        movimentacao.setQuantidade(quantidade);
        movimentacao.setTipo(tipo);
        movimentacao.setDataMovimentacao(LocalDateTime.now());
        movimentacao.setObservacao(observacao);
        movimentacao.setComarca(comarca);
        movimentacao.setOrdemServico(comarca.getOrdemServico());
        movimentacao.setProjeto(resolverProjetoDaComarca(comarca));
        movimentacaoEstoqueRepository.save(movimentacao);
    }

    private void validarMateriaisVinculadosAoEstoque(List<MaterialItem> materiais) {
        materiais.stream()
                .filter(material -> material.getMaterial() == null)
                .findFirst()
                .ifPresent(material -> {
                    throw new IllegalArgumentException(
                            "O material previsto \"" + material.getNomeMaterial()
                                    + "\" precisa estar vinculado ao estoque antes da homologação.");
                });
    }

    private void baixarEstoqueDaComarca(Comarca comarca, List<MaterialItem> materiais) {
        for (MaterialItem item : materiais) {
            if (Boolean.TRUE.equals(item.getEstoqueBaixado())) {
                continue;
            }

            Material material = item.getMaterial();
            int quantidadeBaixa = item.getQuantidadeAuditada();
            int disponivel = material.getQuantidadeDisponivel() != null ? material.getQuantidadeDisponivel() : 0;
            if (quantidadeBaixa > disponivel) {
                throw new SaldoInsuficienteException(
                        "Estoque insuficiente para baixar " + material.getNome() + ". Disponível: "
                                + disponivel + ", auditado: " + quantidadeBaixa + ".");
            }

            material.setQuantidadeDisponivel(disponivel - quantidadeBaixa);
            if (Boolean.TRUE.equals(item.getEstoqueReservado())) {
                material.setQuantidadeReservada(Math.max(0, quantidadeReservada(material) - item.getQuantidadePrevista()));
            }
            materialRepository.save(material);

            registrarMovimentacaoEstoque(comarca, material, quantidadeBaixa, TipoMovimentacao.BAIXA,
                    montarObservacaoBaixa(comarca, item));

            item.setEstoqueReservado(false);
            item.setEstoqueBaixado(true);
            if (item.getDataHoraRetirada() == null) {
                item.setDataHoraRetirada(LocalDateTime.now());
            }
            if (item.getDataHoraUso() == null) {
                item.setDataHoraUso(LocalDateTime.now());
            }
            materialItemRepository.save(item);
        }
    }

    private void estornarBaixaDaComarca(Comarca comarca, List<MaterialItem> materiais) {
        for (MaterialItem item : materiais) {
            if (!Boolean.TRUE.equals(item.getEstoqueBaixado())) {
                continue;
            }

            Material material = item.getMaterial();
            if (material == null) {
                continue;
            }

            int quantidadeAuditada = item.getQuantidadeAuditada();
            int disponivel = material.getQuantidadeDisponivel() != null ? material.getQuantidadeDisponivel() : 0;
            material.setQuantidadeDisponivel(disponivel + quantidadeAuditada);
            material.setQuantidadeReservada(quantidadeReservada(material) + item.getQuantidadePrevista());
            materialRepository.save(material);

            registrarMovimentacaoEstoque(comarca, material, quantidadeAuditada, TipoMovimentacao.ESTORNO_BAIXA,
                    montarObservacaoEstornoBaixa(comarca, item));

            item.setEstoqueBaixado(false);
            item.setEstoqueReservado(true);
            materialItemRepository.save(item);
        }
    }

    private String montarObservacaoBaixa(Comarca comarca, MaterialItem item) {
        String numeroOs = comarca.getOrdemServico() != null ? comarca.getOrdemServico().getNumeroOs() : "OS não vinculada";
        return "Baixa automática As-Built | " + numeroOs + " | " + comarca.getNomeComarca()
                + " | Material previsto #" + item.getId();
    }

    private String montarObservacaoEstornoBaixa(Comarca comarca, MaterialItem item) {
        String numeroOs = comarca.getOrdemServico() != null ? comarca.getOrdemServico().getNumeroOs() : "OS não vinculada";
        return "Reabertura As-Built - estorno da baixa | " + numeroOs + " | " + comarca.getNomeComarca()
                + " | Material previsto #" + item.getId();
    }

    private String montarObservacaoReserva(Comarca comarca, MaterialItem item) {
        String numeroOs = comarca.getOrdemServico() != null ? comarca.getOrdemServico().getNumeroOs() : "OS não vinculada";
        return "Reserva de material previsto | " + numeroOs + " | " + comarca.getNomeComarca()
                + " | Material previsto #" + item.getId();
    }

    private Projeto resolverProjetoDaComarca(Comarca comarca) {
        if (comarca.getProjeto() != null) {
            return comarca.getProjeto();
        }
        if (comarca.getOrdemServico() != null) {
            return comarca.getOrdemServico().getProjeto();
        }
        return null;
    }

    private int quantidadeReservada(Material material) {
        return material.getQuantidadeReservada() != null ? material.getQuantidadeReservada() : 0;
    }

    private int saldoLivreParaPlanejamento(Material material) {
        int disponivel = material.getQuantidadeDisponivel() != null ? material.getQuantidadeDisponivel() : 0;
        return Math.max(0, disponivel - quantidadeReservada(material));
    }

    private int quantidadeMovimentacao(MovimentacaoEstoque movimentacao) {
        return movimentacao.getQuantidade() != null ? movimentacao.getQuantidade() : 0;
    }

    private Comarca recarregarComarca(Long comarcaId) {
        return comarcaRepository.findById(comarcaId)
                .orElseThrow(() -> new IllegalArgumentException("Comarca não encontrada."));
    }

    private void sincronizarStatusAsBuilt(Comarca comarca) {
        if (comarca == null) {
            return;
        }

        List<MaterialItem> materiais = materiaisDaComarca(comarca);
        boolean conciliado = materiaisConciliados(materiais);
        if (materiais.isEmpty()) {
            comarca.setAsBuiltStatus(AS_BUILT_PENDENTE);
        } else if (!conciliado) {
            comarca.setAsBuiltStatus(AS_BUILT_DIVERGENTE);
        } else if (!AS_BUILT_HOMOLOGADO.equals(comarca.getAsBuiltStatus())) {
            comarca.setAsBuiltStatus(AS_BUILT_PENDENTE);
        }
        comarcaRepository.save(comarca);
    }

    private String resolverStatusAsBuilt(Comarca comarca, boolean conciliado, boolean temMateriais) {
        if (!temMateriais) {
            return AS_BUILT_PENDENTE;
        }
        if (AS_BUILT_REABERTO.equals(comarca.getAsBuiltStatus())) {
            return AS_BUILT_REABERTO;
        }
        if (AS_BUILT_HOMOLOGADO.equals(comarca.getAsBuiltStatus()) && conciliado) {
            return AS_BUILT_HOMOLOGADO;
        }
        if (AS_BUILT_HOMOLOGADO_COM_DIVERGENCIA.equals(comarca.getAsBuiltStatus())) {
            return AS_BUILT_HOMOLOGADO_COM_DIVERGENCIA;
        }
        if (!conciliado) {
            return AS_BUILT_DIVERGENTE;
        }
        return AS_BUILT_PENDENTE;
    }

    private boolean materiaisConciliados(List<MaterialItem> materiais) {
        return !materiais.isEmpty()
                && materiais.stream().allMatch(mat -> mat.getQuantidadePrevista() == mat.getQuantidadeAuditada());
    }

    private List<MaterialItem> materiaisDaComarca(Comarca comarca) {
        return comarca.getMateriais() != null ? comarca.getMateriais() : List.of();
    }

    private String salvarArquivoFotoVistoria(MultipartFile foto) {
        return salvarArquivoImagem(foto, DIRETORIO_FOTO_VISTORIA, "/uploads/comarcas/vistoria/");
    }

    private String salvarArquivoImagem(MultipartFile foto, String diretorio, String urlBase) {
        String extensao = extrairExtensao(foto.getOriginalFilename());

        if (!EXTENSOES_FOTO_VISTORIA.contains(extensao.toLowerCase())
                || !MIME_FOTO_VISTORIA.contains(foto.getContentType())) {
            throw new ArquivoInvalidoException("Formato de arquivo não permitido. Envie apenas imagens .jpg, .jpeg ou .png.");
        }

        Path pastaDestino = Paths.get(System.getProperty("user.home"), diretorio);
        try {
            Files.createDirectories(pastaDestino);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao preparar diretório de upload da vistoria.", e);
        }

        String nomeUnico = UUID.randomUUID() + "." + extensao.toLowerCase();
        Path caminhoAbsoluto = pastaDestino.resolve(nomeUnico);

        try {
            foto.transferTo(caminhoAbsoluto.toFile());
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar foto da vistoria no servidor.", e);
        }

        return urlBase + nomeUnico;
    }

    private void sincronizarPercentualVistoria(Comarca comarca) {
        int percentual = 0;

        if (comarca.getFotoVistoriaUrl() != null && !comarca.getFotoVistoriaUrl().isBlank()) {
            percentual += 50;
        }

        if (comarca.getAssinaturaBase64() != null && !comarca.getAssinaturaBase64().isBlank()) {
            percentual += 50;
        }

        comarca.setPercentualConcluido(BigDecimal.valueOf(percentual));

        if (percentual == 100 && (comarca.getEtapaAtual() == null || comarca.getEtapaAtual() == 1)) {
            comarca.setSituacao("VISTORIA_CONCLUIDA");
        } else if (percentual > 0 && (comarca.getEtapaAtual() == null || comarca.getEtapaAtual() == 1)) {
            comarca.setSituacao("VISTORIA_EM_ANDAMENTO");
        }
    }

    private String extrairExtensao(String nomeArquivo) {
        if (nomeArquivo == null || !nomeArquivo.contains(".")) {
            throw new ArquivoInvalidoException("Arquivo enviado sem extensão válida.");
        }
        return nomeArquivo.substring(nomeArquivo.lastIndexOf(".") + 1);
    }
}
