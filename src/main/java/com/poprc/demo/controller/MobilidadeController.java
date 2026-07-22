package com.poprc.demo.controller;

import com.poprc.demo.model.EvidenciaFoto;
import com.poprc.demo.model.RegistroPonto;
import com.poprc.demo.model.TipoPonto; // Mudou aqui
import com.poprc.demo.service.FotoService;
import com.poprc.demo.service.PontoService;
import com.poprc.demo.security.UsuarioAutenticado;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/campo")
@RequiredArgsConstructor
public class MobilidadeController {

    private final PontoService pontoService;
    private final FotoService fotoService;

    @PostMapping("/ponto")
    public ResponseEntity<RegistroPonto> registrarPonto(
            @RequestBody PontoRequestDTO request,
            Authentication authentication) {
        validarOperacaoPropria(authentication, request.getFuncionarioId());
        RegistroPonto novoPonto = pontoService.salvarPonto(
                request.getFuncionarioId(),
                request.getTipo(),
                request.getLatitude(),
                request.getLongitude()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(novoPonto);
    }

    @GetMapping("/ponto/funcionario/{funcionarioId}/ultimo")
    public ResponseEntity<RegistroPonto> obterUltimoPonto(
            @PathVariable Long funcionarioId,
            Authentication authentication) {
        validarConsultaFuncionario(authentication, funcionarioId);
        return pontoService.obterUltimoPonto(funcionarioId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/upload-foto")
    public ResponseEntity<EvidenciaFotoResponse> uploadFoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam("ordemServicoId") Long ordemServicoId,
            @RequestParam("funcionarioId") Long funcionarioId,
            @RequestParam("latitude") String latitude,
            @RequestParam("longitude") String longitude,
            Authentication authentication) {

        validarOperacaoPropria(authentication, funcionarioId);

        EvidenciaFoto novaEvidencia = fotoService.salvarEvidencia(
                file,
                ordemServicoId,
                funcionarioId,
                latitude,
                longitude
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(EvidenciaFotoResponse.from(novaEvidencia));
    }

    @GetMapping("/evidencias/os/{ordemServicoId}")
    public ResponseEntity<List<EvidenciaFotoResponse>> listarEvidencias(
            @PathVariable Long ordemServicoId) {
        List<EvidenciaFotoResponse> evidencias = fotoService.listarPorOrdemServico(ordemServicoId)
                .stream()
                .map(EvidenciaFotoResponse::from)
                .toList();
        return ResponseEntity.ok(evidencias);
    }

    @DeleteMapping("/evidencias/{evidenciaId}")
    public ResponseEntity<Void> removerEvidencia(
            @PathVariable Long evidenciaId,
            @RequestParam("funcionarioId") Long funcionarioId,
            Authentication authentication) {
        validarOperacaoPropria(authentication, funcionarioId);
        fotoService.removerEvidencia(evidenciaId, funcionarioId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
    public ResponseEntity<Map<String, String>> tratarErroOperacional(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("erro", ex.getMessage()));
    }

    private void validarOperacaoPropria(Authentication authentication, Long funcionarioId) {
        UsuarioAutenticado usuario = usuario(authentication);
        if (!"ADMIN".equals(usuario.getPerfil()) && !usuario.getFuncionarioId().equals(funcionarioId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "O usuário só pode registrar operações em seu próprio nome.");
        }
    }

    private void validarConsultaFuncionario(Authentication authentication, Long funcionarioId) {
        UsuarioAutenticado usuario = usuario(authentication);
        if ("TECNICO".equals(usuario.getPerfil()) && !usuario.getFuncionarioId().equals(funcionarioId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "O técnico só pode consultar sua própria jornada.");
        }
    }

    private UsuarioAutenticado usuario(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UsuarioAutenticado usuario)) {
            throw new org.springframework.security.access.AccessDeniedException("Sessão de funcionário inválida.");
        }
        return usuario;
    }

    @Data
    public static class PontoRequestDTO {
        private Long funcionarioId;
        private TipoPonto tipo; // Mudou aqui
        private String latitude;
        private String longitude;
    }

    @Data
    public static class EvidenciaFotoResponse {
        private Long id;
        private String caminhoArquivo;
        private String latitude;
        private String longitude;
        private LocalDateTime dataUpload;
        private Long ordemServicoId;
        private Long funcionarioId;
        private String funcionarioNome;

        static EvidenciaFotoResponse from(EvidenciaFoto evidencia) {
            EvidenciaFotoResponse response = new EvidenciaFotoResponse();
            response.setId(evidencia.getId());
            response.setCaminhoArquivo(evidencia.getCaminhoArquivo());
            response.setLatitude(evidencia.getLatitude());
            response.setLongitude(evidencia.getLongitude());
            response.setDataUpload(evidencia.getDataUpload());
            response.setOrdemServicoId(evidencia.getOrdemServico().getId());
            response.setFuncionarioId(evidencia.getFuncionario().getId());
            response.setFuncionarioNome(evidencia.getFuncionario().getNome());
            return response;
        }
    }
}
