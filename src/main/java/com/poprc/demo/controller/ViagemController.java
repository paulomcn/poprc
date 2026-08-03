package com.poprc.demo.controller;

import com.poprc.demo.model.PrestacaoContas;
import com.poprc.demo.model.Viagem;
import com.poprc.demo.repository.ViagemRepository;
import com.poprc.demo.repository.FuncionarioRepository; // 💥 Import novo
import com.poprc.demo.repository.ProjetoRepository; // 💥 Import novo
import com.poprc.demo.service.FinanceiroService;
import com.poprc.demo.service.ComprovantePrestacaoService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional; // 💥 Import novo
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/financeiro")
@RequiredArgsConstructor
public class ViagemController {

    private final FinanceiroService financeiroService;
    private final ViagemRepository viagemRepository;
    private final FuncionarioRepository funcionarioRepository; // 💥 Injetado para o update
    private final ProjetoRepository projetoRepository; // 💥 Injetado para o update
    private final ComprovantePrestacaoService comprovantePrestacaoService;

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
                    if (viagem.getStatus() == com.poprc.demo.model.StatusViagem.CONCLUIDA) {
                        throw new IllegalStateException("Uma viagem concluída não pode mais ser editada.");
                    }
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
    @PostMapping(value = "/prestacao-contas", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PrestacaoContas> fecharPrestacao(
            @RequestParam Long viagemId,
            @RequestParam BigDecimal custoReal,
            @RequestParam("comprovante") MultipartFile comprovante) {
        PrestacaoContas prestacao = comprovantePrestacaoService.fecharPrestacao(
                viagemId, custoReal, comprovante);
        return ResponseEntity.status(HttpStatus.CREATED).body(prestacao);
    }

    @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
    public ResponseEntity<Map<String, String>> tratarErroDeRegra(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("erro", ex.getMessage()));
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

}
