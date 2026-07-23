package com.poprc.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.lowagie.text.pdf.PdfReader;
import com.poprc.demo.model.Comarca;
import com.poprc.demo.model.Contrato;
import com.poprc.demo.model.DocumentoInterno;
import com.poprc.demo.model.OrdemRetirada;
import com.poprc.demo.model.OrdemRetiradaItem;
import com.poprc.demo.model.OrdemServico;
import com.poprc.demo.repository.DocumentoInternoRepository;
import com.poprc.demo.repository.OrdemRetiradaDocumentoRepository;
import com.poprc.demo.repository.OrdemRetiradaRepository;

import tools.jackson.databind.ObjectMapper;

class DocumentoPdfVisualFixtureTest {

    private static final Path OUTPUT = Path.of("target", "pdf-review");

    @Test
    void geraDocumentosVaziosEPreenchidosParaRevisaoVisual() throws Exception {
        Files.createDirectories(OUTPUT);

        DocumentoPdfService documentoService = new DocumentoPdfService(
                new ObjectMapper(), mock(DocumentoInternoRepository.class));
        OrdemRetiradaPdfService retiradaService = new OrdemRetiradaPdfService(
                mock(OrdemRetiradaRepository.class),
                mock(OrdemRetiradaDocumentoRepository.class));

        byte[] osVazia = documentoService.gerarPdf(documentoVazio());
        byte[] osPreenchida = documentoService.gerarPdf(documentoPreenchido());
        byte[] orVazia = retiradaService.gerarPdf(ordemRetirada(false));
        byte[] orPreenchida = retiradaService.gerarPdf(ordemRetirada(true));

        gravarEValidar("os-vazia.pdf", osVazia, 5);
        gravarEValidar("os-preenchida.pdf", osPreenchida, 5);
        gravarEValidar("or-antes-retirada.pdf", orVazia, 1);
        gravarEValidar("or-concluida.pdf", orPreenchida, 1);
    }

    private DocumentoInterno documentoVazio() {
        DocumentoInterno documento = new DocumentoInterno();
        documento.setId(100L);
        documento.setTipo("VISTORIA_INICIAL_OS");
        documento.setStatus("PENDENTE_ASSINATURA");
        documento.setConteudoJson("{}");
        documento.setCriadoPor("Paulo Morais");
        documento.setRecebidoPor("Gerente do Forum");
        documento.setDataGeracao(LocalDateTime.of(2026, 7, 17, 9, 0));
        return documento;
    }

    private DocumentoInterno documentoPreenchido() throws Exception {
        DocumentoInterno documento = documentoVazio();
        documento.setId(101L);
        documento.setTipo("ENCERRAMENTO_OS");
        documento.setStatus("REGISTRADO");
        documento.setConteudoJson("""
                {
                  "contrato":"HML-20260717-C01",
                  "numeroOs":"HML-20260717-C01 - OS 01",
                  "projeto":"Infraestrutura e cabeamento estruturado",
                  "comarcaForum":"Obra Homologacao Concorrencia",
                  "endereco":"Avenida de Homologacao, 100 - Centro",
                  "dataInicio":"17/07/2026 09:00",
                  "dataConclusao":"17/07/2026 17:00",
                  "equipeResponsavel":"Equipe Tecnica RC",
                  "gestorRc":"Paulo Morais",
                  "gerenteForum":"Gerente do Forum",
                  "descricaoServicos":"Instalacao de cabeamento, organizacao do rack, identificacao dos pontos e testes finais de conectividade.",
                  "anomaliasPreExistentes":"Canaleta antiga com desgaste na sala administrativa, registrada antes do inicio.",
                  "protocoloComunicacao":"PROTOCOLO-HML-001",
                  "observacoesFinais":"Servicos concluidos e ambiente entregue limpo e organizado.",
                  "ressalvas":"Sem ressalvas pendentes.",
                  "tecnicoResponsavel":"Tecnico Desenvolvimento",
                  "gestorProjetoRc":"Paulo Morais",
                  "recebidoPor":"Gerente do Forum",
                  "responsavelDesignadoNome":"Responsavel Local",
                  "responsavelDesignadoCargo":"Gerente Administrativo",
                  "declaracaoDesignacao":"Declaro ter acompanhado a vistoria, a execucao e o recebimento dos servicos descritos.",
                  "objetoServicos":["Instalação de cabeamento estruturado","Organização e identificação de racks"],
                  "estadoInicial":["Foi realizado registro fotográfico completo do ambiente"],
                  "estadoFinal":["O ambiente foi entregue limpo e organizado","Foi realizado registro fotográfico final antes e depois"]
                }
                """);
        String assinatura = assinaturaBase64();
        documento.setAssinaturaTecnicoBase64(assinatura);
        documento.setTecnicoAssinadoPor("Tecnico Desenvolvimento");
        documento.setAssinaturaGestorBase64(assinatura);
        documento.setGestorAssinadoPor("Paulo Morais");
        documento.setAssinaturaGerenteBase64(assinatura);
        documento.setGerenteAssinadoPor("Gerente do Forum");
        return documento;
    }

