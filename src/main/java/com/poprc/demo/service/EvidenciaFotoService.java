package com.poprc.demo.service;

import com.poprc.demo.model.EvidenciaFoto;
import com.poprc.demo.repository.EvidenciaFotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EvidenciaFotoService {

    private final EvidenciaFotoRepository repository;

    public EvidenciaFoto salvarEvidencia(MultipartFile arquivo, EvidenciaFoto evidencia) {
        // Mock rápido de upload: na vida real, aqui entra S3, GCP, ou salvar no disco.
        String caminhoSalvo = "/uploads/" + arquivo.getOriginalFilename(); 
        
        evidencia.setCaminhoArquivo(caminhoSalvo);
        evidencia.setDataUpload(LocalDateTime.now());
        
        return repository.save(evidencia);
    }
}