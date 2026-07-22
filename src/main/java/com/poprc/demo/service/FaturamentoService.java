package com.poprc.demo.service;

import com.poprc.demo.dto.FaturamentoPainelDTO;
import com.poprc.demo.model.Comarca;
import com.poprc.demo.model.Contrato;
import com.poprc.demo.model.Faturamento;
import com.poprc.demo.model.Projeto;
import com.poprc.demo.model.SituacaoFaturamento;
import com.poprc.demo.model.TipoContratante;
import com.poprc.demo.repository.ComarcaRepository;
import com.poprc.demo.repository.ContratoRepository;
import com.poprc.demo.repository.FaturamentoRepository;
import com.poprc.demo.repository.ProjetoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FaturamentoService {

    public static final BigDecimal ALIQUOTA_RETIDA = new BigDecimal("0.048000");
    public static final BigDecimal ALIQUOTA_A_PAGAR = new BigDecimal("0.149300");

    private final FaturamentoRepository faturamentoRepository;
    private final ContratoRepository contratoRepository;
    private final ProjetoRepository projetoRepository;
    private final ComarcaRepository comarcaRepository;

    @Transactional
    public Faturamento registrarMedicao(Faturamento faturamento, Long contratoId, Long projetoId) {
        Contrato contrato = buscarContrato(contratoId);
        Projeto projeto = buscarProjeto(projetoId);
        validarProjetoDoContrato(projeto, contratoId);
        validarValor(faturamento);

        faturamento.setContrato(contrato);
        faturamento.setProjeto(projeto);
        faturamento.setServicosExecutados(normalizarTexto(faturamento.getServicosExecutados()));
        faturamento.setSituacao(SituacaoFaturamento.A_FATURAR);
        limparDadosFiscais(faturamento);
        return faturamentoRepository.save(faturamento);
    }

    @Transactional
    public Faturamento atualizarMedicao(Long id, Faturamento dados, Long contratoId, Long projetoId) {
        Faturamento faturamento = buscarPorId(id);
        if (faturamento.getSituacao() != SituacaoFaturamento.A_FATURAR) {
            throw new IllegalStateException("Somente medições ainda não faturadas podem ser editadas.");
        }
        Contrato contrato = buscarContrato(contratoId);
        Projeto projeto = buscarProjeto(projetoId);
        validarProjetoDoContrato(projeto, contratoId);
        validarValor(dados);

        faturamento.setContrato(contrato);
        faturamento.setProjeto(projeto);
        faturamento.setServicosExecutados(normalizarTexto(dados.getServicosExecutados()));
        faturamento.setValorMedicao(dados.getValorMedicao());
        return faturamentoRepository.save(faturamento);
    }

    @Transactional
    public Faturamento emitirNotaFiscal(
            Long id,
            String numeroNF,
            LocalDate dataEmissao,
            LocalDate dataVencimento) {
        Faturamento faturamento = buscarPorId(id);

        if (faturamento.getSituacao() != SituacaoFaturamento.A_FATURAR) {
            throw new IllegalStateException("A nota fiscal só pode ser registrada em uma medição a faturar.");
        }
        if (numeroNF == null || numeroNF.isBlank()) {
            throw new IllegalArgumentException("O número da nota fiscal é obrigatório.");
        }
        if (dataEmissao == null) {
            throw new IllegalArgumentException("A data de emissão é obrigatória.");
        }
        if (dataVencimento == null) {
            throw new IllegalArgumentException("A data de vencimento da cobrança é obrigatória.");
        }
        if (dataVencimento.isBefore(dataEmissao)) {
            throw new IllegalArgumentException("O vencimento da cobrança não pode ser anterior à emissão.");
        }

        String numeroNormalizado = numeroNF.trim();
        if (faturamentoRepository.existsByContratoIdAndNumeroNotaFiscalIgnoreCase(
                faturamento.getContrato().getId(), numeroNormalizado)) {
            throw new IllegalArgumentException("Já existe uma nota fiscal com este número no contrato.");
        }

        BigDecimal impostoRetido = calcularImposto(faturamento.getValorMedicao(), ALIQUOTA_RETIDA);
        BigDecimal impostoPagar = calcularImposto(faturamento.getValorMedicao(), ALIQUOTA_A_PAGAR);

        faturamento.setNumeroNotaFiscal(numeroNormalizado);
        faturamento.setDataEmissao(dataEmissao);
        faturamento.setDataVencimento(dataVencimento);
        faturamento.setCompetenciaFiscal(calcularCompetencia(dataEmissao));
        faturamento.setAliquotaImpostoRetido(ALIQUOTA_RETIDA);
        faturamento.setAliquotaImpostoPagar(ALIQUOTA_A_PAGAR);
        faturamento.setImpostoRetido(impostoRetido);
        faturamento.setImpostoPagar(impostoPagar);
        faturamento.setImpostoTotal(impostoRetido.add(impostoPagar));
        faturamento.setSituacao(SituacaoFaturamento.FATURADO);
        return faturamentoRepository.save(faturamento);
    }

    @Transactional
    public Faturamento emitirNotaFiscal(Long id, String numeroNF, LocalDate dataVencimento) {
        return emitirNotaFiscal(id, numeroNF, LocalDate.now(), dataVencimento);
    }

    @Transactional
    public Faturamento darBaixaPagamento(Long id, LocalDate dataPagamento) {
        Faturamento faturamento = buscarPorId(id);

        if (faturamento.getSituacao() != SituacaoFaturamento.FATURADO
                && faturamento.getSituacao() != SituacaoFaturamento.EM_ATRASO) {
            throw new IllegalStateException("Somente faturamentos emitidos podem receber baixa.");
        }
        if (dataPagamento == null) {
            throw new IllegalArgumentException("A data do pagamento é obrigatória.");
        }
        if (dataPagamento.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("A data do pagamento não pode estar no futuro.");
        }
        if (faturamento.getDataEmissao() != null && dataPagamento.isBefore(faturamento.getDataEmissao())) {
            throw new IllegalArgumentException("A data do pagamento não pode ser anterior à emissão.");
        }

        faturamento.setSituacao(SituacaoFaturamento.PAGO);
        faturamento.setDataPagamento(dataPagamento);
        return faturamentoRepository.save(faturamento);
    }

    @Transactional
    public Faturamento darBaixaPagamento(Long id) {
        return darBaixaPagamento(id, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public Faturamento buscarPorId(Long id) {
        return faturamentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Faturamento não encontrado."));
    }

    @Transactional
    public List<Faturamento> listarTodos() {
        List<Faturamento> faturamentos = faturamentoRepository.findAll();
        LocalDate hoje = LocalDate.now();
        boolean alterou = false;
        for (Faturamento faturamento : faturamentos) {
            if (faturamento.getSituacao() == SituacaoFaturamento.FATURADO
                    && faturamento.getDataVencimento() != null
                    && faturamento.getDataVencimento().isBefore(hoje)) {
                faturamento.setSituacao(SituacaoFaturamento.EM_ATRASO);
                alterou = true;
            }
        }
        return alterou ? faturamentoRepository.saveAll(faturamentos) : faturamentos;
    }

    @Transactional
    public List<FaturamentoPainelDTO> listarPainel() {
        return listarTodos().stream().map(this::paraPainel).toList();
    }

    public LocalDate calcularCompetencia(LocalDate dataEmissao) {
        if (dataEmissao == null) {
            throw new IllegalArgumentException("A data de emissão é obrigatória.");
        }
        return dataEmissao.plusMonths(1).withDayOfMonth(20);
    }

    private FaturamentoPainelDTO paraPainel(Faturamento faturamento) {
        Contrato contrato = faturamento.getContrato();
        Projeto projeto = faturamento.getProjeto();
        TipoContratante tipo = contrato.getTipoContratante() == null
                ? TipoContratante.SETOR_PUBLICO
                : contrato.getTipoContratante();
        String destino = tipo == TipoContratante.SETOR_PRIVADO
                ? contrato.getCliente()
                : buscarNomeComarca(projeto);

        return new FaturamentoPainelDTO(
                faturamento.getId(),
                contrato.getId(),
                contrato.getContrato(),
                contrato.getCliente(),
                tipo,
                projeto != null ? projeto.getId() : null,
                destino,
                faturamento.getServicosExecutados(),
                faturamento.getValorMedicao(),
                faturamento.getNumeroNotaFiscal(),
                faturamento.getDataEmissao(),
                faturamento.getDataVencimento(),
                faturamento.getDataPagamento(),
                faturamento.getSituacao(),
                faturamento.getCompetenciaFiscal(),
                faturamento.getAliquotaImpostoRetido(),
                faturamento.getAliquotaImpostoPagar(),
                faturamento.getImpostoRetido(),
                faturamento.getImpostoPagar(),
                faturamento.getImpostoTotal(),
                calcularAlertaFiscal(faturamento.getCompetenciaFiscal()));
    }

    private String buscarNomeComarca(Projeto projeto) {
        if (projeto == null || projeto.getId() == null) {
            return "Comarca não informada";
        }
        return comarcaRepository.findByProjetoId(projeto.getId())
                .map(Comarca::getNomeComarca)
                .filter(nome -> nome != null && !nome.isBlank())
                .orElse("Comarca não informada");
    }

    private String calcularAlertaFiscal(LocalDate competencia) {
        if (competencia == null) {
            return "SEM_NF";
        }
        LocalDate hoje = LocalDate.now();
        if (competencia.isBefore(hoje)) {
            return "VENCIDO";
        }
        if (competencia.isEqual(hoje)) {
            return "VENCE_HOJE";
        }
        return !competencia.isAfter(hoje.plusDays(7)) ? "PROXIMO" : "NO_PRAZO";
    }

    private Contrato buscarContrato(Long contratoId) {
        return contratoRepository.findById(contratoId)
                .orElseThrow(() -> new IllegalArgumentException("Contrato não encontrado."));
    }

    private Projeto buscarProjeto(Long projetoId) {
        return projetoRepository.findById(projetoId)
                .orElseThrow(() -> new IllegalArgumentException("Projeto não encontrado."));
    }

    private void validarProjetoDoContrato(Projeto projeto, Long contratoId) {
        if (projeto.getContrato() == null || !contratoId.equals(projeto.getContrato().getId())) {
            throw new IllegalArgumentException("O projeto selecionado não pertence ao contrato informado.");
        }
        if (Boolean.TRUE.equals(projeto.getArquivado())) {
            throw new IllegalStateException("Não é possível faturar um projeto arquivado.");
        }
    }

    private void validarValor(Faturamento faturamento) {
        if (faturamento.getValorMedicao() == null || faturamento.getValorMedicao().signum() <= 0) {
            throw new IllegalArgumentException("O valor da medição deve ser maior que zero.");
        }
    }

    private BigDecimal calcularImposto(BigDecimal valor, BigDecimal aliquota) {
        return valor.multiply(aliquota).setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizarTexto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private void limparDadosFiscais(Faturamento faturamento) {
        faturamento.setNumeroNotaFiscal(null);
        faturamento.setDataEmissao(null);
        faturamento.setDataVencimento(null);
        faturamento.setDataPagamento(null);
        faturamento.setCompetenciaFiscal(null);
        faturamento.setAliquotaImpostoRetido(null);
        faturamento.setAliquotaImpostoPagar(null);
        faturamento.setImpostoRetido(null);
        faturamento.setImpostoPagar(null);
        faturamento.setImpostoTotal(null);
    }
}
