package com.poprc.demo.controller;

import com.poprc.demo.model.Faturamento;
import com.poprc.demo.model.SituacaoFaturamento;
import com.poprc.demo.repository.FaturamentoRepository;
import com.poprc.demo.repository.ContratoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/faturamentos")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class FaturamentoController {

    private final FaturamentoRepository faturamentoRepository;
    private final ContratoRepository contratoRepository;

    /**
     * 📋 Listar todas as medições/faturamentos
     */
    @GetMapping
    public ResponseEntity<List<Faturamento>> listarTodos() {
        return ResponseEntity.ok(faturamentoRepository.findAll());
    }

    /**
     * 💾 POST: Criar novo faturamento/medição do zero
     */
    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> criarFaturamento(@RequestBody Faturamento faturamento) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (faturamento.getContrato() == null || faturamento.getContrato().getId() == null) {
                response.put("erro", "Contrato vinculado é obrigatório!");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            contratoRepository.findById(faturamento.getContrato().getId())
                    .ifPresent(faturamento::setContrato);

            // ⚡ REMOVIDO O BLOCO QUE TENTAVA FORÇAR "PENDENTE" VIA CÓDIGO E DAVA ERRO

            Faturamento salvo = faturamentoRepository.save(faturamento);
            response.put("faturamento", salvo);
            response.put("mensagem", "Nova medição registrada com sucesso!");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            response.put("erro", "Erro ao criar faturamento: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * ✏️ PUT /{id}: Editar QUALQUER campo do faturamento
     */
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> atualizarFaturamento(@PathVariable Long id,
            @RequestBody Faturamento dados) {
        Map<String, Object> response = new HashMap<>();

        return faturamentoRepository.findById(id)
                .map(faturamento -> {
                    faturamento.setServicosExecutados(dados.getServicosExecutados());
                    faturamento.setValorMedicao(dados.getValorMedicao());
                    faturamento.setDataVencimento(dados.getDataVencimento());
                    faturamento.setNumeroNotaFiscal(dados.getNumeroNotaFiscal());
                    faturamento.setSituacao(dados.getSituacao());

                    if (dados.getContrato() != null && dados.getContrato().getId() != null) {
                        contratoRepository.findById(dados.getContrato().getId())
                                .ifPresent(faturamento::setContrato);
                    }

                    Faturamento salvo = faturamentoRepository.save(faturamento);
                    response.put("faturamento", salvo);
                    response.put("mensagem", "Faturamento atualizado com sucesso!");
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 🧾 PUT: Emitir Nota Fiscal
     */
    @PutMapping("/{id}/emitir-nota")
    @Transactional
    public ResponseEntity<Map<String, Object>> emitirNotaFiscal(
            @PathVariable Long id,
            @RequestParam String numeroNotaFiscal) {

        Map<String, Object> response = new HashMap<>();
        return faturamentoRepository.findById(id)
                .map(faturamento -> {
                    faturamento.setNumeroNotaFiscal(numeroNotaFiscal);
                    faturamento.setSituacao(SituacaoFaturamento.FATURADO); // 💥 CORRIGIDO!

                    Faturamento salvo = faturamentoRepository.save(faturamento);
                    response.put("faturamento", salvo);
                    response.put("mensagem", "Nota Fiscal emitida e registrada no banco!");
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 💰 PUT: Baixar Pagamento
     */
    @PutMapping("/{id}/baixar-pagamento")
    @Transactional
    public ResponseEntity<Map<String, Object>> baixarPagamento(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        return faturamentoRepository.findById(id)
                .map(faturamento -> {
                    faturamento.setSituacao(SituacaoFaturamento.PAGO); // 💥 CORRIGIDO!

                    Faturamento salvo = faturamentoRepository.save(faturamento);
                    response.put("faturamento", salvo);
                    response.put("mensagem", "Pagamento baixado com sucesso! Saldo atualizado.");
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}