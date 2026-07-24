package com.poprc.demo.controller;

import com.poprc.demo.dto.PendenciaOperacionalDTO;
import com.poprc.demo.security.UsuarioAutenticado;
import com.poprc.demo.service.PendenciaOperacionalService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pendencias-operacionais")
@RequiredArgsConstructor
public class PendenciaOperacionalController {

    private final PendenciaOperacionalService service;

    @GetMapping
    public ResponseEntity<List<PendenciaOperacionalDTO>> listar(
            @RequestParam(required = false) String area,
            @RequestParam(required = false) Long funcionarioId,
            Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof UsuarioAutenticado usuario)) {
            throw new AccessDeniedException("Autenticação necessária para consultar pendências.");
        }

        return ResponseEntity.ok(switch (usuario.getPerfil()) {
            case "ADMIN", "SUPERVISOR_TECNICO" -> service.listar(area, funcionarioId);
            case "TECNICO" -> service.listar(
                    PendenciaOperacionalService.AREA_TECNICO, usuario.getFuncionarioId());
            case "ESTOQUE" -> service.listar(PendenciaOperacionalService.AREA_ESTOQUE, null);
            case "AUDITOR" -> service.listar(PendenciaOperacionalService.AREA_AUDITORIA, null);
            default -> throw new AccessDeniedException("Perfil sem acesso à fila operacional.");
        });
    }
}
