package com.poprc.demo.service;

import com.poprc.demo.model.Contrato;
import com.poprc.demo.model.Faturamento;
import com.poprc.demo.model.Projeto;
import com.poprc.demo.model.SituacaoFaturamento;
import com.poprc.demo.repository.ContratoRepository;
import com.poprc.demo.repository.FaturamentoRepository;
import com.poprc.demo.repository.ProjetoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class FaturamentoService {

    private final FaturamentoRepository faturamentoRepository;
    private final ContratoRepository contratoRepository;
    private final ProjetoRepository projetoRepository;

    // Regra 1: Registrar Medição (Nasce como A_FATURAR)
    @Transactional
    public Faturamento registrarMedicao(Faturamento faturamento, Long contratoId, Long projetoId) {
        Contrato contrato = contratoRepository.findById(contratoId)
                .orElseThrow(() -> new IllegalArgumentException("Contrato não encontrado."));
        Projeto projeto = projetoRepository.findById(projetoId)
                .orElseThrow(() -> new IllegalArgumentException("Projeto não encontrado."));
        validarProjetoDoContrato(projeto, contratoId);
        validarCampos(faturamento);

        faturamento.setContrato(contrato);
        faturamento.setProjeto(projeto);
        faturamento.setSituacao(SituacaoFaturamento.A_FATURAR);
        faturamento.setNumeroNotaFiscal(null);
        return faturamentoRepository.save(faturamento);
    }

    @Transactional
    public Faturamento atualizarMedicao(Long id, Faturamento dados, Long contratoId, Long projetoId) {
        Faturamento faturamento = buscarPorId(id);
        if (faturamento.getSituacao() != SituacaoFaturamento.A_FATURAR) {
            throw new IllegalStateException("Somente medições ainda não faturadas podem ser editadas.");
        }
        Contrato contrato = contratoRepository.findById(contratoId)
                .orElseThrow(() -> new IllegalArgumentException("Contrato não encontrado."));
        Projeto projeto = projetoRepository.findById(projetoId)
                .orElseThrow(() -> new IllegalArgumentException("Projeto não encontrado."));
        validarProjetoDoContrato(projeto, contratoId);
        validarCampos(dados);

        faturamento.setContrato(contrato);
        faturamento.setProjeto(projeto);
        faturamento.setServicosExecutados(dados.getServicosExecutados().trim());
        faturamento.setValorMedicao(dados.getValorMedicao());
        faturamento.setDataVencimento(dados.getDataVencimento());
        return faturamentoRepository.save(faturamento);
    }

    // Regra 2: Emitir Nota Fiscal (Vira FATURADO e ganha vencimento)
    @Transactional
    public Faturamento emitirNotaFiscal(Long id, String numeroNF, LocalDate dataVencimento) {
        Faturamento faturamento = faturamentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Faturamento não encontrado."));

        if (faturamento.getSituacao() != SituacaoFaturamento.A_FATURAR) {
            throw new IllegalStateException("A nota fiscal só pode ser registrada em uma medição a faturar.");
        }
        if (numeroNF == null || numeroNF.isBlank()) {
            throw new IllegalArgumentException("O número da nota fiscal é obrigatório.");
        }
        if (dataVencimento == null) {
            throw new IllegalArgumentException("A data de vencimento é obrigatória.");
        }

        faturamento.setNumeroNotaFiscal(numeroNF.trim());
        faturamento.setSituacao(SituacaoFaturamento.FATURADO);
        faturamento.setDataVencimento(dataVencimento);
        return faturamentoRepository.save(faturamento);
    }

    // Regra 3: Dar Baixa no Pagamento (Vira PAGO)
    @Transactional
    public Faturamento darBaixaPagamento(Long id) {
        Faturamento faturamento = faturamentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Faturamento não encontrado."));

        if (faturamento.getSituacao() != SituacaoFaturamento.FATURADO
                && faturamento.getSituacao() != SituacaoFaturamento.EM_ATRASO) {
            throw new IllegalStateException("Somente faturamentos emitidos podem receber baixa.");
        }

        faturamento.setSituacao(SituacaoFaturamento.PAGO);
        return faturamentoRepository.save(faturamento);
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

    private void validarProjetoDoContrato(Projeto projeto, Long contratoId) {
        if (projeto.getContrato() == null || !contratoId.equals(projeto.getContrato().getId())) {
            throw new IllegalArgumentException("O projeto selecionado não pertence ao contrato informado.");
        }
        if (Boolean.TRUE.equals(projeto.getArquivado())) {
            throw new IllegalStateException("Não é possível faturar um projeto arquivado.");
        }
    }

    private void validarCampos(Faturamento faturamento) {
        if (faturamento.getServicosExecutados() == null || faturamento.getServicosExecutados().isBlank()) {
            throw new IllegalArgumentException("A descrição dos serviços executados é obrigatória.");
        }
        if (faturamento.getValorMedicao() == null
                || faturamento.getValorMedicao().signum() <= 0) {
            throw new IllegalArgumentException("O valor da medição deve ser maior que zero.");
        }
    }
}
