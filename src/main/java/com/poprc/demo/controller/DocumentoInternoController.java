package com.poprc.demo.controller;

import com.poprc.demo.model.Comarca;
import com.poprc.demo.model.DocumentoInterno;
import com.poprc.demo.repository.ComarcaRepository;
import com.poprc.demo.repository.DocumentoInternoRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@RestController
@RequestMapping("/api/documentos-internos")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DocumentoInternoController {

    private static final String TIPO_VISTORIA = "VISTORIA_OS";
    private static final String STATUS_PENDENTE = "PENDENTE_ASSINATURA";
    private static final String STATUS_REGISTRADO = "REGISTRADO";

    private final DocumentoInternoRepository documentoInternoRepository;
    private final ComarcaRepository comarcaRepository;

    @GetMapping("/comarca/{comarcaId}")
    public ResponseEntity<List<DocumentoInterno>> listarPorComarca(
            @PathVariable Long comarcaId,
            @AuthenticationPrincipal OAuth2User principal,
            @RequestHeader(value = "X-Usuario-Atual", required = false) String usuarioHeader) {
        String usuarioAtual = usuarioAtual(principal, usuarioHeader);
        List<DocumentoInterno> documentos = documentoInternoRepository
                .findByComarcaIdAndTipoOrderByDataGeracaoDesc(comarcaId, TIPO_VISTORIA)
                .stream()
                .filter(documento -> podeVisualizar(documento, usuarioAtual))
                .toList();
        return ResponseEntity.ok(documentos);
    }

    @PostMapping("/vistoria")
    public ResponseEntity<DocumentoInterno> gerarDocumentoVistoria(
            @RequestBody DocumentoVistoriaRequest request,
            @AuthenticationPrincipal OAuth2User principal,
            @RequestHeader(value = "X-Usuario-Atual", required = false) String usuarioHeader) {
        Comarca comarca = comarcaRepository.findById(request.getComarcaId())
                .orElseThrow(() -> new IllegalArgumentException("Comarca não encontrada."));

        DocumentoInterno documento = new DocumentoInterno();
        documento.setTipo(TIPO_VISTORIA);
        documento.setStatus(STATUS_PENDENTE);
        documento.setComarca(comarca);
        documento.setConteudoJson(request.getConteudoJson());
        documento.setCriadoPor(usuarioAtual(principal, usuarioHeader));
        documento.setRecebidoPor(normalizarRecebedor(request.getRecebidoPor()));
        documento.setDataGeracao(LocalDateTime.now());
        return ResponseEntity.ok(documentoInternoRepository.save(documento));
    }

    @PatchMapping("/{id}/assinar")
    public ResponseEntity<DocumentoInterno> assinarDocumento(
            @PathVariable Long id,
            @RequestBody AssinaturaDocumentoRequest request,
            @AuthenticationPrincipal OAuth2User principal,
            @RequestHeader(value = "X-Usuario-Atual", required = false) String usuarioHeader) {
        String usuarioAtual = usuarioAtual(principal, usuarioHeader);
        DocumentoInterno documento = documentoInternoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado."));
        if (!podeVisualizar(documento, usuarioAtual)) {
            return ResponseEntity.status(403).build();
        }
        if (request.getAssinaturaBase64() == null || request.getAssinaturaBase64().isBlank()) {
            throw new IllegalArgumentException("Assinatura digital é obrigatória para registrar o documento.");
        }

        documento.setAssinaturaBase64(request.getAssinaturaBase64());
        documento.setStatus(STATUS_REGISTRADO);
        documento.setDataAssinatura(LocalDateTime.now());
        documento.setHashRegistro(gerarHash(documento));
        return ResponseEntity.ok(documentoInternoRepository.save(documento));
    }

    private String usuarioAtual(OAuth2User principal, String usuarioHeader) {
        if (principal != null) {
            Object email = principal.getAttribute("email");
            Object nome = principal.getAttribute("nome");
            Object fullName = principal.getAttribute("Full_Name");
            Object id = principal.getAttribute("id");
            if (email != null) return email.toString();
            if (nome != null) return nome.toString();
            if (fullName != null) return fullName.toString();
            if (id != null) return id.toString();
        }
        return usuarioHeader != null && !usuarioHeader.isBlank() ? usuarioHeader.trim() : "Sistema";
    }

    private String normalizarRecebedor(String recebidoPor) {
        return recebidoPor != null && !recebidoPor.isBlank() ? recebidoPor.trim() : "Responsável da Unidade";
    }

    private boolean podeVisualizar(DocumentoInterno documento, String usuarioAtual) {
        if ("Sistema".equals(usuarioAtual)) {
            return true;
        }
        return usuarioAtual.equalsIgnoreCase(documento.getCriadoPor())
                || usuarioAtual.equalsIgnoreCase(documento.getRecebidoPor());
    }

    private String gerarHash(DocumentoInterno documento) {
        try {
            String base = String.join("|",
                    String.valueOf(documento.getId()),
                    String.valueOf(documento.getTipo()),
                    String.valueOf(documento.getConteudoJson()),
                    String.valueOf(documento.getCriadoPor()),
                    String.valueOf(documento.getRecebidoPor()),
                    String.valueOf(documento.getDataGeracao()),
                    String.valueOf(documento.getDataAssinatura()),
                    String.valueOf(documento.getAssinaturaBase64()));
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(base.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível gerar o hash do documento.", e);
        }
    }

    @Data
    public static class DocumentoVistoriaRequest {
        private Long comarcaId;
        private String conteudoJson;
        private String recebidoPor;
    }

    @Data
    public static class AssinaturaDocumentoRequest {
        private String assinaturaBase64;
    }
}
