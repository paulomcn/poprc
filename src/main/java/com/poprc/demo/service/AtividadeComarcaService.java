package com.poprc.demo.service;

import com.poprc.demo.exception.ArquivoInvalidoException;
import com.poprc.demo.exception.DescricaoAtividadeObrigatoriaException;
import com.poprc.demo.model.AtividadeComarca;
import com.poprc.demo.model.Comarca;
import com.poprc.demo.repository.AtividadeComarcaRepository;
import com.poprc.demo.repository.ComarcaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AtividadeComarcaService {

    // Whitelist de extensões aceitas (regra de negócio + segurança no upload)
    private static final Set<String> EXTENSOES_PERMITIDAS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> MIME_PERMITIDOS = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp");
    private static final String DIRETORIO_UPLOAD = "rc_uploads/comarcas";

    private final AtividadeComarcaRepository atividadeComarcaRepository;
    private final ComarcaRepository comarcaRepository;

    @Transactional
    public AtividadeComarca registrarAtividade(Long comarcaId, LocalDate dataInicio, LocalDate dataEncerramento,
            String descricaoAtividades, List<MultipartFile> fotos) {

        validarDescricao(descricaoAtividades);

        Comarca comarca = comarcaRepository.findById(comarcaId)
                .orElseThrow(() -> new RuntimeException("Comarca não encontrada"));

        AtividadeComarca atividade = new AtividadeComarca();
        atividade.setComarca(comarca);
        atividade.setDataInicio(dataInicio);
        atividade.setDataEncerramento(dataEncerramento);
        atividade.setDescricaoAtividades(descricaoAtividades.trim());
        atividade.setDataRegistro(LocalDateTime.now());
        atividade.setFotosEvidencia(salvarFotos(fotos));

        return atividadeComarcaRepository.save(atividade);
    }

    @Transactional
    public AtividadeComarca atualizarAtividade(Long atividadeId, LocalDate dataInicio, LocalDate dataEncerramento,
            String descricaoAtividades, List<MultipartFile> novasFotos) {

        validarDescricao(descricaoAtividades);

        AtividadeComarca atividade = atividadeComarcaRepository.findById(atividadeId)
                .orElseThrow(() -> new RuntimeException("Registro de atividade não encontrado"));

        // Datas e descrição podem ser editadas posteriormente, conforme regra de
        // negócio
        atividade.setDataInicio(dataInicio);
        atividade.setDataEncerramento(dataEncerramento);
        atividade.setDescricaoAtividades(descricaoAtividades.trim());

        List<String> novosCaminhos = salvarFotos(novasFotos);
        if (!novosCaminhos.isEmpty()) {
            atividade.getFotosEvidencia().addAll(novosCaminhos);
        }

        return atividadeComarcaRepository.save(atividade);
    }

    @Transactional(readOnly = true)
    public List<AtividadeComarca> listarPorComarca(Long comarcaId) {
        return atividadeComarcaRepository.findByComarcaIdOrderByDataInicioDesc(comarcaId);
    }

    private void validarDescricao(String descricaoAtividades) {
        if (descricaoAtividades == null || descricaoAtividades.replaceAll("<[^>]*>", "").trim().isEmpty()) {
            throw new DescricaoAtividadeObrigatoriaException(
                    "O campo 'Descrição das Atividades' é obrigatório e não pode estar vazio.");
        }
    }

    private List<String> salvarFotos(List<MultipartFile> fotos) {
        if (fotos == null || fotos.isEmpty()) {
            return new ArrayList<>();
        }

        Path pastaDestino = Paths.get(System.getProperty("user.home"), DIRETORIO_UPLOAD);
        try {
            Files.createDirectories(pastaDestino);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao preparar diretório de upload de evidências", e);
        }

        return fotos.stream()
                .filter(f -> f != null && !f.isEmpty())
                .map(foto -> salvarArquivoIndividual(foto, pastaDestino))
                .collect(Collectors.toList());
    }

    private String salvarArquivoIndividual(MultipartFile foto, Path pastaDestino) {
        String nomeOriginal = foto.getOriginalFilename();
        String extensao = extrairExtensao(nomeOriginal);

        // Dupla validação: extensão do nome do arquivo + content-type declarado
        if (!EXTENSOES_PERMITIDAS.contains(extensao.toLowerCase())
                || !MIME_PERMITIDOS.contains(foto.getContentType())) {
            throw new ArquivoInvalidoException(
                    "Formato de arquivo não permitido. Envie apenas imagens .jpg, .jpeg, .png ou .webp.");
        }

        // Nome aleatório (UUID) evita colisão e impede path traversal a partir do nome
        // original
        String nomeUnico = UUID.randomUUID() + "." + extensao.toLowerCase();
        Path caminhoAbsoluto = pastaDestino.resolve(nomeUnico);

        try {
            foto.transferTo(caminhoAbsoluto.toFile());
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar evidência fotográfica no servidor", e);
        }

        // URL pública servida pelo resource handler configurado em WebConfig
        return "/uploads/comarcas/" + nomeUnico;
    }

    private String extrairExtensao(String nomeArquivo) {
        if (nomeArquivo == null || !nomeArquivo.contains(".")) {
            throw new ArquivoInvalidoException("Arquivo enviado sem extensão válida.");
        }
        return nomeArquivo.substring(nomeArquivo.lastIndexOf(".") + 1);
    }
}