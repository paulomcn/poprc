package com.poprc.demo.controller;

import com.poprc.demo.model.PrestacaoContas;
import com.poprc.demo.model.Viagem;
import com.poprc.demo.service.FinanceiroService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/financeiro")
@RequiredArgsConstructor
public class ViagemController {

    private final FinanceiroService financeiroService;

    @PostMapping("/viagens")
    public ResponseEntity<Viagem> criarViagem(@RequestBody NovaViagemDTO dto) {
        Viagem viagem = new Viagem();
        viagem.setSolicitacaoVeiculo(dto.getSolicitacaoVeiculo());
        viagem.setHospedagemDetalhes(dto.getHospedagemDetalhes());
        viagem.setAdiantamentoDiarias(dto.getAdiantamentoDiarias());
        viagem.setCustoPlanejado(dto.getCustoPlanejado());

        Viagem salva = financeiroService.criarViagem(viagem, dto.getFuncionarioId(), dto.getProjetoId());
        return ResponseEntity.status(HttpStatus.CREATED).body(salva);
    }

    @PostMapping("/prestacao-contas")
    public ResponseEntity<PrestacaoContas> fecharPrestacao(@RequestBody PrestacaoContasDTO dto) {
        PrestacaoContas prestacao = financeiroService.fecharPrestacaoContas(
                dto.getViagemId(),
                dto.getCustoReal(),
                dto.getCaminhoNotaFiscal()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(prestacao);
    }

    // DTOs pra isolar a requisição e não engessar o Front
    @Data
    public static class NovaViagemDTO {
        private String solicitacaoVeiculo;
        private String hospedagemDetalhes;
        private BigDecimal adiantamentoDiarias;
        private BigDecimal custoPlanejado;
        private Long funcionarioId;
        private Long projetoId;
    }

    @Data
    public static class PrestacaoContasDTO {
        private Long viagemId;
        private BigDecimal custoReal;
        private String caminhoNotaFiscal;
    }
}