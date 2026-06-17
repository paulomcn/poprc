package com.poprc.demo.controller;

import com.poprc.demo.model.EvidenciaFoto;
import com.poprc.demo.model.RegistroPonto;
import com.poprc.demo.model.TipoRegistro;
import com.poprc.demo.service.FotoService;
import com.poprc.demo.service.PontoService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/campo")
@RequiredArgsConstructor
public class MobilidadeController {

    // Injetando os novos serviços da Parte 2
    private final PontoService pontoService;
    private final FotoService fotoService;

    @PostMapping("/ponto")
    public ResponseEntity<RegistroPonto> registrarPonto(@RequestBody PontoRequestDTO request) {
        RegistroPonto novoPonto = pontoService.salvarPonto(
                request.getFuncionarioId(),
                request.getTipo(),
                request.getLatitude(),
                request.getLongitude()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(novoPonto);
    }

    @PostMapping("/upload-foto")
    public ResponseEntity<EvidenciaFoto> uploadFoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam("ordemServicoId") Long ordemServicoId,
            @RequestParam("funcionarioId") Long funcionarioId,
            @RequestParam("latitude") String latitude,
            @RequestParam("longitude") String longitude) {

        EvidenciaFoto novaEvidencia = fotoService.salvarEvidencia(
                file,
                ordemServicoId,
                funcionarioId,
                latitude,
                longitude
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(novaEvidencia);
    }

    // DTO necessário para o React mandar os dados do ponto num JSON limpo
    @Data
    public static class PontoRequestDTO {
        private Long funcionarioId;
        private TipoRegistro tipo; 
        private String latitude;
        private String longitude;
    }
}