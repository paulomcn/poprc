package com.poprc.demo.service;

import com.poprc.demo.dto.DevolverOrdemRetiradaRequest;
import com.poprc.demo.dto.ExecutarOrdemRetiradaRequest;
import com.poprc.demo.exception.SaldoInsuficienteException;
import com.poprc.demo.model.Comarca;
import com.poprc.demo.model.Material;
import com.poprc.demo.model.MaterialItem;
import com.poprc.demo.model.MovimentacaoEstoque;
import com.poprc.demo.model.OrdemRetirada;
import com.poprc.demo.model.OrdemRetiradaItem;
import com.poprc.demo.model.OrdemServico;
import com.poprc.demo.model.TipoMovimentacao;
import com.poprc.demo.repository.MaterialItemRepository;
import com.poprc.demo.repository.MaterialRepository;
import com.poprc.demo.repository.MovimentacaoEstoqueRepository;
import com.poprc.demo.repository.ComarcaRepository;
import com.poprc.demo.repository.OrdemRetiradaRepository;
import com.poprc.demo.repository.OrdemServicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrdemRetiradaService {

    private static final String STATUS_GERADA = "GERADA";
    private static final String STATUS_RETIRADA = "RETIRADA";
    private static final String STATUS_DEVOLVIDA = "DEVOLVIDA";
    private static final String CATEGORIA_FERRAMENTA = "FERRAMENTA";

    private final OrdemRetiradaRepository ordemRetiradaRepository;
    private final OrdemServicoRepository ordemServicoRepository;
    private final ComarcaRepository comarcaRepository;
    private final MaterialRepository materialRepository;
    private final MaterialItemRepository materialItemRepository;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

    @Transactional
    public OrdemRetirada criarParaOrdemServico(OrdemServico ordemServico, Comarca comarca, String geradoPor) {
        if (ordemServico == null || ordemServico.getId() == null) {
            throw new IllegalArgumentException("OS é obrigatória para gerar a OR.");
        }
        if (comarca == null || comarca.getMateriais() == null || comarca.getMateriais().isEmpty()) {
            throw new IllegalArgumentException("A OS precisa ter materiais previstos para gerar uma OR.");
        }

        OrdemRetirada ordemRetirada = new OrdemRetirada();
        ordemRetirada.setNumeroOr(gerarNumeroOr(ordemServico));
        ordemRetirada.setStatus(STATUS_GERADA);
        ordemRetirada.setOrdemServico(ordemServico);
        ordemRetirada.setComarca(comarca);
        ordemRetirada.setGeradoPor(geradoPor != null && !geradoPor.isBlank() ? geradoPor : "Sistema");
        ordemRetirada.setDataGeracao(LocalDateTime.now());

        for (MaterialItem materialItem : comarca.getMateriais()) {
            if (materialItem.getMaterial() == null) {
                continue;
            }
            OrdemRetiradaItem item = new OrdemRetiradaItem();
            item.setOrdemRetirada(ordemRetirada);
            item.setMaterialItem(materialItem);
            item.setMaterial(materialItem.getMaterial());
            item.setNomeMaterial(materialItem.getNomeMaterial());
            item.setCategoria(materialItem.getMaterial().getCategoria());
            item.setQuantidadeSolicitada(materialItem.getQuantidadePrevista());
            ordemRetirada.getItens().add(item);
        }

        if (ordemRetirada.getItens().isEmpty()) {
            throw new IllegalArgumentException("A OR precisa ter ao menos um item vinculado ao estoque.");
        }

        return ordemRetiradaRepository.save(ordemRetirada);
    }

    @Transactional
    public OrdemRetirada criarAdicionalParaOs(Long ordemServicoId, String geradoPor) {
        OrdemServico ordemServico = ordemServicoRepository.findById(ordemServicoId)
                .orElseThrow(() -> new IllegalArgumentException("OS não encontrada."));
        if (ordemServico.getProjeto() == null || ordemServico.getProjeto().getId() == null) {
            throw new IllegalArgumentException("OS sem projeto/comarca vinculada.");
        }
        Comarca comarca = comarcaRepository.findByProjetoId(ordemServico.getProjeto().getId())
                .orElseThrow(() -> new IllegalArgumentException("Comarca da OS não encontrada."));
        return criarParaOrdemServico(ordemServico, comarca, geradoPor);
    }

    @Transactional(readOnly = true)
    public List<OrdemRetirada> listarTodas() {
        return ordemRetiradaRepository.findAllByOrderByDataGeracaoDesc();
    }

    @Transactional(readOnly = true)
    public List<OrdemRetirada> listarPorComarca(Long comarcaId) {
        return ordemRetiradaRepository.findByComarcaIdOrderByDataGeracaoDesc(comarcaId);
    }

    @Transactional(readOnly = true)
    public List<OrdemRetirada> listarPorOs(Long ordemServicoId) {
        return ordemRetiradaRepository.findByOrdemServicoIdOrderByDataGeracaoDesc(ordemServicoId);
    }

    @Transactional
    public OrdemRetirada executarRetirada(Long id, ExecutarOrdemRetiradaRequest request) {
        OrdemRetirada ordemRetirada = buscar(id);
        if (!STATUS_GERADA.equals(ordemRetirada.getStatus())) {
            throw new IllegalArgumentException("Esta OR não está disponível para retirada.");
        }
        validarTexto(request.getConferidoPor(), "Informe quem conferiu os itens.");
        validarTexto(request.getLevadoPor(), "Informe quem levou os itens.");
        validarTexto(request.getAssinaturaConferenteBase64(), "Assinatura de quem conferiu é obrigatória.");
        validarTexto(request.getAssinaturaRetiranteBase64(), "Assinatura de quem levou é obrigatória.");

        LocalDateTime agora = LocalDateTime.now();
        for (OrdemRetiradaItem item : ordemRetirada.getItens()) {
            Material material = item.getMaterial();
            int quantidade = valor(item.getQuantidadeSolicitada());
            int disponivel = valor(material.getQuantidadeDisponivel());
            int reservado = valor(material.getQuantidadeReservada());
            if (quantidade > disponivel) {
                throw new SaldoInsuficienteException("Estoque insuficiente para retirar " + material.getNome()
                        + ". Em estoque: " + disponivel + ", solicitado: " + quantidade + ".");
            }

            material.setQuantidadeDisponivel(disponivel - quantidade);
            material.setQuantidadeReservada(Math.max(0, reservado - quantidade));
            materialRepository.save(material);

            item.setQuantidadeRetirada(quantidade);
            MaterialItem materialItem = item.getMaterialItem();
            if (materialItem != null) {
                materialItem.setEstoqueReservado(false);
                materialItem.setEstoqueBaixado(true);
                materialItem.setDataHoraRetirada(agora);
                materialItemRepository.save(materialItem);
            }
            registrarMovimentacao(ordemRetirada, item, quantidade, TipoMovimentacao.RETIRADA_OR,
                    "Retirada via " + ordemRetirada.getNumeroOr() + " | Conferente: "
                            + request.getConferidoPor() + " | Levou: " + request.getLevadoPor());
        }

        ordemRetirada.setStatus(STATUS_RETIRADA);
        ordemRetirada.setDataRetirada(agora);
        ordemRetirada.setConferidoPor(request.getConferidoPor().trim());
        ordemRetirada.setLevadoPor(request.getLevadoPor().trim());
        ordemRetirada.setAssinaturaConferenteBase64(request.getAssinaturaConferenteBase64());
        ordemRetirada.setAssinaturaRetiranteBase64(request.getAssinaturaRetiranteBase64());
        return ordemRetiradaRepository.save(ordemRetirada);
    }

    @Transactional
    public OrdemRetirada devolver(Long id, DevolverOrdemRetiradaRequest request) {
        OrdemRetirada ordemRetirada = buscar(id);
        if (!STATUS_RETIRADA.equals(ordemRetirada.getStatus())) {
            throw new IllegalArgumentException("A OR precisa estar retirada para registrar devolução.");
        }
        validarTexto(request.getDevolvidoPor(), "Informe quem devolveu os itens.");
        validarTexto(request.getRecebidoPor(), "Informe quem conferiu/recebeu a devolução.");
        validarTexto(request.getAssinaturaRecebimentoBase64(), "Assinatura de recebimento é obrigatória.");

        Map<Long, DevolverOrdemRetiradaRequest.ItemDevolucaoRequest> devolucoes = request.getItens() == null
                ? Map.of()
                : request.getItens().stream()
                        .filter(item -> item.getItemId() != null)
                        .collect(Collectors.toMap(
                                DevolverOrdemRetiradaRequest.ItemDevolucaoRequest::getItemId,
                                Function.identity(),
                                (a, b) -> b));

        for (OrdemRetiradaItem item : ordemRetirada.getItens()) {
            int retirada = valor(item.getQuantidadeRetirada());
            int devolvida = valor(devolucoes.getOrDefault(item.getId(),
                    new DevolverOrdemRetiradaRequest.ItemDevolucaoRequest()).getQuantidadeDevolvida());
            if (devolvida < 0 || devolvida > retirada) {
                throw new IllegalArgumentException("Quantidade devolvida inválida para " + item.getNomeMaterial() + ".");
            }
            if (CATEGORIA_FERRAMENTA.equals(item.getCategoria()) && devolvida < retirada) {
                throw new IllegalArgumentException("Ferramentas devem retornar obrigatoriamente: " + item.getNomeMaterial() + ".");
            }

            item.setQuantidadeDevolvida(devolvida);
            if (devolvida > 0) {
                Material material = item.getMaterial();
                material.setQuantidadeDisponivel(valor(material.getQuantidadeDisponivel()) + devolvida);
                materialRepository.save(material);
                registrarMovimentacao(ordemRetirada, item, devolvida, TipoMovimentacao.DEVOLUCAO_OR,
                        "Devolução via " + ordemRetirada.getNumeroOr() + " | Devolveu: "
                                + request.getDevolvidoPor() + " | Recebeu: " + request.getRecebidoPor());
            }

            // O item permanece marcado como baixado para impedir baixa duplicada no As-Built.
            // A volta física ao estoque é registrada pela movimentação DEVOLUCAO_OR.
        }

        ordemRetirada.setStatus(STATUS_DEVOLVIDA);
        ordemRetirada.setDataDevolucao(LocalDateTime.now());
        ordemRetirada.setDevolvidoPor(request.getDevolvidoPor().trim());
        ordemRetirada.setRecebidoPor(request.getRecebidoPor().trim());
        ordemRetirada.setAssinaturaRecebimentoBase64(request.getAssinaturaRecebimentoBase64());
        return ordemRetiradaRepository.save(ordemRetirada);
    }

    private OrdemRetirada buscar(Long id) {
        return ordemRetiradaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ordem de Retirada não encontrada."));
    }

    private String gerarNumeroOr(OrdemServico ordemServico) {
        long sequencial = ordemRetiradaRepository.countByOrdemServicoId(ordemServico.getId()) + 1;
        String numero;
        do {
            numero = ordemServico.getNumeroOs() + " - OR " + String.format("%02d", sequencial);
            sequencial++;
        } while (ordemRetiradaRepository.existsByNumeroOr(numero));
        return numero;
    }

    private void registrarMovimentacao(OrdemRetirada ordemRetirada, OrdemRetiradaItem item, int quantidade,
            TipoMovimentacao tipo, String observacao) {
        MovimentacaoEstoque movimentacao = new MovimentacaoEstoque();
        movimentacao.setMaterial(item.getMaterial());
        movimentacao.setQuantidade(quantidade);
        movimentacao.setTipo(tipo);
        movimentacao.setDataMovimentacao(LocalDateTime.now());
        movimentacao.setObservacao(observacao);
        movimentacao.setComarca(ordemRetirada.getComarca());
        movimentacao.setProjeto(ordemRetirada.getComarca() != null ? ordemRetirada.getComarca().getProjeto() : null);
        movimentacao.setOrdemServico(ordemRetirada.getOrdemServico());
        movimentacao.setOrdemRetirada(ordemRetirada);
        movimentacaoEstoqueRepository.save(movimentacao);
    }

    private void validarTexto(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensagem);
        }
    }

    private int valor(Integer valor) {
        return valor != null ? valor : 0;
    }
}
