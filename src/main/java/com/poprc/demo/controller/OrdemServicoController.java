package com.poprc.demo.controller;

import com.poprc.demo.dto.CriarOrdemServicoRequest;
import com.poprc.demo.exception.SaldoInsuficienteException;
import com.poprc.demo.model.OrdemServico;
import com.poprc.demo.model.StatusOS;
import com.poprc.demo.service.OrdemServicoService;
import com.poprc.demo.repository.OrdemServicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*; // 💥 Import simplificado para pegar o CrossOrigin

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/ordens-servico")
@CrossOrigin(origins = "*") // 🛡️ Blindagem total contra erro de CORS no front
@RequiredArgsConstructor
public class OrdemServicoController {

    private final OrdemServicoService ordemServicoService;
    private final OrdemServicoRepository ordemServicoRepository;

    /**
     * 🛠️ POST: Criar nova Ordem de Serviço amarrada ao contrato
     */
    @PostMapping
    public ResponseEntity<OrdemServico> criar(@RequestBody CriarOrdemServicoRequest request) {
        OrdemServico novaOrdem = ordemServicoService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(novaOrdem);
    }

    @PostMapping("/reparar-vinculos-comarcas")
    public ResponseEntity<Map<String, Object>> repararVinculosComarcas() {
        return ResponseEntity.ok(ordemServicoService.repararVinculosComarcas());
    }

    /**
     * 🔄 PUT: Transicionar os cards entre as colunas do Kanban
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<OrdemServico> atualizarStatus(@PathVariable Long id,
            @RequestBody StatusUpdateRequest request) {
        OrdemServico ordem = ordemServicoService.atualizarStatus(id, request.getStatus());
        if (ordem != null) {
            return ResponseEntity.ok(ordem);
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/checklist")
    public ResponseEntity<OrdemServico> atualizarChecklist(@PathVariable Long id,
            @RequestBody ChecklistUpdateRequest request) {
        OrdemServico ordem = ordemServicoService.atualizarChecklist(id, request.getChecklist());
        if (ordem != null) {
            return ResponseEntity.ok(ordem);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<List<OrdemServico>> listarTodas(
            @RequestParam(required = false) String numeroOs,
            @RequestParam(required = false) String cliente) {
        // Adeus findAll() genérico! Agora a busca vai blindada e cirúrgica
        List<OrdemServico> ordens = ordemServicoRepository.buscarComFiltros(numeroOs, cliente);
        return ResponseEntity.ok(ordens);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdemServico> buscarPorId(@PathVariable Long id) {
        return ordemServicoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public static class StatusUpdateRequest {
        private StatusOS status;

        public StatusOS getStatus() {
            return status;
        }

        public void setStatus(StatusOS status) {
            this.status = status;
        }
    }

    public static class ChecklistUpdateRequest {
        private String checklist;

        public String getChecklist() {
            return checklist;
        }

        public void setChecklist(String checklist) {
            this.checklist = checklist;
        }
    }

    @ExceptionHandler({ IllegalArgumentException.class, SaldoInsuficienteException.class })
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("erro", ex.getMessage()));
    }
}
