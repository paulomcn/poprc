package com.poprc.demo.service;

import com.poprc.demo.repository.EvidenciaFotoRepository;
import com.poprc.demo.repository.FuncionarioRepository;
import com.poprc.demo.repository.OrdemServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class FotoServiceTest {

    private EvidenciaFotoRepository fotoRepository;
    private FuncionarioRepository funcionarioRepository;
    private OrdemServicoRepository ordemServicoRepository;
    private FotoService fotoService;

    @BeforeEach
    void setUp() {
        fotoRepository = mock(EvidenciaFotoRepository.class);
        funcionarioRepository = mock(FuncionarioRepository.class);
        ordemServicoRepository = mock(OrdemServicoRepository.class);
        fotoService = new FotoService(fotoRepository, funcionarioRepository, ordemServicoRepository);
    }

    @Test
    void deveRejeitarFormatoNaoPermitidoAntesDeConsultarBanco() {
        MockMultipartFile arquivo = new MockMultipartFile(
                "file", "evidencia.pdf", "application/pdf", "%PDF".getBytes());

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> fotoService.salvarEvidencia(arquivo, 1L, 1L, "-12.9", "-38.5"));

        assertEquals("Formato inválido. Envie uma imagem JPG ou PNG.", erro.getMessage());
        verifyNoInteractions(fotoRepository, funcionarioRepository, ordemServicoRepository);
    }

    @Test
    void deveRejeitarConteudoFalsoMesmoComMimeDeImagem() {
        MockMultipartFile arquivo = new MockMultipartFile(
                "file", "evidencia.png", "image/png", "nao-e-uma-imagem".getBytes());

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> fotoService.salvarEvidencia(arquivo, 1L, 1L, "-12.9", "-38.5"));

        assertEquals("O arquivo enviado não é uma imagem válida.", erro.getMessage());
        verifyNoInteractions(fotoRepository, funcionarioRepository, ordemServicoRepository);
    }
}
