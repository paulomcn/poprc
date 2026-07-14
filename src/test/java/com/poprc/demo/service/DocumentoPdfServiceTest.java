package com.poprc.demo.service;

import com.poprc.demo.model.DocumentoInterno;
import com.poprc.demo.repository.DocumentoInternoRepository;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class DocumentoPdfServiceTest {

    @Test
    void deveGerarPdfMesmoComCamposEmBranco() {
        DocumentoPdfService service = new DocumentoPdfService(
                new ObjectMapper(), mock(DocumentoInternoRepository.class));
        DocumentoInterno documento = new DocumentoInterno();
        documento.setId(12L);
        documento.setTipo("VISTORIA_INICIAL_OS");
        documento.setStatus("PENDENTE_ASSINATURA");
        documento.setConteudoJson("{\"numeroOs\":\"Contrato 01 - OS 01\",\"descricaoServicos\":\"\"}");

        byte[] pdf = service.gerarPdf(documento);

        assertTrue(pdf.length > 500);
        assertTrue(new String(pdf, 0, 4, StandardCharsets.US_ASCII).startsWith("%PDF"));
    }
}
