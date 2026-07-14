package com.poprc.demo.controller;

import com.poprc.demo.model.DocumentoInterno;
import com.poprc.demo.repository.ComarcaRepository;
import com.poprc.demo.repository.DocumentoAssinaturaLogRepository;
import com.poprc.demo.repository.DocumentoInternoRepository;
import com.poprc.demo.service.DocumentoPdfService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentoInternoControllerTest {

    private DocumentoInternoRepository documentoRepository;
    private DocumentoAssinaturaLogRepository logRepository;
    private DocumentoInternoController controller;
    private DocumentoInterno documento;

    @BeforeEach
    void setUp() {
        documentoRepository = mock(DocumentoInternoRepository.class);
        logRepository = mock(DocumentoAssinaturaLogRepository.class);
        controller = new DocumentoInternoController(
                documentoRepository,
                mock(ComarcaRepository.class),
                logRepository,
                mock(DocumentoPdfService.class));

        documento = new DocumentoInterno();
        documento.setId(1L);
        documento.setTipo("VISTORIA_INICIAL_OS");
        documento.setStatus("PENDENTE_ASSINATURA");
        documento.setConteudoJson("{}");
        documento.setCriadoPor("Sistema");
        when(documentoRepository.findById(1L)).thenReturn(Optional.of(documento));
        when(documentoRepository.save(documento)).thenReturn(documento);
    }

    @Test
    void deveImpedirSubstituicaoDaAssinaturaDoMesmoPapel() {
        DocumentoInternoController.AssinaturaPapelRequest request = assinaturaPngValida();

        controller.assinarDocumentoPorPapel(1L, "TECNICO", request, null, null);

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> controller.assinarDocumentoPorPapel(1L, "TECNICO", request, null, null));
        assertTrue(erro.getMessage().contains("não pode ser substituída"));
        verify(logRepository, times(1)).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deveRejeitarAssinaturaQueNaoSejaPngReal() {
        DocumentoInternoController.AssinaturaPapelRequest request = new DocumentoInternoController.AssinaturaPapelRequest();
        request.setNomeAssinante("Tecnico Teste");
        request.setAssinaturaBase64("data:image/png;base64,bmFvLWUtaW1hZ2Vt");

        assertThrows(IllegalArgumentException.class,
                () -> controller.assinarDocumentoPorPapel(1L, "TECNICO", request, null, null));
    }

    @Test
    void deveConfirmarIntegridadeDepoisDaAssinatura() {
        controller.assinarDocumentoPorPapel(1L, "TECNICO", assinaturaPngValida(), null, null);

        ResponseEntity<Map<String, Object>> response = controller.verificarIntegridade(1L, null, null);

        assertEquals(Boolean.TRUE, response.getBody().get("integro"));
    }

    private DocumentoInternoController.AssinaturaPapelRequest assinaturaPngValida() {
        DocumentoInternoController.AssinaturaPapelRequest request = new DocumentoInternoController.AssinaturaPapelRequest();
        request.setNomeAssinante("Tecnico Teste");
        request.setAssinaturaBase64("data:image/png;base64,iVBORw0KGgo=");
        return request;
    }
}
