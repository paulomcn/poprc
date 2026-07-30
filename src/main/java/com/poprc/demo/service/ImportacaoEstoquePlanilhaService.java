package com.poprc.demo.service;

import com.poprc.demo.dto.ImportacaoEstoquePlanilhaRequest;
import com.poprc.demo.dto.ImportacaoEstoquePlanilhaResultadoDTO;
import com.poprc.demo.model.ImportacaoEstoquePlanilha;
import com.poprc.demo.model.LocalEstoque;
import com.poprc.demo.model.Material;
import com.poprc.demo.model.TipoControleEstoque;
import com.poprc.demo.model.UnidadeMedida;
import com.poprc.demo.repository.ImportacaoEstoquePlanilhaRepository;
import com.poprc.demo.repository.LocalEstoqueRepository;
import com.poprc.demo.repository.MaterialRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
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
    private final MaterialRepository materialRepository;
    private final LocalEstoqueRepository localEstoqueRepository;
    private final EstoqueService estoqueService;

    @Transactional
    public ImportacaoEstoquePlanilhaResultadoDTO importar(
            ImportacaoEstoquePlanilhaRequest request, String usuario) {
        validarRequest(request);
        String hash = request.hashSha256().trim().toLowerCase(Locale.ROOT);
        if (importacaoRepository.existsByHashSha256(hash)) {
            throw new IllegalArgumentException(
                    "Esta planilha já foi importada. Nenhum saldo foi alterado.");
        }

        LocalEstoque local = localEstoqueRepository.findById(request.localEstoqueId())
                .filter(item -> !Boolean.FALSE.equals(item.getAtivo()))
                .orElseThrow(() -> new IllegalArgumentException("Depósito de destino não encontrado ou inativo."));
        String responsavel = usuario != null && !usuario.isBlank() ? usuario : "Usuário autenticado";

        Map<String, List<Material>> materiaisPorNome = materialRepository.findAll().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        material -> normalizar(material.getNome())));
        Set<String> nomesRecebidos = new HashSet<>();

        ImportacaoEstoquePlanilha importacao = new ImportacaoEstoquePlanilha();
        importacao.setNomeArquivo(limitar(request.nomeArquivo().trim(), 255));
        importacao.setHashSha256(hash);
        importacao.setDataImportacao(LocalDateTime.now());
        importacao.setImportadoPor(limitar(responsavel, 255));
        importacao.setLocalEstoque(local);
        importacaoRepository.saveAndFlush(importacao);

        int criados = 0;
        int atualizados = 0;
        int positivos = 0;
        int negativos = 0;
        BigDecimal valorTotal = BigDecimal.ZERO;
        Map<String, Integer> sequenciaisPartNumber = new HashMap<>();

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
            if (correspondencias.isEmpty()) {
                material = novoMaterial(item, local, sequenciaisPartNumber);
                material = estoqueService.cadastrarMaterial(material);
                materiaisPorNome.put(nomeNormalizado, List.of(material));
                criados++;
            } else {
                material = correspondencias.getFirst();
                if (TipoControleEstoque.METRAGEM.equals(material.getTipoControle())
                        || TipoControleEstoque.BOBINA.equals(material.getTipoControle())
                        || TipoControleEstoque.ROLO.equals(material.getTipoControle())) {
                    throw new IllegalArgumentException(
                            "O material " + material.getNome()
                                    + " usa controle por metragem/bobina e não pode receber quantidade unitária da planilha.");
                }
                atualizados++;
            }

            int saldoAnterior = material.getQuantidadeDisponivel() != null
                    ? material.getQuantidadeDisponivel()
                    : 0;
            int saldoDesejado = item.quantidade();
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
            }
            valorTotal = valorTotal.add(
                    item.custoUnitario().multiply(BigDecimal.valueOf(saldoDesejado)));
        }

        importacao.setItensProcessados(request.itens().size());
        importacao.setMateriaisCriados(criados);
        importacao.setMateriaisAtualizados(atualizados);
        importacao.setAjustesPositivos(positivos);
        importacao.setAjustesNegativos(negativos);
        importacao.setValorTotalImportado(valorTotal.setScale(2, RoundingMode.HALF_UP));
        importacaoRepository.save(importacao);

        return new ImportacaoEstoquePlanilhaResultadoDTO(
                importacao.getId(),
                importacao.getNomeArquivo(),
                importacao.getDataImportacao(),
                importacao.getImportadoPor(),
                local.getNome(),
                importacao.getItensProcessados(),
                criados,
                atualizados,
                positivos,
                negativos,
                importacao.getValorTotalImportado());
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
    }

    private void validarItem(ImportacaoEstoquePlanilhaRequest.ItemImportacao item) {
        if (item == null || item.nome() == null || item.nome().isBlank()) {
            throw new IllegalArgumentException("Todo material precisa ter um nome.");
        }
        if (item.quantidade() == null || item.quantidade() < 0) {
            throw new IllegalArgumentException(
                    "Quantidade inválida para " + item.nome() + ".");
        }
        if (item.custoUnitario() == null || item.custoUnitario().signum() < 0) {
            throw new IllegalArgumentException(
                    "Custo unitário inválido para " + item.nome() + ".");
        }
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
