package com.poprc.demo.service;

import com.poprc.demo.model.Contrato;
import com.poprc.demo.model.Faturamento;
import com.poprc.demo.model.SituacaoFaturamento;
import com.poprc.demo.repository.ContratoRepository;
import com.poprc.demo.repository.FaturamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class FaturamentoService {

    private final FaturamentoRepository faturamentoRepository;
    private final ContratoRepository contratoRepository;

    // Regra 1: Registrar Medição (Nasce como A_FATURAR)
    @Transactional
    public Faturamento registrarMedicao(Faturamento faturamento, Long contratoId) {
        Contrato contrato = contratoRepository.findById(contratoId)
                .orElseThrow(() -> new RuntimeException("Contrato não encontrado"));

        faturamento.setContrato(contrato);
        faturamento.setSituacao(SituacaoFaturamento.A_FATURAR);
        return faturamentoRepository.save(faturamento);
    }

    // Regra 2: Emitir Nota Fiscal (Vira FATURADO e ganha vencimento)
    @Transactional
    public Faturamento emitirNotaFiscal(Long id, String numeroNF, LocalDate dataVencimento) {
        Faturamento faturamento = faturamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Faturamento não encontrado"));

        faturamento.setNumeroNotaFiscal(numeroNF);
        faturamento.setSituacao(SituacaoFaturamento.FATURADO);
        faturamento.setDataVencimento(dataVencimento);
        return faturamentoRepository.save(faturamento);
    }

    // Regra 3: Dar Baixa no Pagamento (Vira PAGO)
    @Transactional
    public Faturamento darBaixaPagamento(Long id) {
        Faturamento faturamento = faturamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Faturamento não encontrado"));

        faturamento.setSituacao(SituacaoFaturamento.PAGO);
        return faturamentoRepository.save(faturamento);
    }

    @Transactional(readOnly = true)
    public Faturamento buscarPorId(Long id) {
        return faturamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Faturamento não encontrado"));
    }
}