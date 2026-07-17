package com.poprc.demo.service;

import com.poprc.demo.model.PrestacaoContas;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ComprovantePrestacaoService {

    private static final long TAMANHO_MAXIMO = 10L * 1024 * 1024;
    private static final String DIRETORIO = "rc_uploads/financeiro/comprovantes";
    private static final Map<String, String> EXTENSOES = Map.of(
            "application/pdf", "pdf",
            "image/jpeg", "jpg",
            "image/jpg", "jpg",
            "image/png", "png");

    private final FinanceiroService financeiroService;

    public PrestacaoContas fecharPrestacao(
            Long viagemId,
            java.math.BigDecimal custoReal,
            MultipartFile comprovante) {
        validar(comprovante);
        Path pasta = Paths.get(System.getProperty("user.home"), DIRETORIO)
                .toAbsolutePath()
                .normalize();
        String nome = UUID.randomUUID() + "." + EXTENSOES.get(comprovante.getContentType());
        Path destino = pasta.resolve(nome).normalize();
        if (!destino.startsWith(pasta)) {
            throw new IllegalArgumentException("Caminho de comprovante inválido.");
        }

        try {
            Files.createDirectories(pasta);
            comprovante.transferTo(destino.toFile());
            return financeiroService.fecharPrestacaoContas(
                    viagemId,
                    custoReal,
                    "/uploads/financeiro/comprovantes/" + nome);
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível salvar o comprovante no servidor.", ex);
        } catch (RuntimeException ex) {
            try {
                Files.deleteIfExists(destino);
            } catch (IOException ignored) {
                // Preserva a exceção original da operação financeira.
            }
            throw ex;
        }
    }

    private void validar(MultipartFile comprovante) {
        if (comprovante == null || comprovante.isEmpty()) {
            throw new IllegalArgumentException("O comprovante é obrigatório.");
        }
        if (comprovante.getSize() > TAMANHO_MAXIMO) {
            throw new IllegalArgumentException("O comprovante deve ter no máximo 10 MB.");
        }
        if (!EXTENSOES.containsKey(comprovante.getContentType())) {
            throw new IllegalArgumentException("Formato inválido. Envie PDF, JPG ou PNG.");
        }
    }
}
