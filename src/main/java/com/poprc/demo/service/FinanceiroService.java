package com.poprc.demo.service;

import com.poprc.demo.model.*;
import com.poprc.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class FinanceiroService {

    private final ViagemRepository viagemRepository;
    private final PrestacaoContasRepository prestacaoContasRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final ProjetoRepository projetoRepository;

    @Transactional
    public Viagem criarViagem(Viagem viagem, Long funcionarioId, Long projetoId) {
        Funcionario viajante = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new RuntimeException("Viajante não encontrado"));
        Projeto projeto = projetoRepository.findById(projetoId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));

        viagem.setFuncionario(viajante);
        viagem.setProjeto(projeto);
        if (viagem.getStatus() == null) {
            viagem.setStatus(StatusViagem.PLANEJADA);
        }
        return viagemRepository.save(viagem);
    }

    @Transactional
    public PrestacaoContas fecharPrestacaoContas(Long viagemId, BigDecimal custoReal, String pathNota) {
        Viagem viagem = viagemRepository.findById(viagemId)
                .orElseThrow(() -> new RuntimeException("Viagem não encontrada"));

        // Regra de ouro: Se estourou o planejado, toma a flag de EXCEDIDO na cara
        String statusCalculado = custoReal.compareTo(viagem.getCustoPlanejado()) > 0 
                ? "EXCEDIDO" 
                : "DENTRO_DO_ORCAMENTO";

        PrestacaoContas prestacao = new PrestacaoContas();
        prestacao.setViagem(viagem);
        prestacao.setCustoReal(custoReal);
        prestacao.setCaminhoNotaFiscal(pathNota);
        prestacao.setStatus(statusCalculado);

        // Atualiza a viagem automática para o fechamento
        viagem.setStatus(StatusViagem.CONCLUIDA);
        viagemRepository.save(viagem);

        return prestacaoContasRepository.save(prestacao);
    }
}