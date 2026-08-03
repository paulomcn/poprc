package com.poprc.demo.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EvidenciaMetragemOrServiceTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void limparConfiguracao() {
        System.clearProperty("app.upload.dir");
    }

    @Test
    void deveValidarSalvarECarregarFotoDaMetragem() throws Exception {
        System.setProperty("app.upload.dir", tempDir.toString());
        EvidenciaMetragemOrService service = new EvidenciaMetragemOrService();
        BufferedImage imagem = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(imagem, "png", output);
        String base64 = "data:image/png;base64,"
                + Base64.getEncoder().encodeToString(output.toByteArray());

        EvidenciaMetragemOrService.EvidenciaPreparada preparada =
                service.preparar(base64, "medidor.png");
        EvidenciaMetragemOrService.EvidenciaSalva salva = service.salvar(preparada);
        EvidenciaMetragemOrService.ArquivoEvidencia carregada =
                service.carregar(salva.caminho(), salva.nomeOriginal());

        assertEquals("medidor.png", carregada.nomeArquivo());
        assertEquals("image/png", carregada.contentType());
        assertArrayEquals(preparada.conteudo(), carregada.conteudo());
    }

    @Test
    void deveExigirImagemValida() {
        EvidenciaMetragemOrService service = new EvidenciaMetragemOrService();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.preparar("data:text/plain;base64,QQ==", "arquivo.txt"));

        assertEquals("A evidência de metragem deve ser uma imagem JPG ou PNG.", exception.getMessage());
    }
}
