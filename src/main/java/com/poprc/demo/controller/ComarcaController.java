package com.poprc.demo.controller;

import com.poprc.demo.model.Comarca;
import com.poprc.demo.service.ComarcaService;
import com.poprc.demo.repository.ComarcaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/comarcas")
@RequiredArgsConstructor
public class ComarcaController {

    private final ComarcaService comarcaService;
    private final ComarcaRepository comarcaRepository;

    @GetMapping
    public ResponseEntity<List<Comarca>> listarTodas() {
        List<Comarca> comarcas = comarcaRepository.findAll();
        return ResponseEntity.ok(comarcas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Comarca> obterPorId(@PathVariable Long id) {
        Optional<Comarca> comarca = comarcaService.obterPorId(id);
        return comarca.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/progresso")
    public ResponseEntity<Comarca> atualizarProgresso(@PathVariable Long id, @RequestBody ProgressoRequest request) {
        Comarca comarca = comarcaService.atualizarProgresso(id, request.getPercentualConcluido(),
                request.getSituacao());
        if (comarca != null) {
            return ResponseEntity.ok(comarca);
        }
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/{id}/pendencias")
    public ResponseEntity<Comarca> atualizarPendencias(@PathVariable Long id, @RequestBody PendenciasRequest request) {
        Comarca comarca = comarcaService.atualizarPendencias(id, request.getPendencias());
        if (comarca != null) {
            return ResponseEntity.ok(comarca);
        }
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Comarca> atualizar(@PathVariable Long id, @RequestBody AtualizacaoComarcaRequest request) {
        Comarca comarca = comarcaService.atualizarComarca(id, request.getPercentualConcluido(),
                request.getPendencias());
        if (comarca != null) {
            return ResponseEntity.ok(comarca);
        }
        return ResponseEntity.notFound().build();
    }

    public static class AtualizacaoComarcaRequest {
        private java.math.BigDecimal percentualConcluido;
        private String pendencias;

        public java.math.BigDecimal getPercentualConcluido() {
            return percentualConcluido;
        }

        public void setPercentualConcluido(java.math.BigDecimal percentualConcluido) {
            this.percentualConcluido = percentualConcluido;
        }

        public String getPendencias() {
            return pendencias;
        }

        public void setPendencias(String pendencias) {
            this.pendencias = pendencias;
        }
    }

    public static class ProgressoRequest {
        private java.math.BigDecimal percentualConcluido;
        private String situacao;

        public java.math.BigDecimal getPercentualConcluido() {
            return percentualConcluido;
        }

        public void setPercentualConcluido(java.math.BigDecimal percentualConcluido) {
            this.percentualConcluido = percentualConcluido;
        }

        public String getSituacao() {
            return situacao;
        }

        public void setSituacao(String situacao) {
            this.situacao = situacao;
        }
    }

    public static class PendenciasRequest {
        private String pendencias;

        public String getPendencias() {
            return pendencias;
        }

        public void setPendencias(String pendencias) {
            this.pendencias = pendencias;
        }
    }
}
