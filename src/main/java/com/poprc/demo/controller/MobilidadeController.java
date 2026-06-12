package com.poprc.demo.controller;

import com.poprc.demo.model.EvidenciaFoto;
import com.poprc.demo.model.RegistroPonto;
import com.poprc.demo.service.EvidenciaFotoService;
import com.poprc.demo.service.RegistroPontoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/campo")
@RequiredArgsConstructor
public class MobilidadeController {

    private final RegistroPontoService pontoService;
    private final EvidenciaFotoService fotoService;

    @PostMapping("/ponto")
    public ResponseEntity<RegistroPonto> registrarPonto(@RequestBody RegistroPonto ponto) {
        RegistroPonto salvo = pontoService.salvarPonto(ponto);
        return ResponseEntity.ok(salvo);
    }

    @PostMapping("/upload-foto")
    public ResponseEntity<EvidenciaFoto> uploadFoto(
            @RequestParam("arquivo") MultipartFile arquivo,
            @ModelAttribute EvidenciaFoto evidenciaDados) {
            
        EvidenciaFoto salva = fotoService.salvarEvidencia(arquivo, evidenciaDados);
        return ResponseEntity.ok(salva);
    }
}