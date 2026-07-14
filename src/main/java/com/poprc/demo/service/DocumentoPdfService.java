package com.poprc.demo.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.poprc.demo.model.DocumentoInterno;
import com.poprc.demo.repository.DocumentoInternoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentoPdfService {

    private static final String DIRETORIO_DOCUMENTOS = "rc_uploads/documentos";
    private static final Font TITULO = new Font(Font.HELVETICA, 15, Font.BOLD, new Color(15, 23, 42));
    private static final Font SUBTITULO = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(30, 64, 175));
    private static final Font ROTULO = new Font(Font.HELVETICA, 8, Font.BOLD, new Color(71, 85, 105));
    private static final Font TEXTO = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.BLACK);

    private final ObjectMapper objectMapper;
    private final DocumentoInternoRepository documentoRepository;

    public byte[] gerarPdf(DocumentoInterno documentoInterno) {
        try {
            JsonNode conteudo = objectMapper.readTree(documentoInterno.getConteudoJson() == null
                    ? "{}"
                    : documentoInterno.getConteudoJson());
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Document pdf = new Document(PageSize.A4, 42, 42, 42, 42);
            PdfWriter.getInstance(pdf, output);
            pdf.open();

            Paragraph titulo = new Paragraph("ORDEM DE SERVIÇO (OS)", TITULO);
            titulo.setAlignment(Element.ALIGN_CENTER);
            pdf.add(titulo);
            Paragraph modelo = new Paragraph(texto(conteudo, "modelo", documentoInterno.getTipo()), SUBTITULO);
            modelo.setAlignment(Element.ALIGN_CENTER);
            modelo.setSpacingAfter(18);
            pdf.add(modelo);

            PdfPTable identificacao = new PdfPTable(new float[] { 1, 2 });
            identificacao.setWidthPercentage(100);
            adicionarCampo(identificacao, "Empresa", texto(conteudo, "empresa", "RC TECHNOLOGY AND INTEGRATION LTDA"));
            adicionarCampo(identificacao, "CNPJ", texto(conteudo, "cnpj", "33.910.895/0001-50"));
            adicionarCampo(identificacao, "Contrato", texto(conteudo, "contrato", ""));
            adicionarCampo(identificacao, "Ordem de Serviço", texto(conteudo, "numeroOs", ""));
            adicionarCampo(identificacao, "Projeto", texto(conteudo, "projeto", ""));
            adicionarCampo(identificacao, "Comarca/Fórum", texto(conteudo, "comarcaForum", ""));
            adicionarCampo(identificacao, "Endereço", texto(conteudo, "endereco", ""));
            adicionarCampo(identificacao, "Início", texto(conteudo, "dataInicio", ""));
            adicionarCampo(identificacao, "Conclusão", texto(conteudo, "dataConclusao", ""));
            adicionarCampo(identificacao, "Equipe", texto(conteudo, "equipeResponsavel", ""));
            pdf.add(identificacao);

            adicionarSecao(pdf, "SERVIÇOS", texto(conteudo, "descricaoServicos", "Espaço para preenchimento dos serviços previstos ou executados."));
            adicionarSecao(pdf, "CONDIÇÕES E ANOMALIAS", texto(conteudo, "anomaliasPreExistentes", "Sem informações registradas."));
            adicionarSecao(pdf, "OBSERVAÇÕES FINAIS", texto(conteudo, "observacoesFinais", ""));
            adicionarSecao(pdf, "RESSALVAS", texto(conteudo, "ressalvas", ""));

            pdf.add(new Paragraph("ASSINATURAS", SUBTITULO));
            pdf.add(tabelaAssinaturas(documentoInterno));

            Paragraph auditoria = new Paragraph(
                    "Documento #" + documentoInterno.getId()
                            + " | Status: " + documentoInterno.getStatus()
                            + " | Hash: " + valor(documentoInterno.getHashRegistro()),
                    new Font(Font.COURIER, 7, Font.NORMAL, new Color(71, 85, 105)));
            auditoria.setSpacingBefore(18);
            pdf.add(auditoria);
            pdf.close();
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Não foi possível gerar o PDF do documento.", ex);
        }
    }

    @Transactional
    public DocumentoInterno arquivarPdf(DocumentoInterno documento) {
        try {
            byte[] bytes = gerarPdf(documento);
            String hash = sha256(bytes);
            Path pasta = Paths.get(System.getProperty("user.home"), DIRETORIO_DOCUMENTOS)
                    .toAbsolutePath().normalize();
            Files.createDirectories(pasta);
            String nome = "documento-" + documento.getId() + "-" + hash.substring(0, 16) + ".pdf";
            Path destino = pasta.resolve(nome).normalize();
            if (!destino.startsWith(pasta)) {
                throw new IllegalStateException("Caminho inválido para o PDF.");
            }
            Files.write(destino, bytes);
            documento.setPdfPath("/uploads/documentos/" + nome);
            documento.setPdfHash(hash);
            documento.setPdfGeradoEm(LocalDateTime.now());
            return documentoRepository.save(documento);
        } catch (Exception ex) {
            throw new IllegalStateException("Não foi possível arquivar o PDF do documento.", ex);
        }
    }

    public byte[] obterPdf(DocumentoInterno documento) {
        if (documento.getPdfPath() != null && !documento.getPdfPath().isBlank()) {
            try {
                String nome = Path.of(documento.getPdfPath()).getFileName().toString();
                Path arquivo = Paths.get(System.getProperty("user.home"), DIRETORIO_DOCUMENTOS, nome)
                        .toAbsolutePath().normalize();
                if (Files.exists(arquivo)) {
                    byte[] bytes = Files.readAllBytes(arquivo);
                    if (documento.getPdfHash() == null || documento.getPdfHash().equals(sha256(bytes))) {
                        return bytes;
                    }
                    throw new IllegalStateException("O PDF arquivado falhou na verificação de integridade.");
                }
            } catch (Exception ex) {
                throw new IllegalStateException("Não foi possível ler o PDF arquivado.", ex);
            }
        }
        return gerarPdf(documento);
    }

    private PdfPTable tabelaAssinaturas(DocumentoInterno documento) throws Exception {
        PdfPTable tabela = new PdfPTable(3);
        tabela.setWidthPercentage(100);
        adicionarAssinatura(tabela, "Técnico", documento.getTecnicoAssinadoPor(), documento.getAssinaturaTecnicoBase64());
        adicionarAssinatura(tabela, "Gestor RC", documento.getGestorAssinadoPor(), documento.getAssinaturaGestorBase64());
        adicionarAssinatura(tabela, "Gerente do Fórum", documento.getGerenteAssinadoPor(), documento.getAssinaturaGerenteBase64());
        return tabela;
    }

    private void adicionarAssinatura(PdfPTable tabela, String papel, String nome, String base64) throws Exception {
        PdfPCell celula = new PdfPCell();
        celula.setPadding(8);
        celula.setMinimumHeight(90);
        celula.addElement(new Paragraph(papel, ROTULO));
        if (base64 != null && base64.contains(",")) {
            byte[] imagemBytes = Base64.getDecoder().decode(base64.substring(base64.indexOf(',') + 1));
            Image imagem = Image.getInstance(imagemBytes);
            imagem.scaleToFit(130, 42);
            imagem.setAlignment(Element.ALIGN_CENTER);
            celula.addElement(imagem);
        } else {
            celula.addElement(new Paragraph("\n\n________________________", TEXTO));
        }
        celula.addElement(new Paragraph(valor(nome), TEXTO));
        tabela.addCell(celula);
    }

    private void adicionarCampo(PdfPTable tabela, String rotulo, String conteudo) {
        PdfPCell chave = new PdfPCell(new Phrase(rotulo, ROTULO));
        chave.setPadding(6);
        chave.setBackgroundColor(new Color(241, 245, 249));
        tabela.addCell(chave);
        PdfPCell valor = new PdfPCell(new Phrase(valor(conteudo), TEXTO));
        valor.setPadding(6);
        tabela.addCell(valor);
    }

    private void adicionarSecao(Document pdf, String titulo, String conteudo) throws Exception {
        Paragraph cabecalho = new Paragraph(titulo, SUBTITULO);
        cabecalho.setSpacingBefore(14);
        cabecalho.setSpacingAfter(5);
        pdf.add(cabecalho);
        Paragraph corpo = new Paragraph(valor(conteudo), TEXTO);
        corpo.setLeading(14);
        pdf.add(corpo);
    }

    private String texto(JsonNode node, String campo, String padrao) {
        JsonNode valor = node.get(campo);
        return valor == null || valor.isNull() ? padrao : valor.asText(padrao);
    }

    private String valor(String texto) {
        return texto == null || texto.isBlank() ? "________________________________" : texto;
    }

    private String sha256(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(bytes));
    }
}
