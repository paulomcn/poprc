package com.poprc.demo.controller;

import com.poprc.demo.model.PrestacaoContas;
import com.poprc.demo.model.Viagem;
import com.poprc.demo.repository.ViagemRepository;
import com.poprc.demo.repository.FuncionarioRepository; // 💥 Import novo
import com.poprc.demo.repository.ProjetoRepository; // 💥 Import novo
import com.poprc.demo.service.FinanceiroService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional; // 💥 Import novo
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/financeiro")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ViagemController {

    private final FinanceiroService financeiroService;
    private final ViagemRepository viagemRepository;
    private final FuncionarioRepository funcionarioRepository; // 💥 Injetado para o update
    private final ProjetoRepository projetoRepository; // 💥 Injetado para o update

    /**
     * 📋 Listar todas as viagens cadastradas
     */
    @GetMapping("/viagens")
    public ResponseEntity<List<Viagem>> listarViagens() {
        return ResponseEntity.ok(viagemRepository.findAll());
    }

    /**
     * ✈️ POST: Lançar nova viagem do zero
     */
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

    /**
     * ✏️ PUT: Editar uma ordem de viagem existente (CRUD Completo) 💥
     */
    @PutMapping("/viagens/{id}")
    @Transactional
    public ResponseEntity<Viagem> atualizarViagem(@PathVariable Long id, @RequestBody NovaViagemDTO dto) {
        return viagemRepository.findById(id)
                .map(viagem -> {
                    viagem.setSolicitacaoVeiculo(dto.getSolicitacaoVeiculo());
                    viagem.setHospedagemDetalhes(dto.getHospedagemDetalhes());
                    viagem.setAdiantamentoDiarias(dto.getAdiantamentoDiarias());
                    viagem.setCustoPlanejado(dto.getCustoPlanejado());

                    if (dto.getFuncionarioId() != null) {
                        funcionarioRepository.findById(dto.getFuncionarioId()).ifPresent(viagem::setFuncionario);
                    }
                    if (dto.getProjetoId() != null) {
                        projetoRepository.findById(dto.getProjetoId()).ifPresent(viagem::setProjeto);
                    }

                    Viagem atualizada = viagemRepository.save(viagem);
                    return ResponseEntity.ok(atualizada);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 💰 POST: Prestar contas e liquidar custos
     */
    @PostMapping("/prestacao-contas")
    public ResponseEntity<PrestacaoContas> fecharPrestacao(@RequestBody PrestacaoContasDTO dto) {
        PrestacaoContas prestacao = financeiroService.fecharPrestacaoContas(
                dto.getViagemId(),
                dto.getCustoReal(),
                dto.getCaminhoNotaFiscal());
        return ResponseEntity.status(HttpStatus.CREATED).body(prestacao);
    }

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