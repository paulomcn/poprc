package com.poprc.demo.controller;

import com.poprc.demo.exception.ArquivoInvalidoException;
import com.poprc.demo.exception.SaldoInsuficienteException;
import com.poprc.demo.model.Comarca;
import com.poprc.demo.model.MaterialItem;
import com.poprc.demo.service.ComarcaService;
import com.poprc.demo.repository.ComarcaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @PostMapping("/{id}/vistoria/foto")
    public ResponseEntity<Comarca> salvarFotoVistoria(
            @PathVariable Long id,
            @RequestParam("foto") MultipartFile foto) {
        Comarca comarca = comarcaService.salvarFotoVistoria(id, foto);
        return ResponseEntity.ok(comarca);
    }

    @PatchMapping("/{id}/vistoria/assinatura")
    public ResponseEntity<Comarca> salvarAssinaturaVistoria(
            @PathVariable Long id,
            @RequestBody AssinaturaVistoriaRequest request) {
        Comarca comarca = comarcaService.salvarAssinaturaVistoria(id, request.getAssinaturaBase64());
        return ResponseEntity.ok(comarca);
    }

    @PatchMapping("/{id}/avancar-etapa")
    public ResponseEntity<Comarca> avancarEtapa(@PathVariable Long id) {
        Comarca comarca = comarcaService.avancarParaInfraestrutura(id);
        return ResponseEntity.ok(comarca);
    }

    @GetMapping("/{id}/auditoria")
    public ResponseEntity<Map<String, Object>> buscarAuditoria(@PathVariable Long id) {
        return ResponseEntity.ok(comarcaService.buscarAuditoriaPorComarca(id));
    }

    @GetMapping("/os/{numeroOs}/auditoria")
    public ResponseEntity<Map<String, Object>> buscarAuditoriaPorOs(@PathVariable String numeroOs) {
        return ResponseEntity.ok(comarcaService.buscarAuditoriaPorNumeroOs(numeroOs));
    }

    @PutMapping("/materiais/{materialId}/auditoria")
    public ResponseEntity<Map<String, Object>> atualizarQuantidadeAuditada(
            @PathVariable Long materialId,
            @RequestParam Integer quantidadeAuditada) {
        return ResponseEntity.ok(comarcaService.atualizarQuantidadeAuditada(materialId, quantidadeAuditada));
    }

    @PatchMapping("/{id}/as-built/homologar")
    public ResponseEntity<Map<String, Object>> homologarAsBuilt(@PathVariable Long id) {
        return ResponseEntity.ok(comarcaService.homologarAsBuilt(id));
    }

    @GetMapping("/{id}/materiais-previstos")
    public ResponseEntity<List<MaterialItem>> listarMateriaisPrevistos(@PathVariable Long id) {
        return ResponseEntity.ok(comarcaService.listarMateriaisPrevistos(id));
    }

    @PostMapping("/{id}/materiais-previstos")
    public ResponseEntity<Comarca> adicionarMaterialPrevisto(
            @PathVariable Long id,
            @RequestBody MaterialPrevistoRequest request) {
        return ResponseEntity.ok(comarcaService.adicionarMaterialPrevisto(
                id,
                request.getMaterialId(),
                request.getNomeMaterial(),
                request.getQuantidadePrevista()));
    }

    @PutMapping("/materiais-previstos/{materialId}")
    public ResponseEntity<Comarca> atualizarMaterialPrevisto(
            @PathVariable Long materialId,
            @RequestBody MaterialPrevistoRequest request) {
        return ResponseEntity.ok(comarcaService.atualizarMaterialPrevisto(
                materialId,
                request.getMaterialId(),
                request.getNomeMaterial(),
                request.getQuantidadePrevista()));
    }

    @DeleteMapping("/materiais-previstos/{materialId}")
    public ResponseEntity<Comarca> removerMaterialPrevisto(@PathVariable Long materialId) {
        return ResponseEntity.ok(comarcaService.removerMaterialPrevisto(materialId));
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

    public static class AssinaturaVistoriaRequest {
        private String assinaturaBase64;

        public String getAssinaturaBase64() {
            return assinaturaBase64;
        }

        public void setAssinaturaBase64(String assinaturaBase64) {
            this.assinaturaBase64 = assinaturaBase64;
        }
    }

    public static class MaterialPrevistoRequest {
        private Long materialId;
        private String nomeMaterial;
        private Integer quantidadePrevista;

        public Long getMaterialId() {
            return materialId;
        }

        public void setMaterialId(Long materialId) {
            this.materialId = materialId;
        }

        public String getNomeMaterial() {
            return nomeMaterial;
        }

        public void setNomeMaterial(String nomeMaterial) {
            this.nomeMaterial = nomeMaterial;
        }

        public Integer getQuantidadePrevista() {
            return quantidadePrevista;
        }

        public void setQuantidadePrevista(Integer quantidadePrevista) {
            this.quantidadePrevista = quantidadePrevista;
        }
    }

    @ExceptionHandler({ ArquivoInvalidoException.class, IllegalArgumentException.class, SaldoInsuficienteException.class })
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("erro", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
