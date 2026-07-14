package com.poprc.demo.controller;

import com.poprc.demo.model.Comarca;
import com.poprc.demo.model.DocumentoInterno;
import com.poprc.demo.model.DocumentoAssinaturaLog;
import com.poprc.demo.repository.ComarcaRepository;
import com.poprc.demo.repository.DocumentoAssinaturaLogRepository;
import com.poprc.demo.repository.DocumentoInternoRepository;
import com.poprc.demo.service.DocumentoPdfService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/documentos-internos")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DocumentoInternoController {

    private static final String TIPO_VISTORIA_LEGADO = "VISTORIA_OS";
    private static final String TIPO_VISTORIA_INICIAL = "VISTORIA_INICIAL_OS";
    private static final String TIPO_ENCERRAMENTO = "ENCERRAMENTO_OS";
    private static final String STATUS_PENDENTE = "PENDENTE_ASSINATURA";
    private static final String STATUS_PARCIAL = "PARCIALMENTE_ASSINADO";
    private static final String STATUS_REGISTRADO = "REGISTRADO";

    private final DocumentoInternoRepository documentoInternoRepository;
    private final ComarcaRepository comarcaRepository;
    private final DocumentoAssinaturaLogRepository assinaturaLogRepository;
    private final DocumentoPdfService documentoPdfService;

    @GetMapping("/comarca/{comarcaId}")
    public ResponseEntity<List<DocumentoInterno>> listarPorComarca(
            @PathVariable Long comarcaId,
            @AuthenticationPrincipal OAuth2User principal,
            @RequestHeader(value = "X-Usuario-Atual", required = false) String usuarioHeader) {
        String usuarioAtual = usuarioAtual(principal, usuarioHeader);
        List<DocumentoInterno> documentos = documentoInternoRepository
                .findByComarcaIdOrderByDataGeracaoDesc(comarcaId)
                .stream()
                .filter(documento -> tipoDocumentoValido(documento.getTipo()))
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
        documento.setTipo(normalizarTipoDocumento(request.getTipo()));
        documento.setStatus(STATUS_PENDENTE);
        documento.setComarca(comarca);
        documento.setConteudoJson(request.getConteudoJson());
        documento.setCriadoPor(usuarioAtual(principal, usuarioHeader));
        documento.setRecebidoPor(normalizarRecebedor(request.getRecebidoPor()));
        documento.setDataGeracao(LocalDateTime.now());
        return ResponseEntity.ok(documentoInternoRepository.save(documento));
    }

    @PutMapping("/{id}/conteudo")
    public ResponseEntity<DocumentoInterno> atualizarConteudoDocumento(
            @PathVariable Long id,
            @RequestBody DocumentoVistoriaRequest request,
            @AuthenticationPrincipal OAuth2User principal,
            @RequestHeader(value = "X-Usuario-Atual", required = false) String usuarioHeader) {
        DocumentoInterno documento = documentoInternoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado."));
        if (!podeVisualizar(documento, usuarioAtual(principal, usuarioHeader))) {
            return ResponseEntity.status(403).build();
        }
        if (!STATUS_PENDENTE.equals(documento.getStatus())
                || temTexto(documento.getAssinaturaTecnicoBase64())
                || temTexto(documento.getAssinaturaGestorBase64())
                || temTexto(documento.getAssinaturaGerenteBase64())) {
            throw new IllegalStateException(
                    "Documento assinado não pode ser alterado. Crie uma nova versão para modificar o conteúdo.");
        }
        if (request.getConteudoJson() == null || request.getConteudoJson().isBlank()) {
            throw new IllegalArgumentException("O conteúdo do documento é obrigatório.");
        }
        documento.setConteudoJson(request.getConteudoJson());
        documento.setRecebidoPor(normalizarRecebedor(request.getRecebidoPor()));
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
        if (STATUS_REGISTRADO.equals(documento.getStatus())) {
            throw new IllegalStateException("Documento finalizado não pode receber novas assinaturas.");
        }
        validarAssinatura(request.getAssinaturaBase64());

        documento.setAssinaturaBase64(request.getAssinaturaBase64());
        documento.setStatus(STATUS_REGISTRADO);
        documento.setDataAssinatura(LocalDateTime.now());
        documento.setHashRegistro(gerarHash(documento));
        DocumentoInterno salvo = documentoInternoRepository.save(documento);
        registrarLogAssinatura(salvo, "LEGADO", usuarioAtual, usuarioAtual, request.getAssinaturaBase64());
        return ResponseEntity.ok(documentoPdfService.arquivarPdf(salvo));
    }

    @PatchMapping("/{id}/assinaturas/{papel}")
    public ResponseEntity<DocumentoInterno> assinarDocumentoPorPapel(
            @PathVariable Long id,
            @PathVariable String papel,
            @RequestBody AssinaturaPapelRequest request,
            @AuthenticationPrincipal OAuth2User principal,
            @RequestHeader(value = "X-Usuario-Atual", required = false) String usuarioHeader) {
        String usuarioAtual = usuarioAtual(principal, usuarioHeader);
        DocumentoInterno documento = documentoInternoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado."));
        if (!podeVisualizar(documento, usuarioAtual)) {
            return ResponseEntity.status(403).build();
        }
        if (STATUS_REGISTRADO.equals(documento.getStatus())) {
            throw new IllegalStateException("Documento finalizado não pode receber novas assinaturas.");
        }
        validarAssinatura(request.getAssinaturaBase64());

        String assinadoPor = request.getNomeAssinante() != null && !request.getNomeAssinante().isBlank()
                ? request.getNomeAssinante().trim()
                : usuarioAtual;
        LocalDateTime agora = LocalDateTime.now();
        String papelNormalizado = papel.toUpperCase();
        switch (papelNormalizado) {
            case "TECNICO" -> {
                validarPapelPendente(documento.getAssinaturaTecnicoBase64(), "Técnico");
                documento.setAssinaturaTecnicoBase64(request.getAssinaturaBase64());
                documento.setTecnicoAssinadoPor(assinadoPor);
                documento.setDataAssinaturaTecnico(agora);
            }
            case "GESTOR_RC" -> {
                validarPapelPendente(documento.getAssinaturaGestorBase64(), "Gestor RC");
                documento.setAssinaturaGestorBase64(request.getAssinaturaBase64());
                documento.setGestorAssinadoPor(assinadoPor);
                documento.setDataAssinaturaGestor(agora);
            }
            case "GERENTE_FORUM" -> {
                validarPapelPendente(documento.getAssinaturaGerenteBase64(), "Gerente do Fórum");
                documento.setAssinaturaGerenteBase64(request.getAssinaturaBase64());
                documento.setGerenteAssinadoPor(assinadoPor);
                documento.setDataAssinaturaGerente(agora);
            }
            default -> throw new IllegalArgumentException("Papel de assinatura inválido.");
        }

        boolean todasAssinaturas = temTexto(documento.getAssinaturaTecnicoBase64())
                && temTexto(documento.getAssinaturaGestorBase64())
                && temTexto(documento.getAssinaturaGerenteBase64());
        documento.setStatus(todasAssinaturas ? STATUS_REGISTRADO : STATUS_PARCIAL);
        documento.setDataAssinatura(agora);
        documento.setHashRegistro(gerarHash(documento));
        DocumentoInterno salvo = documentoInternoRepository.save(documento);
        registrarLogAssinatura(salvo, papelNormalizado, assinadoPor, usuarioAtual, request.getAssinaturaBase64());
        if (STATUS_REGISTRADO.equals(salvo.getStatus())) {
            salvo = documentoPdfService.arquivarPdf(salvo);
        }
        return ResponseEntity.ok(salvo);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> obterPdf(
            @PathVariable Long id,
            @AuthenticationPrincipal OAuth2User principal,
            @RequestHeader(value = "X-Usuario-Atual", required = false) String usuarioHeader) {
        DocumentoInterno documento = documentoInternoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado."));
        if (!podeVisualizar(documento, usuarioAtual(principal, usuarioHeader))) {
            return ResponseEntity.status(403).build();
        }
        byte[] pdf = documentoPdfService.obterPdf(documento);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=documento-os-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }

    @GetMapping("/{id}/assinaturas/log")
    public ResponseEntity<List<AssinaturaLogResponse>> listarLogAssinaturas(
            @PathVariable Long id,
            @AuthenticationPrincipal OAuth2User principal,
            @RequestHeader(value = "X-Usuario-Atual", required = false) String usuarioHeader) {
        DocumentoInterno documento = documentoInternoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado."));
        if (!podeVisualizar(documento, usuarioAtual(principal, usuarioHeader))) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(assinaturaLogRepository.findByDocumentoIdOrderByRegistradoEmAsc(id).stream()
                .map(AssinaturaLogResponse::from)
                .toList());
    }

    @GetMapping("/{id}/integridade")
    public ResponseEntity<Map<String, Object>> verificarIntegridade(
            @PathVariable Long id,
            @AuthenticationPrincipal OAuth2User principal,
            @RequestHeader(value = "X-Usuario-Atual", required = false) String usuarioHeader) {
        DocumentoInterno documento = documentoInternoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado."));
        if (!podeVisualizar(documento, usuarioAtual(principal, usuarioHeader))) {
            return ResponseEntity.status(403).build();
        }
        String hashCalculado = gerarHash(documento);
        boolean integro = documento.getHashRegistro() != null && documento.getHashRegistro().equals(hashCalculado);
        return ResponseEntity.ok(Map.of(
                "documentoId", documento.getId(),
                "integro", integro,
                "hashRegistrado", documento.getHashRegistro() == null ? "" : documento.getHashRegistro(),
                "hashCalculado", hashCalculado));
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

    private String normalizarTipoDocumento(String tipo) {
        if (TIPO_ENCERRAMENTO.equals(tipo)) {
            return TIPO_ENCERRAMENTO;
        }
        return TIPO_VISTORIA_INICIAL;
    }

    private boolean tipoDocumentoValido(String tipo) {
        return TIPO_VISTORIA_LEGADO.equals(tipo)
                || TIPO_VISTORIA_INICIAL.equals(tipo)
                || TIPO_ENCERRAMENTO.equals(tipo);
    }

    private boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private void validarPapelPendente(String assinaturaExistente, String papel) {
        if (temTexto(assinaturaExistente)) {
            throw new IllegalStateException("A assinatura de " + papel + " já foi registrada e não pode ser substituída.");
        }
    }

    private void validarAssinatura(String assinaturaBase64) {
        String prefixo = "data:image/png;base64,";
        if (assinaturaBase64 == null || !assinaturaBase64.startsWith(prefixo)) {
            throw new IllegalArgumentException("A assinatura deve ser uma imagem PNG em Base64.");
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(assinaturaBase64.substring(prefixo.length()));
            if (bytes.length == 0 || bytes.length > 1024 * 1024) {
                throw new IllegalArgumentException("A assinatura deve ter no máximo 1 MB.");
            }
            if (bytes.length < 8
                    || (bytes[0] & 0xff) != 0x89
                    || bytes[1] != 0x50
                    || bytes[2] != 0x4e
                    || bytes[3] != 0x47) {
                throw new IllegalArgumentException("O conteúdo da assinatura não é um PNG válido.");
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Assinatura Base64 inválida.", ex);
        }
    }

    private void registrarLogAssinatura(
            DocumentoInterno documento,
            String papel,
            String nomeAssinante,
            String registradoPor,
            String assinaturaBase64) {
        DocumentoAssinaturaLog log = new DocumentoAssinaturaLog();
        log.setDocumento(documento);
        log.setPapel(papel);
        log.setNomeAssinante(nomeAssinante);
        log.setRegistradoPor(registradoPor);
        log.setRegistradoEm(LocalDateTime.now());
        log.setHashAssinatura(gerarSha256(assinaturaBase64));
        log.setHashDocumento(documento.getHashRegistro());
        assinaturaLogRepository.save(log);
    }

    private String gerarSha256(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível gerar o hash da assinatura.", e);
        }
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
                    String.valueOf(documento.getAssinaturaBase64()),
                    String.valueOf(documento.getAssinaturaTecnicoBase64()),
                    String.valueOf(documento.getTecnicoAssinadoPor()),
                    String.valueOf(documento.getDataAssinaturaTecnico()),
                    String.valueOf(documento.getAssinaturaGestorBase64()),
                    String.valueOf(documento.getGestorAssinadoPor()),
                    String.valueOf(documento.getDataAssinaturaGestor()),
                    String.valueOf(documento.getAssinaturaGerenteBase64()),
                    String.valueOf(documento.getGerenteAssinadoPor()),
                    String.valueOf(documento.getDataAssinaturaGerente()));
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(base.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível gerar o hash do documento.", e);
        }
    }

    @Data
    public static class DocumentoVistoriaRequest {
        private Long comarcaId;
        private String tipo;
        private String conteudoJson;
        private String recebidoPor;
    }

    @Data
    public static class AssinaturaDocumentoRequest {
        private String assinaturaBase64;
    }

    @Data
    public static class AssinaturaPapelRequest {
        private String assinaturaBase64;
        private String nomeAssinante;
    }

    public record AssinaturaLogResponse(
            Long id,
            String papel,
            String nomeAssinante,
            String registradoPor,
            LocalDateTime registradoEm,
            String hashAssinatura,
            String hashDocumento) {
        static AssinaturaLogResponse from(DocumentoAssinaturaLog log) {
            return new AssinaturaLogResponse(
                    log.getId(), log.getPapel(), log.getNomeAssinante(), log.getRegistradoPor(),
                    log.getRegistradoEm(), log.getHashAssinatura(), log.getHashDocumento());
        }
    }
}
