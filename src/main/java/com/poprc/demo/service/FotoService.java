package com.poprc.demo.service;

import com.poprc.demo.model.EvidenciaFoto;
import com.poprc.demo.model.Funcionario;
import com.poprc.demo.model.OrdemServico;
import com.poprc.demo.repository.EvidenciaFotoRepository;
import com.poprc.demo.repository.FuncionarioRepository;
import com.poprc.demo.repository.OrdemServicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FotoService {

    private final EvidenciaFotoRepository fotoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final OrdemServicoRepository osRepository;

    @Transactional
    public EvidenciaFoto salvarEvidencia(MultipartFile arquivo, Long osId, Long funcionarioId, String lat, String lon) {
        // 1. Validações de segurança básicas
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado"));
        
        OrdemServico os = osRepository.findById(osId)
                .orElseThrow(() -> new RuntimeException("Ordem de Serviço não encontrada"));

        // 2. Lógica de Upload Físico em Disco
        String diretorioHome = System.getProperty("user.home");
        Path pastaDestino = Paths.get(diretorioHome, "rc_uploads");
        
        // UUID evita colisão de nomes de arquivos
        String nomeUnicoArquivo = UUID.randomUUID() + "_" + arquivo.getOriginalFilename();
        Path caminhoAbsoluto = pastaDestino.resolve(nomeUnicoArquivo);

        try {
            Files.createDirectories(pastaDestino); // Cria a pasta se não existir
            arquivo.transferTo(caminhoAbsoluto.toFile()); // Descarrega o arquivo no disco
        } catch (IOException e) {
            throw new RuntimeException("Erro crítico ao salvar o arquivo no servidor", e);
        }

        // 3. Criação do Objeto de Evidência e Persistência no Banco
        EvidenciaFoto evidencia = new EvidenciaFoto();
        evidencia.setCaminhoArquivo(caminhoAbsoluto.toString());
        evidencia.setLatitude(lat);
        evidencia.setLongitude(lon);
        evidencia.setDataUpload(LocalDateTime.now());
        evidencia.setOrdemServico(os);
        evidencia.setFuncionario(funcionario);

        return fotoRepository.save(evidencia);
    }
}