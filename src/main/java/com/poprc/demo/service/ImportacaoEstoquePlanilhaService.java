package com.poprc.demo.service;

import com.poprc.demo.dto.ImportacaoEstoquePlanilhaRequest;
import com.poprc.demo.dto.ImportacaoEstoquePlanilhaDetalheDTO;
import com.poprc.demo.dto.ImportacaoEstoquePlanilhaResultadoDTO;
import com.poprc.demo.model.Comarca;
import com.poprc.demo.model.ImportacaoEstoqueItemPlanilha;
import com.poprc.demo.model.ImportacaoEstoquePlanilha;
import com.poprc.demo.model.ImportacaoRetiradaPlanilha;
import com.poprc.demo.model.LocalEstoque;
import com.poprc.demo.model.Material;
import com.poprc.demo.model.TipoControleEstoque;
import com.poprc.demo.model.UnidadeMedida;
import com.poprc.demo.repository.ComarcaRepository;
import com.poprc.demo.repository.ImportacaoEstoqueItemPlanilhaRepository;
import com.poprc.demo.repository.ImportacaoEstoquePlanilhaRepository;
import com.poprc.demo.repository.ImportacaoRetiradaPlanilhaRepository;
import com.poprc.demo.repository.LocalEstoqueRepository;
import com.poprc.demo.repository.MaterialRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ImportacaoEstoquePlanilhaService {

    private final ImportacaoEstoquePlanilhaRepository importacaoRepository;
    private final ImportacaoEstoqueItemPlanilhaRepository itemImportacaoRepository;
    private final ImportacaoRetiradaPlanilhaRepository retiradaImportacaoRepository;
    private final MaterialRepository materialRepository;
    private final LocalEstoqueRepository localEstoqueRepository;
    private final ComarcaRepository comarcaRepository;
    private final EstoqueService estoqueService;

    @Transactional
    public ImportacaoEstoquePlanilhaResultadoDTO importar(
            ImportacaoEstoquePlanilhaRequest request, String usuario) {
        validarRequest(request);
        validarConteudoCompleto(request);
        String hash = request.hashSha256().trim().toLowerCase(Locale.ROOT);
        String responsavel = usuario != null && !usuario.isBlank() ? usuario : "Usuário autenticado";
        ImportacaoEstoquePlanilha importacaoExistente =
                importacaoRepository.findByHashSha256(hash).orElse(null);
        boolean complementacao = importacaoExistente != null;
        LocalEstoque local;
        ImportacaoEstoquePlanilha importacao;
        if (importacaoExistente != null) {
            if (request.retiradas() == null || request.retiradas().isEmpty()) {
                throw new IllegalArgumentException(
                        "Esta planilha já foi importada. Nenhum saldo foi alterado.");
            }
            if (retiradaImportacaoRepository.existsByImportacaoId(importacaoExistente.getId())) {
                throw new IllegalArgumentException(
                        "O estoque e as retiradas desta planilha já foram importados.");
            }
            local = importacaoExistente.getLocalEstoque();
            if (local == null) {
                throw new IllegalStateException(
                        "A importação existente não possui um depósito de destino válido.");
            }
            if (!local.getId().equals(request.localEstoqueId())) {
                throw new IllegalArgumentException(
                        "A complementação deve usar o mesmo depósito da importação original: "
                                + local.getNome() + ".");
            }
            validarInventarioBaseDaComplementacao(importacaoExistente, request.itens());
            importacao = importacaoExistente;
        } else {
            local = localEstoqueRepository.findById(request.localEstoqueId())
                    .filter(item -> !Boolean.FALSE.equals(item.getAtivo()))
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Depósito de destino não encontrado ou inativo."));
            importacao = novaImportacao(request, hash, responsavel, local);
        }

        Map<String, List<Material>> materiaisPorNome = materialRepository.findAll().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        material -> normalizar(material.getNome())));
        Set<String> nomesRecebidos = new HashSet<>();

        int criados = 0;
        int atualizados = 0;
        int positivos = 0;
        int negativos = 0;
        BigDecimal valorTotal = BigDecimal.ZERO;
        Map<String, Integer> sequenciaisPartNumber = new HashMap<>();
        boolean registrarItensHistorico =
                !itemImportacaoRepository.existsByImportacaoId(importacao.getId());

        for (ImportacaoEstoquePlanilhaRequest.ItemImportacao item : request.itens()) {
            validarItem(item);
            String nomeNormalizado = normalizar(item.nome());
            if (!nomesRecebidos.add(nomeNormalizado)) {
                throw new IllegalArgumentException(
                        "A planilha contém o material duplicado: " + item.nome().trim() + ".");
            }

            List<Material> correspondencias = materiaisPorNome.getOrDefault(nomeNormalizado, List.of());
            if (correspondencias.size() > 1) {
                throw new IllegalArgumentException(
                        "Existem materiais duplicados no sistema com o nome " + item.nome().trim()
                                + ". Corrija o cadastro antes de importar.");
            }

            Material material;
            String acao;
            if (correspondencias.isEmpty()) {
                if (complementacao) {
                    throw new IllegalArgumentException(
                            "O material " + item.nome().trim()
                                    + " não foi encontrado para complementar a importação.");
                }
                material = novoMaterial(item, local, sequenciaisPartNumber);
                material = estoqueService.cadastrarMaterial(material);
                materiaisPorNome.put(nomeNormalizado, List.of(material));
                criados++;
                acao = "CRIADO";
            } else {
                material = correspondencias.getFirst();
                if (TipoControleEstoque.METRAGEM.equals(material.getTipoControle())
                        || TipoControleEstoque.BOBINA.equals(material.getTipoControle())
                        || TipoControleEstoque.ROLO.equals(material.getTipoControle())) {
                    throw new IllegalArgumentException(
                            "O material " + material.getNome()
                                    + " usa controle por metragem/bobina e não pode receber quantidade unitária da planilha.");
                }
                if (!complementacao) {
                    atualizados++;
                }
                acao = complementacao ? "REGISTRO_COMPLEMENTAR" : "ATUALIZADO";
            }

            int saldoAnterior = material.getQuantidadeDisponivel() != null
                    ? material.getQuantidadeDisponivel()
                    : 0;
            int saldoDesejado = item.quantidade();
            if (!complementacao) {
                estoqueService.reconciliarSaldoPlanilha(
                        material.getId(),
                        local.getId(),
                        saldoDesejado,
                        item.custoUnitario(),
                        "Inventário importado de " + importacao.getNomeArquivo(),
                        responsavel);
                if (saldoDesejado > saldoAnterior) {
                    positivos++;
                } else if (saldoDesejado < saldoAnterior) {
                    negativos++;
                } else {
                    acao = "SEM_ALTERACAO";
                }
            }
            if (registrarItensHistorico) {
                registrarItemImportado(importacao, material, item, saldoAnterior, saldoDesejado, acao);
            }
            valorTotal = valorTotal.add(
                    item.custoUnitario().multiply(BigDecimal.valueOf(saldoDesejado)));
        }

        if (!complementacao) {
            importacao.setItensProcessados(request.itens().size());
            importacao.setMateriaisCriados(criados);
            importacao.setMateriaisAtualizados(atualizados);
            importacao.setAjustesPositivos(positivos);
            importacao.setAjustesNegativos(negativos);
            importacao.setValorTotalImportado(valorTotal.setScale(2, RoundingMode.HALF_UP));
        } else {
            importacao.setDataComplementacao(LocalDateTime.now());
            importacao.setComplementadoPor(limitar(responsavel, 255));
        }

        ResultadoRetiradas resultadoRetiradas =
                registrarRetiradas(
                        importacao,
                        request.retiradas(),
                        materiaisPorNome,
                        local,
                        responsavel);
        importacao.setAbasRetiradaProcessadas(resultadoRetiradas.abas());
        importacao.setRetiradasImportadas(resultadoRetiradas.retiradas());
        importacao.setFaltasIdentificadas(resultadoRetiradas.faltas());
        importacao.setAjustesNegativos(
                importacao.getAjustesNegativos() + resultadoRetiradas.ajustesSaldo());
        importacaoRepository.save(importacao);

        return new ImportacaoEstoquePlanilhaResultadoDTO(
                importacao.getId(),
                importacao.getNomeArquivo(),
                importacao.getDataImportacao(),
                importacao.getImportadoPor(),
                local.getNome(),
                importacao.getItensProcessados(),
                importacao.getMateriaisCriados(),
                importacao.getMateriaisAtualizados(),
                importacao.getAjustesPositivos(),
                importacao.getAjustesNegativos(),
                importacao.getValorTotalImportado(),
                importacao.getAbasRetiradaProcessadas(),
                importacao.getRetiradasImportadas(),
                importacao.getFaltasIdentificadas(),
                complementacao ? "COMPLEMENTADA" : "IMPORTADA");
    }

    @Transactional(readOnly = true)
    public List<ImportacaoEstoquePlanilhaDetalheDTO> listarHistorico() {
        return importacaoRepository.findAllByOrderByDataImportacaoDesc().stream()
                .map(importacao -> mapearDetalhe(importacao, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public ImportacaoEstoquePlanilhaDetalheDTO detalhar(Long id) {
        ImportacaoEstoquePlanilha importacao = importacaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Importação de estoque não encontrada."));
        return mapearDetalhe(importacao, true);
    }

    @Transactional(readOnly = true)
    public List<ImportacaoEstoquePlanilhaDetalheDTO.Retirada> listarRetiradasImportadas() {
        return retiradaImportacaoRepository.findAllByOrderByImportacaoDataImportacaoDescAbaOrigemAsc()
                .stream()
                .map(this::mapearRetirada)
                .toList();
    }

    private ImportacaoEstoquePlanilha novaImportacao(
            ImportacaoEstoquePlanilhaRequest request,
            String hash,
            String responsavel,
            LocalEstoque local) {
        ImportacaoEstoquePlanilha importacao = new ImportacaoEstoquePlanilha();
        importacao.setNomeArquivo(limitar(request.nomeArquivo().trim(), 255));
        importacao.setHashSha256(hash);
        importacao.setDataImportacao(LocalDateTime.now());
        importacao.setImportadoPor(limitar(responsavel, 255));
        importacao.setLocalEstoque(local);
        return importacaoRepository.saveAndFlush(importacao);
    }

    private void registrarItemImportado(
            ImportacaoEstoquePlanilha importacao,
            Material material,
            ImportacaoEstoquePlanilhaRequest.ItemImportacao item,
            int saldoAnterior,
            int saldoDesejado,
            String acao) {
        ImportacaoEstoqueItemPlanilha registro = new ImportacaoEstoqueItemPlanilha();
        registro.setImportacao(importacao);
        registro.setMaterial(material);
        registro.setNomePlanilha(limitar(item.nome().trim(), 255));
        registro.setSaldoAnterior(saldoAnterior);
        registro.setSaldoImportado(saldoDesejado);
        registro.setCustoUnitario(item.custoUnitario());
        registro.setAcao(acao);
        itemImportacaoRepository.save(registro);
    }

    private ResultadoRetiradas registrarRetiradas(
            ImportacaoEstoquePlanilha importacao,
            List<ImportacaoEstoquePlanilhaRequest.RetiradaImportacao> retiradas,
            Map<String, List<Material>> materiaisPorNome,
            LocalEstoque local,
            String responsavel) {
        if (retiradas == null || retiradas.isEmpty()) {
            return new ResultadoRetiradas(0, 0, 0, 0);
        }
        Map<String, Long> comarcaPorAba = new LinkedHashMap<>();
        Map<Long, Comarca> comarcas = new HashMap<>();
        Map<String, ImportacaoEstoquePlanilhaRequest.RetiradaImportacao> saldoFinalPorMaterial =
                new LinkedHashMap<>();
        int processadas = 0;
        int faltas = 0;
        for (ImportacaoEstoquePlanilhaRequest.RetiradaImportacao retirada : retiradas) {
            validarRetirada(retirada);
            String aba = retirada.aba().trim();
            Long comarcaAnterior = comarcaPorAba.putIfAbsent(normalizar(aba), retirada.comarcaId());
            if (comarcaAnterior != null && !comarcaAnterior.equals(retirada.comarcaId())) {
                throw new IllegalArgumentException(
                        "Todos os itens da aba " + aba + " devem estar vinculados à mesma obra.");
            }
            Comarca comarca = comarcas.computeIfAbsent(
                    retirada.comarcaId(),
                    id -> comarcaRepository.findById(id)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Obra vinculada à aba " + aba + " não foi encontrada.")));
            List<Material> correspondencias =
                    materiaisPorNome.getOrDefault(normalizar(retirada.nomeMaterial()), List.of());
            if (correspondencias.size() != 1) {
                throw new IllegalArgumentException(
                        "Não foi possível identificar unicamente o material "
                                + retirada.nomeMaterial() + " da aba " + aba + ".");
            }
            BigDecimal faltante = retirada.saldoFinal().signum() < 0
                    ? retirada.saldoFinal().abs()
                    : BigDecimal.ZERO;
            ImportacaoRetiradaPlanilha registro = new ImportacaoRetiradaPlanilha();
            registro.setImportacao(importacao);
            registro.setComarca(comarca);
            registro.setMaterial(correspondencias.getFirst());
            registro.setAbaOrigem(limitar(aba, 255));
            registro.setSaldoInicial(retirada.saldoInicial());
            registro.setQuantidadeRetirada(retirada.quantidadeRetirada());
            registro.setSaldoFinal(retirada.saldoFinal());
            registro.setQuantidadeFaltante(faltante);
            registro.setCustoUnitario(retirada.custoUnitario());
            registro.setDataRetirada(retirada.dataRetirada());
            retiradaImportacaoRepository.save(registro);
            processadas++;
            if (faltante.signum() > 0) {
                faltas++;
            }
            saldoFinalPorMaterial.put(normalizar(retirada.nomeMaterial()), retirada);
        }
        int ajustesSaldo = reconciliarSaldosFinais(
                importacao,
                saldoFinalPorMaterial,
                materiaisPorNome,
                local,
                responsavel);
        return new ResultadoRetiradas(comarcaPorAba.size(), processadas, faltas, ajustesSaldo);
    }

    private int reconciliarSaldosFinais(
            ImportacaoEstoquePlanilha importacao,
            Map<String, ImportacaoEstoquePlanilhaRequest.RetiradaImportacao> saldoFinalPorMaterial,
            Map<String, List<Material>> materiaisPorNome,
            LocalEstoque local,
            String responsavel) {
        int ajustes = 0;
        for (Map.Entry<String, ImportacaoEstoquePlanilhaRequest.RetiradaImportacao> entry
                : saldoFinalPorMaterial.entrySet()) {
            Material material = materiaisPorNome.get(entry.getKey()).getFirst();
            ImportacaoEstoquePlanilhaRequest.RetiradaImportacao retirada = entry.getValue();
            int saldoDesejado;
            try {
                saldoDesejado = retirada.saldoFinal().max(BigDecimal.ZERO).intValueExact();
            } catch (ArithmeticException ex) {
                throw new IllegalArgumentException(
                        "O saldo final de " + retirada.nomeMaterial()
                                + " precisa ser um número inteiro.", ex);
            }
            int saldoAtual = material.getQuantidadeDisponivel() != null
                    ? material.getQuantidadeDisponivel()
                    : 0;
            if (saldoAtual == saldoDesejado) {
                continue;
            }
            estoqueService.reconciliarSaldoPlanilha(
                    material.getId(),
                    local.getId(),
                    saldoDesejado,
                    retirada.custoUnitario(),
                    "Saldo final após retiradas importadas de " + importacao.getNomeArquivo(),
                    responsavel);
            material.setQuantidadeDisponivel(saldoDesejado);
            ajustes++;
        }
        return ajustes;
    }

    private ImportacaoEstoquePlanilhaDetalheDTO mapearDetalhe(
            ImportacaoEstoquePlanilha importacao, boolean incluirItens) {
        List<ImportacaoEstoquePlanilhaDetalheDTO.ItemEstoque> itens = incluirItens
                ? itemImportacaoRepository.findByImportacaoIdOrderByNomePlanilhaAsc(importacao.getId())
                        .stream()
                        .map(item -> new ImportacaoEstoquePlanilhaDetalheDTO.ItemEstoque(
                                item.getMaterial().getId(),
                                item.getMaterial().getNome(),
                                item.getSaldoAnterior(),
                                item.getSaldoImportado(),
                                item.getCustoUnitario(),
                                item.getAcao()))
                        .toList()
                : List.of();
        List<ImportacaoEstoquePlanilhaDetalheDTO.Retirada> retiradas = incluirItens
                ? retiradaImportacaoRepository
                        .findByImportacaoIdOrderByAbaOrigemAscMaterialNomeAsc(importacao.getId())
                        .stream()
                        .map(this::mapearRetirada)
                        .toList()
                : List.of();
        return new ImportacaoEstoquePlanilhaDetalheDTO(
                importacao.getId(),
                importacao.getNomeArquivo(),
                importacao.getDataImportacao(),
                importacao.getDataComplementacao(),
                importacao.getImportadoPor(),
                importacao.getComplementadoPor(),
                importacao.getLocalEstoque().getNome(),
                importacao.getItensProcessados(),
                importacao.getMateriaisCriados(),
                importacao.getMateriaisAtualizados(),
                importacao.getAjustesPositivos(),
                importacao.getAjustesNegativos(),
                importacao.getValorTotalImportado(),
                importacao.getAbasRetiradaProcessadas(),
                importacao.getRetiradasImportadas(),
                importacao.getFaltasIdentificadas(),
                itens,
                retiradas);
    }

    private ImportacaoEstoquePlanilhaDetalheDTO.Retirada mapearRetirada(
            ImportacaoRetiradaPlanilha retirada) {
        return new ImportacaoEstoquePlanilhaDetalheDTO.Retirada(
                retirada.getAbaOrigem(),
                retirada.getComarca().getId(),
                retirada.getComarca().getNomeComarca(),
                retirada.getComarca().getOrdemServico() != null
                        ? retirada.getComarca().getOrdemServico().getNumeroOs()
                        : null,
                retirada.getMaterial().getId(),
                retirada.getMaterial().getNome(),
                retirada.getSaldoInicial(),
                retirada.getQuantidadeRetirada(),
                retirada.getSaldoFinal(),
                retirada.getQuantidadeFaltante(),
                retirada.getCustoUnitario(),
                retirada.getDataRetirada());
    }

    private void validarRetirada(ImportacaoEstoquePlanilhaRequest.RetiradaImportacao retirada) {
        if (retirada == null || retirada.aba() == null || retirada.aba().isBlank()) {
            throw new IllegalArgumentException("Toda retirada importada precisa informar a aba de origem.");
        }
        if (retirada.comarcaId() == null) {
            throw new IllegalArgumentException(
                    "Vincule a aba " + retirada.aba() + " a uma obra antes de importar.");
        }
        if (retirada.nomeMaterial() == null || retirada.nomeMaterial().isBlank()) {
            throw new IllegalArgumentException("Toda retirada importada precisa informar o material.");
        }
        if (retirada.saldoInicial() == null
                || retirada.quantidadeRetirada() == null
                || retirada.quantidadeRetirada().signum() < 0
                || retirada.saldoFinal() == null
                || retirada.custoUnitario() == null
                || retirada.custoUnitario().signum() < 0) {
            throw new IllegalArgumentException(
                    "Valores inválidos para " + retirada.nomeMaterial()
                            + " na aba " + retirada.aba()
                            + referenciaLinha(retirada.linhaOrigem()) + ".");
        }
        if (!numeroInteiro(retirada.saldoInicial())
                || !numeroInteiro(retirada.quantidadeRetirada())
                || !numeroInteiro(retirada.saldoFinal())) {
            throw new IllegalArgumentException(
                    "Os saldos e a quantidade retirada de " + retirada.nomeMaterial()
                            + " na aba " + retirada.aba()
                            + referenciaLinha(retirada.linhaOrigem())
                            + " precisam ser números inteiros.");
        }
    }

    private record ResultadoRetiradas(int abas, int retiradas, int faltas, int ajustesSaldo) {
    }

    private Material novoMaterial(
            ImportacaoEstoquePlanilhaRequest.ItemImportacao item,
            LocalEstoque local,
            Map<String, Integer> sequenciais) {
        Material material = new Material();
        material.setNome(item.nome().trim());
        material.setPartNumber(gerarPartNumber(item.nome(), sequenciais));
        material.setCategoria("MATERIAL_CONSUMO");
        material.setDescricao("Material importado da planilha de estoque.");
        material.setTipoControle(TipoControleEstoque.UNIDADE);
        material.setUnidadeMedida(UnidadeMedida.UNIDADE);
        material.setQuantidadeDisponivel(0);
        material.setQuantidadeReservada(0);
        material.setEstoqueMinimo(BigDecimal.ZERO);
        material.setCustoMedio(item.custoUnitario());
        material.setLocalizacao(local.getNome());
        return material;
    }

    private String gerarPartNumber(String nome, Map<String, Integer> sequenciais) {
        String base = normalizar(nome).replace(" ", "-").toUpperCase(Locale.ROOT);
        base = base.length() > 24 ? base.substring(0, 24) : base;
        if (base.isBlank()) {
            base = "MATERIAL";
        }
        int sequencial = sequenciais.merge(base, 1, Integer::sum);
        String candidato;
        do {
            candidato = "IMP-" + base + (sequencial > 1 ? "-" + sequencial : "");
            sequencial++;
        } while (materialRepository.existsByPartNumberIgnoreCase(candidato));
        sequenciais.put(base, sequencial - 1);
        return candidato;
    }

    private void validarRequest(ImportacaoEstoquePlanilhaRequest request) {
        if (request == null
                || request.nomeArquivo() == null
                || request.nomeArquivo().isBlank()) {
            throw new IllegalArgumentException("Nome do arquivo é obrigatório.");
        }
        if (request.hashSha256() == null
                || !request.hashSha256().trim().matches("(?i)^[a-f0-9]{64}$")) {
            throw new IllegalArgumentException("Identificador da planilha é inválido.");
        }
        if (request.localEstoqueId() == null) {
            throw new IllegalArgumentException("Selecione o depósito que receberá os saldos importados.");
        }
        if (request.itens() == null || request.itens().isEmpty()) {
            throw new IllegalArgumentException("A planilha não possui materiais válidos.");
        }
        if (request.itens().size() > 1000) {
            throw new IllegalArgumentException("A planilha excede o limite de 1.000 materiais.");
        }
        if (request.retiradas() != null && request.retiradas().size() > 5000) {
            throw new IllegalArgumentException("A planilha excede o limite de 5.000 retiradas.");
        }
        if (request.avisos() != null && request.avisos().stream().anyMatch(this::possuiTexto)) {
            String primeiroAviso = request.avisos().stream()
                    .filter(this::possuiTexto)
                    .findFirst()
                    .orElse("Há linhas inválidas na planilha.");
            throw new IllegalArgumentException(
                    "A importação foi bloqueada porque há linhas inválidas. " + primeiroAviso);
        }
    }

    private void validarConteudoCompleto(ImportacaoEstoquePlanilhaRequest request) {
        Map<String, ImportacaoEstoquePlanilhaRequest.ItemImportacao> itensPorNome =
                new LinkedHashMap<>();
        for (ImportacaoEstoquePlanilhaRequest.ItemImportacao item : request.itens()) {
            validarItem(item);
            String chave = normalizar(item.nome());
            if (itensPorNome.putIfAbsent(chave, item) != null) {
                throw new IllegalArgumentException(
                        "Material duplicado na planilha"
                                + referenciaLinha(item.linhaOrigem()) + ": " + item.nome().trim() + ".");
            }
        }

        Map<String, BigDecimal> saldoEsperadoPorMaterial = new HashMap<>();
        Set<String> materiaisPorAba = new HashSet<>();
        for (ImportacaoEstoquePlanilhaRequest.RetiradaImportacao retirada
                : request.retiradas() != null ? request.retiradas() : List.<ImportacaoEstoquePlanilhaRequest.RetiradaImportacao>of()) {
            validarRetirada(retirada);
            String chaveMaterial = normalizar(retirada.nomeMaterial());
            ImportacaoEstoquePlanilhaRequest.ItemImportacao itemBase = itensPorNome.get(chaveMaterial);
            if (itemBase == null) {
                throw new IllegalArgumentException(
                        "O material " + retirada.nomeMaterial() + " da aba " + retirada.aba()
                                + referenciaLinha(retirada.linhaOrigem())
                                + " não existe no inventário-base da planilha.");
            }
            String chaveAbaMaterial = normalizar(retirada.aba()) + "|" + chaveMaterial;
            if (!materiaisPorAba.add(chaveAbaMaterial)) {
                throw new IllegalArgumentException(
                        "O material " + retirada.nomeMaterial() + " aparece mais de uma vez na aba "
                                + retirada.aba() + ".");
            }

            BigDecimal saldoEsperado = saldoEsperadoPorMaterial.getOrDefault(
                    chaveMaterial, BigDecimal.valueOf(itemBase.quantidade()));
            if (retirada.saldoInicial().compareTo(saldoEsperado) != 0) {
                throw new IllegalArgumentException(
                        "Sequência de saldo inválida para " + retirada.nomeMaterial() + " na aba "
                                + retirada.aba() + referenciaLinha(retirada.linhaOrigem())
                                + ": esperado " + saldoEsperado.toPlainString() + ", recebido "
                                + retirada.saldoInicial().toPlainString() + ".");
            }
            BigDecimal saldoCalculado = retirada.saldoInicial().subtract(retirada.quantidadeRetirada());
            if (retirada.saldoFinal().compareTo(saldoCalculado) != 0) {
                throw new IllegalArgumentException(
                        "Saldo final inválido para " + retirada.nomeMaterial() + " na aba "
                                + retirada.aba() + referenciaLinha(retirada.linhaOrigem())
                                + ": deveria ser " + saldoCalculado.toPlainString() + ".");
            }
            saldoEsperadoPorMaterial.put(chaveMaterial, retirada.saldoFinal());
        }
    }

    private void validarInventarioBaseDaComplementacao(
            ImportacaoEstoquePlanilha importacao,
            List<ImportacaoEstoquePlanilhaRequest.ItemImportacao> itensRecebidos) {
        List<ImportacaoEstoqueItemPlanilha> itensOriginais =
                itemImportacaoRepository.findByImportacaoIdOrderByNomePlanilhaAsc(importacao.getId());
        if (itensOriginais.isEmpty()) {
            throw new IllegalStateException(
                    "Não foi possível validar o inventário-base da importação original.");
        }
        Map<String, ImportacaoEstoqueItemPlanilha> originaisPorNome = new HashMap<>();
        for (ImportacaoEstoqueItemPlanilha item : itensOriginais) {
            originaisPorNome.put(normalizar(item.getNomePlanilha()), item);
        }
        if (originaisPorNome.size() != itensRecebidos.size()) {
            throw new IllegalArgumentException(
                    "O inventário-base foi alterado desde a importação original. Importe o arquivo original.");
        }
        for (ImportacaoEstoquePlanilhaRequest.ItemImportacao recebido : itensRecebidos) {
            ImportacaoEstoqueItemPlanilha original = originaisPorNome.get(normalizar(recebido.nome()));
            if (original == null
                    || !recebido.quantidade().equals(original.getSaldoImportado())
                    || recebido.custoUnitario().compareTo(original.getCustoUnitario()) != 0) {
                throw new IllegalArgumentException(
                        "O inventário-base foi alterado para " + recebido.nome()
                                + referenciaLinha(recebido.linhaOrigem())
                                + ". Use os mesmos dados da importação original.");
            }
        }
    }

    private void validarItem(ImportacaoEstoquePlanilhaRequest.ItemImportacao item) {
        if (item == null || item.nome() == null || item.nome().isBlank()) {
            throw new IllegalArgumentException("Todo material precisa ter um nome.");
        }
        if (item.quantidade() == null || item.quantidade() < 0) {
            throw new IllegalArgumentException(
                    "Quantidade inválida para " + item.nome()
                            + referenciaLinha(item.linhaOrigem()) + ".");
        }
        if (item.custoUnitario() == null || item.custoUnitario().signum() < 0) {
            throw new IllegalArgumentException(
                    "Custo unitário inválido para " + item.nome()
                            + referenciaLinha(item.linhaOrigem()) + ".");
        }
    }

    private boolean possuiTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private boolean numeroInteiro(BigDecimal valor) {
        return valor.stripTrailingZeros().scale() <= 0;
    }

    private String referenciaLinha(Integer linha) {
        return linha != null ? ", linha " + linha : "";
    }

    private String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^\\p{Alnum}]+", " ")
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private String limitar(String valor, int limite) {
        return valor.length() <= limite ? valor : valor.substring(0, limite);
    }
}