    private OrdemRetirada ordemRetirada(boolean concluida) throws Exception {
        Contrato contrato = new Contrato();
        contrato.setContrato("HML-20260717-C01");
        OrdemServico os = new OrdemServico();
        os.setNumeroOs("HML-20260717-C01 - OS 01");
        os.setContrato(contrato);
        Comarca comarca = new Comarca();
        comarca.setNomeComarca("Obra Homologacao Concorrencia");
        comarca.setEndereco("Avenida de Homologacao, 100 - Centro");

        OrdemRetiradaItem consumo = new OrdemRetiradaItem();
        consumo.setNomeMaterial("Cabo de Rede CAT6");
        consumo.setCategoria("MATERIAL_CONSUMO");
        consumo.setQuantidadeSolicitada(new BigDecimal("25"));
        consumo.setQuantidadeRetirada(concluida ? new BigDecimal("25") : BigDecimal.ZERO);
        consumo.setQuantidadeDevolvida(concluida ? new BigDecimal("3") : BigDecimal.ZERO);

        OrdemRetiradaItem ferramenta = new OrdemRetiradaItem();
        ferramenta.setNomeMaterial("Alicate de Crimpagem");
        ferramenta.setCategoria("FERRAMENTA");
        ferramenta.setQuantidadeSolicitada(BigDecimal.ONE);
        ferramenta.setQuantidadeRetirada(concluida ? BigDecimal.ONE : BigDecimal.ZERO);
        ferramenta.setQuantidadeDevolvida(concluida ? BigDecimal.ONE : BigDecimal.ZERO);

        OrdemRetirada ordem = new OrdemRetirada();
        ordem.setId(concluida ? 201L : 200L);
        ordem.setNumeroOr("HML-20260717-C01 - OS 01 - OR 01");
        ordem.setStatus(concluida ? "DEVOLVIDA" : "GERADA");
        ordem.setDataGeracao(LocalDateTime.of(2026, 7, 17, 8, 45));
        ordem.setGeradoPor("Paulo Morais");
        ordem.setOrdemServico(os);
        ordem.setComarca(comarca);
        ordem.setItens(List.of(consumo, ferramenta));

        if (concluida) {
            String assinatura = assinaturaBase64();
            ordem.setDataRetirada(LocalDateTime.of(2026, 7, 17, 9, 0));
            ordem.setConferidoPor("Sammuel");
            ordem.setLevadoPor("Tecnico Desenvolvimento");
            ordem.setAssinaturaConferenteBase64(assinatura);
            ordem.setAssinaturaRetiranteBase64(assinatura);
            ordem.setDataDevolucao(LocalDateTime.of(2026, 7, 17, 17, 30));
            ordem.setDevolvidoPor("Tecnico Desenvolvimento");
            ordem.setRecebidoPor("Sammuel");
            ordem.setAssinaturaRecebimentoBase64(assinatura);
        }
        return ordem;
    }

    private String assinaturaBase64() throws Exception {
        BufferedImage image = new BufferedImage(300, 80, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLACK);
        graphics.setStroke(new BasicStroke(3));
        graphics.drawLine(20, 55, 70, 25);
        graphics.drawLine(70, 25, 120, 58);
        graphics.drawLine(120, 58, 180, 20);
        graphics.drawLine(180, 20, 260, 52);
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
    }

    private void gravarEValidar(String nome, byte[] conteudo, int paginas) throws Exception {
        Path arquivo = OUTPUT.resolve(nome);
        Files.write(arquivo, conteudo);
        PdfReader reader = new PdfReader(conteudo);
        assertEquals(paginas, reader.getNumberOfPages());
        reader.close();
    }
}
