package com.poprc.demo.controller;

import com.poprc.demo.model.Comarca;
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
    private ComarcaRepository comarcaRepository;
    private DocumentoAssinaturaLogRepository logRepository;
    private DocumentoInternoController controller;
    private DocumentoInterno documento;

    @BeforeEach
    void setUp() {
        documentoRepository = mock(DocumentoInternoRepository.class);
        comarcaRepository = mock(ComarcaRepository.class);
        logRepository = mock(DocumentoAssinaturaLogRepository.class);
        controller = new DocumentoInternoController(
                documentoRepository,
                comarcaRepository,
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
        when(documentoRepository.save(org.mockito.ArgumentMatchers.any(DocumentoInterno.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
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
        assertEquals(Boolean.TRUE, response.getBody().get("verificavel"));
        assertEquals("INTEGRO", response.getBody().get("situacao"));
    }

    @Test
    void deveIdentificarDocumentoLegadoSemHashComoNaoVerificavel() {
        documento.setHashRegistro(null);

        ResponseEntity<Map<String, Object>> response = controller.verificarIntegridade(1L, null, null);

        assertEquals(Boolean.FALSE, response.getBody().get("integro"));
        assertEquals(Boolean.FALSE, response.getBody().get("verificavel"));
        assertEquals("SEM_HASH_LEGADO", response.getBody().get("situacao"));
    }

    @Test
    void deveAtualizarConteudoEnquantoDocumentoEstiverPendente() {
        DocumentoInternoController.DocumentoVistoriaRequest request = new DocumentoInternoController.DocumentoVistoriaRequest();
        request.setConteudoJson("{\"descricaoServicos\":\"Passagem de cabos\"}");
        request.setRecebidoPor("Responsavel Local");

        ResponseEntity<DocumentoInterno> response = controller.atualizarConteudoDocumento(1L, request, null, null);

        assertEquals(request.getConteudoJson(), response.getBody().getConteudoJson());
        assertEquals("Responsavel Local", response.getBody().getRecebidoPor());
        verify(documentoRepository).save(documento);
    }

    @Test
    void deveImpedirAlteracaoDoConteudoDepoisDeAssinado() {
        documento.setAssinaturaTecnicoBase64("data:image/png;base64,iVBORw0KGgo=");
        DocumentoInternoController.DocumentoVistoriaRequest request = new DocumentoInternoController.DocumentoVistoriaRequest();
        request.setConteudoJson("{}");

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> controller.atualizarConteudoDocumento(1L, request, null, null));

        assertTrue(erro.getMessage().contains("não pode ser alterado"));
    }

    @Test
    void deveGerarDocumentosInicialEFinalComoVersoesIndependentes() {
        Comarca comarca = new Comarca();
        comarca.setId(10L);
        comarca.setNomeComarca("Comarca Teste");
        when(comarcaRepository.findById(10L)).thenReturn(Optional.of(comarca));

        DocumentoInterno inicial = controller.gerarDocumentoVistoria(
                documentoRequest(10L, "VISTORIA_INICIAL_OS"), null, "Gestor Teste").getBody();
        DocumentoInterno finalizacao = controller.gerarDocumentoVistoria(
                documentoRequest(10L, "ENCERRAMENTO_OS"), null, "Gestor Teste").getBody();

        assertEquals("VISTORIA_INICIAL_OS", inicial.getTipo());
        assertEquals("ENCERRAMENTO_OS", finalizacao.getTipo());
        assertEquals("PENDENTE_ASSINATURA", inicial.getStatus());
        assertEquals("PENDENTE_ASSINATURA", finalizacao.getStatus());
        assertEquals("Gestor Teste", inicial.getCriadoPor());
        verify(documentoRepository, times(2))
                .save(org.mockito.ArgumentMatchers.any(DocumentoInterno.class));
    }

    private DocumentoInternoController.AssinaturaPapelRequest assinaturaPngValida() {
        DocumentoInternoController.AssinaturaPapelRequest request = new DocumentoInternoController.AssinaturaPapelRequest();
        request.setNomeAssinante("Tecnico Teste");
        request.setAssinaturaBase64("data:image/png;base64,iVBORw0KGgo=");
        return request;
    }

    private DocumentoInternoController.DocumentoVistoriaRequest documentoRequest(Long comarcaId, String tipo) {
        DocumentoInternoController.DocumentoVistoriaRequest request =
                new DocumentoInternoController.DocumentoVistoriaRequest();
        request.setComarcaId(comarcaId);
        request.setTipo(tipo);
        request.setConteudoJson("{}");
        request.setRecebidoPor("Responsável Local");
        return request;
    }
}
