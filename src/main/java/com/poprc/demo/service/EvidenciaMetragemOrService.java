package com.poprc.demo.service;

import com.poprc.demo.storage.UploadStorage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class EvidenciaMetragemOrService {

    private static final long TAMANHO_MAXIMO = 10L * 1024 * 1024;
    private static final String DIRETORIO = "evidencias-metragem-or";
    private static final Map<String, String> EXTENSAO_POR_MIME = Map.of(
            "image/jpeg", "jpg",
            "image/jpg", "jpg",
            "image/png", "png");

    public EvidenciaPreparada preparar(String base64, String nomeOriginal) {
        if (base64 == null || base64.isBlank()) {
            throw new IllegalArgumentException(
                    "A foto da metragem restante é obrigatória para cada bobina/rolo.");
        }
        int separador = base64.indexOf(',');
        if (!base64.startsWith("data:image/") || separador < 0) {
            throw new IllegalArgumentException("A evidência de metragem deve ser uma imagem JPG ou PNG.");
        }
        String cabecalho = base64.substring(5, separador).toLowerCase(Locale.ROOT);
        String mime = cabecalho.split(";")[0];
        String extensao = EXTENSAO_POR_MIME.get(mime);
        if (extensao == null || !cabecalho.contains(";base64")) {
            throw new IllegalArgumentException("A evidência de metragem deve ser uma imagem JPG ou PNG.");
        }

        byte[] conteudo;
        try {
            conteudo = Base64.getDecoder().decode(base64.substring(separador + 1));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("A foto de evidência está corrompida.", ex);
        }
        if (conteudo.length == 0 || conteudo.length > TAMANHO_MAXIMO) {
            throw new IllegalArgumentException("A foto de evidência deve ter no máximo 10 MB.");
        }
        try {
            BufferedImage imagem = ImageIO.read(new ByteArrayInputStream(conteudo));
            if (imagem == null) {
                throw new IllegalArgumentException("O arquivo informado não é uma imagem válida.");
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Não foi possível validar a foto de evidência.", ex);
        }
        String nome = nomeOriginal == null || nomeOriginal.isBlank()
                ? "evidencia-metragem." + extensao
                : Paths.get(nomeOriginal).getFileName().toString();
        return new EvidenciaPreparada(conteudo, mime, extensao, limitar(nome, 255));
    }

    public EvidenciaSalva salvar(EvidenciaPreparada evidencia) {
        Path pasta = UploadStorage.directory(DIRETORIO).toAbsolutePath().normalize();
        String nomeArquivo = UUID.randomUUID() + "." + evidencia.extensao();
        Path arquivo = pasta.resolve(nomeArquivo).normalize();
        if (!arquivo.startsWith(pasta)) {
            throw new IllegalArgumentException("Caminho de evidência inválido.");
        }
        try {
            Files.createDirectories(pasta);
            Files.write(arquivo, evidencia.conteudo());
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível salvar a evidência de metragem.", ex);
        }
        removerEmCasoDeRollback(arquivo);
        return new EvidenciaSalva("/uploads/" + DIRETORIO + "/" + nomeArquivo, evidencia.nomeOriginal());
    }

    public ArquivoEvidencia carregar(String caminho, String nomeOriginal) {
        Path arquivo = resolver(caminho);
        try {
            if (!Files.isRegularFile(arquivo)) {
                throw new IllegalArgumentException("Arquivo da evidência de metragem não encontrado.");
            }
            String contentType = Files.probeContentType(arquivo);
            if (contentType == null || !contentType.startsWith("image/")) {
                contentType = arquivo.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")
                        ? "image/png"
                        : "image/jpeg";
            }
            return new ArquivoEvidencia(nomeOriginal, contentType, Files.readAllBytes(arquivo));
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível ler a evidência de metragem.", ex);
        }
    }

    private Path resolver(String caminho) {
        String prefixo = "/uploads/" + DIRETORIO + "/";
        if (caminho == null || !caminho.startsWith(prefixo)) {
            throw new IllegalStateException("Caminho de evidência de metragem inválido.");
        }
        Path pasta = UploadStorage.directory(DIRETORIO).toAbsolutePath().normalize();
        Path arquivo = pasta.resolve(Paths.get(caminho).getFileName().toString()).normalize();
        if (!arquivo.startsWith(pasta)) {
            throw new IllegalStateException("Caminho de evidência de metragem inválido.");
        }
        return arquivo;
    }

    private void removerEmCasoDeRollback(Path arquivo) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    try {
                        Files.deleteIfExists(arquivo);
                    } catch (IOException ignored) {
                        // O rollback do banco continua sendo a fonte de verdade da operação.
                    }
                }
            }
        });
    }

    private String limitar(String valor, int limite) {
        return valor.length() <= limite ? valor : valor.substring(0, limite);
    }

    public record EvidenciaPreparada(byte[] conteudo, String mime, String extensao, String nomeOriginal) {
    }

    public record EvidenciaSalva(String caminho, String nomeOriginal) {
    }

    public record ArquivoEvidencia(String nomeArquivo, String contentType, byte[] conteudo) {
    }
}
