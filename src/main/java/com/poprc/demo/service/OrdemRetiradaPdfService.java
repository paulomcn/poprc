package com.poprc.demo.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.poprc.demo.model.Comarca;
import com.poprc.demo.model.OrdemRetirada;
import com.poprc.demo.model.OrdemRetiradaItem;
import com.poprc.demo.model.OrdemServico;
import com.poprc.demo.repository.OrdemRetiradaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class OrdemRetiradaPdfService {

    private static final Color AZUL = new Color(8, 57, 112);
    private static final Color AZUL_CLARO = new Color(229, 239, 250);
    private static final Color CINZA_CLARO = new Color(241, 245, 249);
    private static final Color VERMELHO = new Color(185, 28, 28);
    private static final Font TITULO = new Font(Font.HELVETICA, 15, Font.BOLD, AZUL);
    private static final Font SECAO = new Font(Font.HELVETICA, 9, Font.BOLD, AZUL);
    private static final Font ROTULO = new Font(Font.HELVETICA, 7.5f, Font.BOLD, new Color(71, 85, 105));
    private static final Font TEXTO = new Font(Font.HELVETICA, 8.5f, Font.NORMAL, Color.BLACK);
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final OrdemRetiradaRepository ordemRetiradaRepository;

    @Transactional(readOnly = true)
    public byte[] gerarPdf(Long id) {
        OrdemRetirada ordem = ordemRetiradaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ordem de retirada não encontrada."));
        return gerarPdf(ordem);
    }

    byte[] gerarPdf(OrdemRetirada ordem) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Document pdf = new Document(PageSize.A4, 36, 36, 72, 48);
            PdfWriter writer = PdfWriter.getInstance(pdf, output);
            writer.setPageEvent(new CabecalhoRodape());
            pdf.open();

            Paragraph titulo = new Paragraph("ORDEM DE RETIRADA (OR)", TITULO);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(3);
            pdf.add(titulo);
            Paragraph numero = new Paragraph(valor(ordem.getNumeroOr()), SECAO);
            numero.setAlignment(Element.ALIGN_CENTER);
            numero.setSpacingAfter(14);
            pdf.add(numero);

            adicionarIdentificacao(pdf, ordem);
            adicionarSecao(pdf, "ITENS AUTORIZADOS PARA RETIRADA");
            adicionarItens(pdf, ordem);
            adicionarSecao(pdf, "EXECUÇÃO DA RETIRADA");
            adicionarRetirada(pdf, ordem);
            adicionarSecao(pdf, "DEVOLUÇÃO E CONFERÊNCIA");
            adicionarDevolucao(pdf, ordem);

            Paragraph nota = new Paragraph(
                    "A retirada de qualquer item depende da apresentação desta OR. Ferramentas devem retornar obrigatoriamente; materiais de consumo remanescentes devem ser devolvidos ao estoque.",
                    new Font(Font.HELVETICA, 7.5f, Font.ITALIC, new Color(71, 85, 105)));
            nota.setSpacingBefore(10);
            pdf.add(nota);

            pdf.close();
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Não foi possível gerar o PDF da ordem de retirada.", ex);
        }
    }

    private void adicionarIdentificacao(Document pdf, OrdemRetirada ordem) throws Exception {
        OrdemServico os = ordem.getOrdemServico();
        Comarca comarca = ordem.getComarca();
        PdfPTable tabela = new PdfPTable(new float[] { 1.1f, 2.1f, 1.1f, 2.1f });
        tabela.setWidthPercentage(100);
        campo(tabela, "Número da OR", ordem.getNumeroOr());
        campo(tabela, "Status", ordem.getStatus());
        campo(tabela, "Ordem de Serviço", os == null ? null : os.getNumeroOs());
        campo(tabela, "Contrato", os == null || os.getContrato() == null ? null : os.getContrato().getContrato());
        campo(tabela, "Comarca / Fórum", comarca == null ? null : comarca.getNomeComarca());
        campo(tabela, "Endereço", comarca == null ? null : comarca.getEndereco());
        campo(tabela, "Gerada em", data(ordem.getDataGeracao()));
        campo(tabela, "Gerada por", ordem.getGeradoPor());
        pdf.add(tabela);
    }

    private void adicionarItens(Document pdf, OrdemRetirada ordem) throws Exception {
        PdfPTable tabela = new PdfPTable(new float[] { 3.5f, 1.5f, 1.2f, 1.2f, 1.2f });
        tabela.setWidthPercentage(100);
        cabecalho(tabela, "Material");
        cabecalho(tabela, "Categoria");
        cabecalho(tabela, "Solicitado");
        cabecalho(tabela, "Retirado");
        cabecalho(tabela, "Devolvido");
        if (ordem.getItens() == null || ordem.getItens().isEmpty()) {
            PdfPCell vazio = new PdfPCell(new Phrase("Nenhum item vinculado.", TEXTO));
            vazio.setColspan(5);
            vazio.setPadding(8);
            vazio.setHorizontalAlignment(Element.ALIGN_CENTER);
            tabela.addCell(vazio);
        } else {
            for (OrdemRetiradaItem item : ordem.getItens()) {
                dado(tabela, item.getNomeMaterial(), Element.ALIGN_LEFT);
                dado(tabela, categoria(item.getCategoria()), Element.ALIGN_LEFT);
                dado(tabela, quantidade(item.getQuantidadeSolicitada()), Element.ALIGN_RIGHT);
                dado(tabela, quantidadeOperacao(item.getQuantidadeRetirada(), ordem.getDataRetirada() != null),
                        Element.ALIGN_RIGHT);
                dado(tabela, quantidadeOperacao(item.getQuantidadeDevolvida(), ordem.getDataDevolucao() != null),
                        Element.ALIGN_RIGHT);
            }
        }
        pdf.add(tabela);
    }

    private void adicionarRetirada(Document pdf, OrdemRetirada ordem) throws Exception {
        PdfPTable dados = new PdfPTable(new float[] { 1.2f, 2f, 1.2f, 2f });
        dados.setWidthPercentage(100);
        campo(dados, "Retirada em", data(ordem.getDataRetirada()));
        campo(dados, "Conferido por", ordem.getConferidoPor());
        campo(dados, "Levado por", ordem.getLevadoPor());
        campo(dados, "Situação", ordem.getDataRetirada() == null ? "Pendente" : "Executada");
        pdf.add(dados);

        PdfPTable assinaturas = new PdfPTable(2);
        assinaturas.setWidthPercentage(100);
        assinatura(assinaturas, "Assinatura de quem conferiu", ordem.getConferidoPor(), ordem.getAssinaturaConferenteBase64());
        assinatura(assinaturas, "Assinatura de quem levou", ordem.getLevadoPor(), ordem.getAssinaturaRetiranteBase64());
        pdf.add(assinaturas);
    }

    private void adicionarDevolucao(Document pdf, OrdemRetirada ordem) throws Exception {
        PdfPTable dados = new PdfPTable(new float[] { 1.2f, 2f, 1.2f, 2f });
        dados.setWidthPercentage(100);
        campo(dados, "Devolvida em", data(ordem.getDataDevolucao()));
        campo(dados, "Devolvido por", ordem.getDevolvidoPor());
        campo(dados, "Recebido por", ordem.getRecebidoPor());
        campo(dados, "Situação", ordem.getDataDevolucao() == null ? "Pendente" : "Concluída");
        pdf.add(dados);

        PdfPTable assinatura = new PdfPTable(1);
        assinatura.setWidthPercentage(50);
        assinatura.setHorizontalAlignment(Element.ALIGN_LEFT);
        assinatura(assinatura, "Assinatura de quem recebeu a devolução", ordem.getRecebidoPor(), ordem.getAssinaturaRecebimentoBase64());
        pdf.add(assinatura);
    }

    private void adicionarSecao(Document pdf, String titulo) throws Exception {
        PdfPTable faixa = new PdfPTable(1);
        faixa.setWidthPercentage(100);
        faixa.setSpacingBefore(10);
        faixa.setSpacingAfter(4);
        PdfPCell celula = new PdfPCell(new Phrase(titulo, SECAO));
        celula.setPadding(5);
        celula.setBackgroundColor(AZUL_CLARO);
        celula.setBorderColor(AZUL);
        faixa.addCell(celula);
        pdf.add(faixa);
    }

    private void campo(PdfPTable tabela, String rotulo, String conteudo) {
        PdfPCell chave = new PdfPCell(new Phrase(rotulo, ROTULO));
        chave.setPadding(5);
        chave.setBackgroundColor(CINZA_CLARO);
        tabela.addCell(chave);
        dado(tabela, valor(conteudo), Element.ALIGN_LEFT);
    }

    private void cabecalho(PdfPTable tabela, String texto) {
        PdfPCell celula = new PdfPCell(new Phrase(texto, ROTULO));
        celula.setPadding(5);
        celula.setBackgroundColor(CINZA_CLARO);
        celula.setHorizontalAlignment(Element.ALIGN_CENTER);
        tabela.addCell(celula);
    }

    private void dado(PdfPTable tabela, String texto, int alinhamento) {
        PdfPCell celula = new PdfPCell(new Phrase(valor(texto), TEXTO));
        celula.setPadding(5);
        celula.setHorizontalAlignment(alinhamento);
        tabela.addCell(celula);
    }

    private void assinatura(PdfPTable tabela, String papel, String nome, String base64) throws Exception {
        PdfPCell celula = new PdfPCell();
        celula.setPadding(7);
        celula.setMinimumHeight(90);
        celula.addElement(new Paragraph(papel, ROTULO));
        if (base64 != null && base64.contains(",")) {
            byte[] bytes = Base64.getDecoder().decode(base64.substring(base64.indexOf(',') + 1));
            Image imagem = Image.getInstance(bytes);
            imagem.scaleToFit(170, 42);
            imagem.setAlignment(Element.ALIGN_CENTER);
            celula.addElement(imagem);
        } else {
            celula.addElement(new Paragraph("\n\n________________________________________", TEXTO));
        }
        celula.addElement(new Paragraph(valor(nome), TEXTO));
        tabela.addCell(celula);
    }

    private String data(LocalDateTime data) {
        return data == null ? null : DATA_HORA.format(data);
    }

    private String quantidade(BigDecimal quantidade) {
        return quantidade == null ? "0" : quantidade.stripTrailingZeros().toPlainString();
    }

    private String quantidadeOperacao(BigDecimal quantidade, boolean realizada) {
        return realizada ? quantidade(quantidade) : "________________";
    }

    private String categoria(String categoria) {
        if ("MATERIAL_CONSUMO".equals(categoria)) {
            return "Material de consumo";
        }
        if ("FERRAMENTA".equals(categoria)) {
            return "Ferramenta";
        }
        return categoria;
    }

    private String valor(String texto) {
        return texto == null || texto.isBlank() ? "________________________________" : texto;
    }

    private static class CabecalhoRodape extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                PdfContentByte canvas = writer.getDirectContent();
                float largura = document.getPageSize().getWidth();
                float altura = document.getPageSize().getHeight();
                canvas.saveState();
                canvas.setColorFill(AZUL);
                canvas.rectangle(0, altura - 55, largura, 55);
                canvas.fill();
                canvas.setColorFill(VERMELHO);
                canvas.rectangle(0, 0, largura, 29);
                canvas.fill();
                canvas.restoreState();
                adicionarLogo(canvas, altura);

                canvas.beginText();
                canvas.setFontAndSize(BaseFont.createFont(), 8);
                canvas.setColorFill(Color.WHITE);
                canvas.showTextAligned(Element.ALIGN_LEFT, "RC TECHNOLOGY AND INTEGRATION LTDA", 42, altura - 37, 0);
                canvas.showTextAligned(Element.ALIGN_LEFT, "Controle de retirada e devolução", 36, 11, 0);
                canvas.showTextAligned(Element.ALIGN_RIGHT, "Página " + writer.getPageNumber(), largura - 36, 11, 0);
                canvas.endText();
            } catch (Exception ex) {
                throw new IllegalStateException("Não foi possível finalizar a página da OR.", ex);
            }
        }

        private void adicionarLogo(PdfContentByte canvas, float altura) {
            try (InputStream input = OrdemRetiradaPdfService.class.getResourceAsStream("/static/images/rclogo.jpg")) {
                if (input == null) {
                    return;
                }
                Image logo = Image.getInstance(input.readAllBytes());
                logo.scaleToFit(31, 31);
                logo.setAbsolutePosition(8, altura - 43);
                canvas.addImage(logo);
            } catch (Exception ignored) {
                // O cabeçalho textual continua identificando o documento.
            }
        }
    }
}
