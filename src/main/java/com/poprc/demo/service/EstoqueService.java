package com.poprc.demo.service;

import com.poprc.demo.model.Funcionario;
import com.poprc.demo.model.Material;
import com.poprc.demo.model.MovimentacaoEstoque;
import com.poprc.demo.model.TipoMovimentacao;
import com.poprc.demo.model.TipoControleEstoque;
import com.poprc.demo.model.UnidadeMedida;
import com.poprc.demo.repository.FuncionarioRepository;
import com.poprc.demo.repository.MaterialRepository;
import com.poprc.demo.repository.MovimentacaoEstoqueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class EstoqueService {

    private final MaterialRepository materialRepository;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final SaldoLocalService saldoLocalService;

    @Transactional
    public Material cadastrarMaterial(Material material) {
        if (TipoControleEstoque.BOBINA.equals(material.getTipoControle())
                || TipoControleEstoque.ROLO.equals(material.getTipoControle())) {
            material.setQuantidadeDisponivel(0);
            material.setMetragemDisponivel(BigDecimal.ZERO);
        }
        normalizarCadastro(material);
        Material salvo = materialRepository.save(material);
        saldoLocalService.sincronizarSaldoInicial(salvo);
        registrarSaldoInicial(salvo);
        return salvo;
    }

    @Transactional
    public Material atualizarMaterial(Long id, Material dados) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Material não encontrado."));

        TipoControleEstoque controleAtual = material.getTipoControle() != null
                ? material.getTipoControle()
                : TipoControleEstoque.UNIDADE;
        TipoControleEstoque novoControle = dados.getTipoControle() != null
                ? dados.getTipoControle()
                : TipoControleEstoque.UNIDADE;
        boolean possuiSaldo = valor(material.getQuantidadeDisponivel()) > 0
                || valor(material.getQuantidadeReservada()) > 0
                || valor(material.getMetragemDisponivel()).signum() > 0;
        if (possuiSaldo && !controleAtual.equals(novoControle)) {
            throw new IllegalArgumentException(
                    "O tipo de controle não pode ser alterado enquanto o material possuir saldo ou reserva.");
        }

        material.setNome(dados.getNome());
        material.setPartNumber(dados.getPartNumber());
        material.setCategoria(valorTexto(dados.getCategoria(), "MATERIAL_CONSUMO"));
        material.setDescricao(dados.getDescricao());
        material.setFotoProdutoUrl(dados.getFotoProdutoUrl());
        material.setFabricante(dados.getFabricante());
        material.setFornecedor(dados.getFornecedor());
        material.setLocalizacao(dados.getLocalizacao());
        material.setTipoControle(novoControle);
        material.setUnidadeMedida(dados.getUnidadeMedida());
        material.setDimensao(dados.getDimensao());
        material.setComprimentoPorPeca(dados.getComprimentoPorPeca());
        material.setEstoqueMinimo(dados.getEstoqueMinimo());

        normalizarCadastro(material);
        return materialRepository.save(material);
    }

    @Transactional
    public MovimentacaoEstoque registrarEntrada(Long materialId, Integer quantidade, BigDecimal metragem,
            Long funcionarioId, Long localEstoqueId) {
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new IllegalArgumentException("Material não encontrado"));
        
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado"));

        boolean controleMetragem = TipoControleEstoque.METRAGEM.equals(material.getTipoControle());
        if (TipoControleEstoque.BOBINA.equals(material.getTipoControle())
                || TipoControleEstoque.ROLO.equals(material.getTipoControle())) {
            throw new IllegalArgumentException("Cadastre a bobina/rolo individual para registrar esta entrada.");
        }
        BigDecimal saldoAnterior = saldoControle(material);
        if (controleMetragem) {
            validarPositivo(metragem, "A metragem de entrada deve ser maior que zero.");
            material.setMetragemDisponivel(valor(material.getMetragemDisponivel()).add(metragem));
        } else {
            if (quantidade == null || quantidade <= 0) {
                throw new IllegalArgumentException("A quantidade de entrada deve ser maior que zero.");
            }
            material.setQuantidadeDisponivel(valor(material.getQuantidadeDisponivel()) + quantidade);
            atualizarMetragemDerivada(material);
        }
        materialRepository.save(material);
        BigDecimal valorEntrada = controleMetragem ? metragem : BigDecimal.valueOf(quantidade);
        com.poprc.demo.model.LocalEstoque localDestino = saldoLocalService.creditar(
                material, localEstoqueId, valorEntrada);

        MovimentacaoEstoque movimentacao = new MovimentacaoEstoque();
        movimentacao.setMaterial(material);
        movimentacao.setQuantidade(quantidade);
        movimentacao.setMetragem(controleMetragem ? metragem : metragemDaQuantidade(material, quantidade));
        movimentacao.setUnidadeMedida(material.getUnidadeMedida());
        movimentacao.setTipo(TipoMovimentacao.ENTRADA);
        movimentacao.setFuncionario(funcionario);
        movimentacao.setDataMovimentacao(LocalDateTime.now());
        movimentacao.setSaldoAnterior(saldoAnterior);
        movimentacao.setSaldoPosterior(saldoControle(material));
        movimentacao.setEstoqueDestino(localDestino.getNome());
        movimentacao.setLancadoPor(funcionario.getNome());
        movimentacao.setMotivo("Entrada de estoque");

        return movimentacaoEstoqueRepository.save(movimentacao);
    }

    @Transactional
    public MovimentacaoEstoque registrarSaida(Long materialId, Integer quantidade, Long funcionarioId, Long comarcaId) {
        throw new IllegalArgumentException("Saída de material exige uma Ordem de Retirada executada e assinada.");
    }

    @Transactional
    public MovimentacaoEstoque registrarAjuste(Long materialId, Long localEstoqueId, BigDecimal valorAjuste,
            TipoMovimentacao tipo, String motivo, String lancadoPor, String autorizadoPor) {
        if (!TipoMovimentacao.AJUSTE_POSITIVO.equals(tipo)
                && !TipoMovimentacao.AJUSTE_NEGATIVO.equals(tipo)) {
            throw new IllegalArgumentException("Tipo de ajuste inválido.");
        }
        validarTexto(motivo, "O motivo do ajuste é obrigatório.");
        validarTexto(lancadoPor, "Informe quem lançou o ajuste.");
        validarTexto(autorizadoPor, "Informe quem autorizou o ajuste.");
        validarPositivo(valorAjuste, "O valor do ajuste deve ser maior que zero.");

        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new IllegalArgumentException("Material não encontrado."));
        if (TipoControleEstoque.BOBINA.equals(material.getTipoControle())
                || TipoControleEstoque.ROLO.equals(material.getTipoControle())) {
            throw new IllegalArgumentException("Ajustes de bobina/rolo devem ser feitos na unidade rastreável.");
        }
        if (!controlaMetragem(material) && valorAjuste.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("Materiais por unidade exigem um ajuste inteiro.");
        }

        BigDecimal saldoAnterior = saldoControle(material);
        BigDecimal saldoPosterior = TipoMovimentacao.AJUSTE_POSITIVO.equals(tipo)
                ? saldoAnterior.add(valorAjuste)
                : saldoAnterior.subtract(valorAjuste);
        if (saldoPosterior.signum() < 0) {
            throw new IllegalArgumentException("O ajuste deixaria o estoque com saldo negativo.");
        }
        atualizarSaldoControle(material, saldoPosterior);
        materialRepository.save(material);
        com.poprc.demo.model.LocalEstoque localAjuste = TipoMovimentacao.AJUSTE_POSITIVO.equals(tipo)
                ? saldoLocalService.creditar(material, localEstoqueId, valorAjuste)
                : saldoLocalService.debitar(material, localEstoqueId, valorAjuste);

        MovimentacaoEstoque movimentacao = novaMovimentacaoAuditoria(material, tipo, valorAjuste,
                saldoAnterior, saldoPosterior, motivo, lancadoPor, autorizadoPor);
        movimentacao.setEstoqueOrigem(localAjuste.getNome());
        movimentacao.setEstoqueDestino(localAjuste.getNome());
        return movimentacaoEstoqueRepository.save(movimentacao);
    }

    @Transactional
    public MovimentacaoEstoque transferirLocalizacao(Long materialId, Long origemId, Long destinoId,
            BigDecimal valorTransferido, String motivo, String lancadoPor, String autorizadoPor) {
        validarPositivo(valorTransferido, "O valor da transferência deve ser maior que zero.");
        validarTexto(motivo, "O motivo da transferência é obrigatório.");
        validarTexto(lancadoPor, "Informe quem lançou a transferência.");
        validarTexto(autorizadoPor, "Informe quem autorizou a transferência.");

        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new IllegalArgumentException("Material não encontrado."));
        if (TipoControleEstoque.BOBINA.equals(material.getTipoControle())
                || TipoControleEstoque.ROLO.equals(material.getTipoControle())) {
            throw new IllegalArgumentException("Transfira a bobina/rolo pela unidade rastreável completa.");
        }
        BigDecimal saldo = saldoControle(material);
        SaldoLocalService.TransferenciaLocal transferencia = saldoLocalService.transferir(
                material, origemId, destinoId, valorTransferido);

        MovimentacaoEstoque movimentacao = novaMovimentacaoAuditoria(material, TipoMovimentacao.TRANSFERENCIA,
                valorTransferido, saldo, saldo, motivo, lancadoPor, autorizadoPor);
        movimentacao.setEstoqueOrigem(transferencia.origem().getNome());
        movimentacao.setEstoqueDestino(transferencia.destino().getNome());
        return movimentacaoEstoqueRepository.save(movimentacao);
    }

    private void normalizarCadastro(Material material) {
        if (material.getNome() == null || material.getNome().isBlank()) {
            throw new IllegalArgumentException("Nome do material é obrigatório.");
        }
        if (material.getPartNumber() == null || material.getPartNumber().isBlank()) {
            throw new IllegalArgumentException("Part Number é obrigatório.");
        }
        material.setCategoria(valorTexto(material.getCategoria(), "MATERIAL_CONSUMO"));
        material.setTipoControle(material.getTipoControle() != null
                ? material.getTipoControle()
                : TipoControleEstoque.UNIDADE);
        material.setUnidadeMedida(material.getUnidadeMedida() != null
                ? material.getUnidadeMedida()
                : unidadePadrao(material.getTipoControle()));
        material.setQuantidadeDisponivel(Math.max(0, valor(material.getQuantidadeDisponivel())));
        material.setQuantidadeReservada(Math.max(0, valor(material.getQuantidadeReservada())));
        material.setMetragemDisponivel(valor(material.getMetragemDisponivel()).max(BigDecimal.ZERO));
        material.setMetragemReservada(valor(material.getMetragemReservada()).max(BigDecimal.ZERO));
        material.setEstoqueMinimo(valor(material.getEstoqueMinimo()).max(BigDecimal.ZERO));

        if (TipoControleEstoque.PECA_COM_COMPRIMENTO.equals(material.getTipoControle())) {
            validarPositivo(material.getComprimentoPorPeca(),
                    "Informe um comprimento por peça maior que zero.");
            material.setUnidadeMedida(UnidadeMedida.PECA);
            atualizarMetragemDerivada(material);
        } else if (TipoControleEstoque.METRAGEM.equals(material.getTipoControle())) {
            material.setUnidadeMedida(UnidadeMedida.METRO);
        }
    }

    private void registrarSaldoInicial(Material material) {
        int quantidade = valor(material.getQuantidadeDisponivel());
        BigDecimal metragem = valor(material.getMetragemDisponivel());
        if (quantidade <= 0 && metragem.signum() <= 0) {
            return;
        }

        MovimentacaoEstoque movimentacao = new MovimentacaoEstoque();
        movimentacao.setMaterial(material);
        movimentacao.setQuantidade(quantidade > 0 ? quantidade : null);
        movimentacao.setMetragem(metragem.signum() > 0 ? metragem : null);
        movimentacao.setUnidadeMedida(material.getUnidadeMedida());
        movimentacao.setTipo(TipoMovimentacao.ENTRADA);
        movimentacao.setDataMovimentacao(LocalDateTime.now());
        movimentacao.setObservacao("Saldo inicial informado no cadastro do material.");
        movimentacao.setMotivo("Saldo inicial");
        movimentacao.setLancadoPor("Sistema");
        movimentacao.setSaldoAnterior(BigDecimal.ZERO);
        movimentacao.setSaldoPosterior(saldoControle(material));
        movimentacao.setEstoqueDestino(material.getLocalizacao());
        movimentacaoEstoqueRepository.save(movimentacao);
    }

    private MovimentacaoEstoque novaMovimentacaoAuditoria(Material material, TipoMovimentacao tipo,
            BigDecimal valorMovimentado, BigDecimal saldoAnterior, BigDecimal saldoPosterior,
            String motivo, String lancadoPor, String autorizadoPor) {
        MovimentacaoEstoque movimentacao = new MovimentacaoEstoque();
        movimentacao.setMaterial(material);
        movimentacao.setTipo(tipo);
        movimentacao.setUnidadeMedida(material.getUnidadeMedida());
        if (controlaMetragem(material)) {
            movimentacao.setMetragem(valorMovimentado);
        } else if (valorMovimentado.signum() > 0) {
            movimentacao.setQuantidade(valorMovimentado.intValueExact());
        }
        movimentacao.setSaldoAnterior(saldoAnterior);
        movimentacao.setSaldoPosterior(saldoPosterior);
        movimentacao.setMotivo(motivo.trim());
        movimentacao.setObservacao(motivo.trim());
        movimentacao.setLancadoPor(lancadoPor.trim());
        movimentacao.setAutorizadoPor(autorizadoPor.trim());
        movimentacao.setDataMovimentacao(LocalDateTime.now());
        return movimentacao;
    }

    private BigDecimal saldoControle(Material material) {
        return controlaMetragem(material)
                ? valor(material.getMetragemDisponivel())
                : BigDecimal.valueOf(valor(material.getQuantidadeDisponivel()));
    }

    private void atualizarSaldoControle(Material material, BigDecimal saldo) {
        if (controlaMetragem(material)) {
            material.setMetragemDisponivel(saldo);
        } else {
            material.setQuantidadeDisponivel(saldo.intValueExact());
            atualizarMetragemDerivada(material);
        }
    }

    private boolean controlaMetragem(Material material) {
        return material != null && (TipoControleEstoque.METRAGEM.equals(material.getTipoControle())
                || TipoControleEstoque.BOBINA.equals(material.getTipoControle())
                || TipoControleEstoque.ROLO.equals(material.getTipoControle()));
    }

    private void validarTexto(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensagem);
        }
    }

    private void atualizarMetragemDerivada(Material material) {
        if (TipoControleEstoque.PECA_COM_COMPRIMENTO.equals(material.getTipoControle())
                && material.getComprimentoPorPeca() != null) {
            material.setMetragemDisponivel(material.getComprimentoPorPeca()
                    .multiply(BigDecimal.valueOf(valor(material.getQuantidadeDisponivel()))));
        }
    }

    private BigDecimal metragemDaQuantidade(Material material, Integer quantidade) {
        if (!TipoControleEstoque.PECA_COM_COMPRIMENTO.equals(material.getTipoControle())
                || material.getComprimentoPorPeca() == null || quantidade == null) {
            return null;
        }
        return material.getComprimentoPorPeca().multiply(BigDecimal.valueOf(quantidade));
    }

    private UnidadeMedida unidadePadrao(TipoControleEstoque tipoControle) {
        return switch (tipoControle) {
            case PECA_COM_COMPRIMENTO -> UnidadeMedida.PECA;
            case METRAGEM -> UnidadeMedida.METRO;
            case ROLO -> UnidadeMedida.ROLO;
            case BOBINA -> UnidadeMedida.BOBINA;
            default -> UnidadeMedida.UNIDADE;
        };
    }

    private void validarPositivo(BigDecimal valor, String mensagem) {
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException(mensagem);
        }
    }

    private int valor(Integer valor) {
        return valor != null ? valor : 0;
    }

    private BigDecimal valor(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private String valorTexto(String valor, String padrao) {
        return valor != null && !valor.isBlank() ? valor : padrao;
    }
}
