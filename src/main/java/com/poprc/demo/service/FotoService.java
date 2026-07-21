package com.poprc.demo.service;

import com.poprc.demo.model.EvidenciaFoto;
import com.poprc.demo.model.Funcionario;
import com.poprc.demo.model.OrdemServico;
import com.poprc.demo.model.StatusOS;
import com.poprc.demo.repository.EvidenciaFotoRepository;
import com.poprc.demo.repository.FuncionarioRepository;
import com.poprc.demo.repository.OrdemServicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FotoService {

    private static final long TAMANHO_MAXIMO = 10L * 1024 * 1024;
    private static final Map<String, String> EXTENSAO_POR_MIME = Map.of(
            "image/jpeg", "jpg",
            "image/jpg", "jpg",
            "image/png", "png");
    private static final String DIRETORIO_EVIDENCIAS = "rc_uploads/evidencias";

    private final EvidenciaFotoRepository fotoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final OrdemServicoRepository osRepository;

    @Transactional
    public EvidenciaFoto salvarEvidencia(MultipartFile arquivo, Long osId, Long funcionarioId, String lat, String lon) {
        validarArquivo(arquivo);
        validarCoordenada(lat, -90, 90, "latitude");
        validarCoordenada(lon, -180, 180, "longitude");

        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado."));

        OrdemServico os = osRepository.findById(osId)
                .orElseThrow(() -> new IllegalArgumentException("Ordem de Serviço não encontrada."));
        validarOrdemEditavel(os);

        Path pastaDestino = Paths.get(System.getProperty("user.home"), DIRETORIO_EVIDENCIAS)
                .toAbsolutePath()
                .normalize();
        String extensao = EXTENSAO_POR_MIME.get(arquivo.getContentType());
        String nomeUnicoArquivo = UUID.randomUUID() + "." + extensao;
        Path caminhoAbsoluto = pastaDestino.resolve(nomeUnicoArquivo).normalize();
        if (!caminhoAbsoluto.startsWith(pastaDestino)) {
            throw new IllegalArgumentException("Caminho de arquivo inválido.");
        }

        try {
            Files.createDirectories(pastaDestino);
            arquivo.transferTo(caminhoAbsoluto.toFile());
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível salvar a evidência no servidor.", e);
        }

        EvidenciaFoto evidencia = new EvidenciaFoto();
        evidencia.setCaminhoArquivo("/uploads/evidencias/" + nomeUnicoArquivo);
        evidencia.setLatitude(lat);
        evidencia.setLongitude(lon);
        evidencia.setDataUpload(LocalDateTime.now());
        evidencia.setOrdemServico(os);
        evidencia.setFuncionario(funcionario);

        try {
            return fotoRepository.save(evidencia);
        } catch (RuntimeException ex) {
            try {
                Files.deleteIfExists(caminhoAbsoluto);
            } catch (IOException ignored) {
                // O erro original do banco é mais relevante para a operação.
            }
            throw ex;
        }
    }

    public List<EvidenciaFoto> listarPorOrdemServico(Long ordemServicoId) {
        if (!osRepository.existsById(ordemServicoId)) {
            throw new IllegalArgumentException("Ordem de Serviço não encontrada.");
        }
        return fotoRepository.findByOrdemServicoIdOrderByDataUploadDesc(ordemServicoId);
    }

    @Transactional
    public void removerEvidencia(Long evidenciaId, Long funcionarioId) {
        EvidenciaFoto evidencia = fotoRepository.findById(evidenciaId)
                .orElseThrow(() -> new IllegalArgumentException("Evidência fotográfica não encontrada."));

        if (funcionarioId == null || evidencia.getFuncionario() == null
                || !funcionarioId.equals(evidencia.getFuncionario().getId())) {
            throw new IllegalStateException("Apenas o técnico que enviou a evidência pode removê-la.");
        }

        validarOrdemEditavel(evidencia.getOrdemServico());
        removerArquivo(evidencia.getCaminhoArquivo());
        fotoRepository.delete(evidencia);
    }

    private void validarOrdemEditavel(OrdemServico ordemServico) {
        if (ordemServico == null) {
            throw new IllegalArgumentException("Ordem de Serviço não encontrada.");
        }
        if (StatusOS.AGUARDANDO_VALIDACAO.equals(ordemServico.getStatus())
                || StatusOS.AGUARDANDO_DEVOLUCAO.equals(ordemServico.getStatus())
                || StatusOS.AGUARDANDO_AUDITORIA.equals(ordemServico.getStatus())
                || StatusOS.AGUARDANDO_ENCERRAMENTO.equals(ordemServico.getStatus())
                || StatusOS.CONCLUIDA.equals(ordemServico.getStatus())
                || StatusOS.FATURADA.equals(ordemServico.getStatus())) {
            throw new IllegalStateException(
                    "As evidências não podem ser alteradas após o envio da OS para validação.");
        }
    }

    private void removerArquivo(String caminhoArquivo) {
        if (caminhoArquivo == null || !caminhoArquivo.startsWith("/uploads/evidencias/")) {
            return;
        }

        Path pastaDestino = Paths.get(System.getProperty("user.home"), DIRETORIO_EVIDENCIAS)
                .toAbsolutePath()
                .normalize();
        Path arquivo = pastaDestino.resolve(Paths.get(caminhoArquivo).getFileName().toString())
                .toAbsolutePath()
                .normalize();
        if (!arquivo.startsWith(pastaDestino)) {
            throw new IllegalStateException("Caminho de evidência inválido.");
        }

        try {
            Files.deleteIfExists(arquivo);
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível remover a evidência do servidor.", ex);
        }
    }

    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("A foto de evidência é obrigatória.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO) {
            throw new IllegalArgumentException("A foto deve ter no máximo 10 MB.");
        }
        if (!EXTENSAO_POR_MIME.containsKey(arquivo.getContentType())) {
            throw new IllegalArgumentException("Formato inválido. Envie uma imagem JPG ou PNG.");
        }
        try {
            if (ImageIO.read(arquivo.getInputStream()) == null) {
                throw new IllegalArgumentException("O arquivo enviado não é uma imagem válida.");
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Não foi possível validar a imagem enviada.", ex);
        }
    }

    private void validarCoordenada(String valor, double minimo, double maximo, String nome) {
        try {
            double numero = Double.parseDouble(valor);
            if (numero < minimo || numero > maximo) {
                throw new IllegalArgumentException("Coordenada de " + nome + " inválida.");
            }
        } catch (NullPointerException | NumberFormatException ex) {
            throw new IllegalArgumentException("Coordenada de " + nome + " inválida.");
        }
    }
}
