package com.poprc.demo.service;

import com.lowagie.text.pdf.PdfReader;
import com.poprc.demo.model.Comarca;
import com.poprc.demo.model.Contrato;
import com.poprc.demo.model.OrdemRetirada;
import com.poprc.demo.model.OrdemRetiradaItem;
import com.poprc.demo.model.OrdemServico;
import com.poprc.demo.repository.OrdemRetiradaRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class OrdemRetiradaPdfServiceTest {

    @Test
    void deveGerarOrImprimivelMesmoAntesDaRetirada() throws Exception {
        OrdemRetiradaPdfService service = new OrdemRetiradaPdfService(mock(OrdemRetiradaRepository.class));

        Contrato contrato = new Contrato();
        contrato.setContrato("Contrato 01");
        OrdemServico os = new OrdemServico();
        os.setNumeroOs("Contrato 01 - OS 01");
        os.setContrato(contrato);
        Comarca comarca = new Comarca();
        comarca.setNomeComarca("KGM-Te");
        comarca.setEndereco("Endereço de teste");

        OrdemRetiradaItem item = new OrdemRetiradaItem();
        item.setNomeMaterial("Cabo de Rede Cat6");
        item.setCategoria("MATERIAL_CONSUMO");
        item.setQuantidadeSolicitada(new BigDecimal("25"));

        OrdemRetirada ordem = new OrdemRetirada();
        ordem.setId(4L);
        ordem.setNumeroOr("Contrato 01 - OS 01 - OR 01");
        ordem.setStatus("GERADA");
        ordem.setDataGeracao(LocalDateTime.of(2026, 7, 15, 9, 30));
        ordem.setGeradoPor("Paulo Morais");
        ordem.setOrdemServico(os);
        ordem.setComarca(comarca);
        ordem.setItens(List.of(item));

        byte[] pdf = service.gerarPdf(ordem);

        assertTrue(pdf.length > 1_000);
        assertTrue(new String(pdf, 0, 4, StandardCharsets.US_ASCII).startsWith("%PDF"));
        PdfReader reader = new PdfReader(pdf);
        assertEquals(1, reader.getNumberOfPages());
        assertTrue(reader.getPageSize(1).getWidth() > 590);
        reader.close();
    }
}
