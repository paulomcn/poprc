package com.poprc.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class NotaFiscalEstoqueDTO {
    private NotaFiscalEstoqueDTO() { }

    public record ArquivoRequest(String nomeArquivo, String contentType, String arquivoBase64) { }

    public record Preview(
            String nomeArquivo,
            String tipoArquivo,
            String hashSha256,
            String chaveAcesso,
            String numero,
            String serie,
            String emitenteNome,
            String emitenteCnpj,
            LocalDateTime dataEmissao,
            BigDecimal valorTotal,
            List<String> avisos,
            List<ItemPreview> itens) { }

    public record ItemPreview(
            String codigoProduto,
            String descricao,
            String ncm,
            String cfop,
            String unidadeFiscal,
            BigDecimal quantidade,
            BigDecimal valorUnitario,
            BigDecimal valorTotal,
            Long materialSugeridoId,
            String tipoControleSugerido,
            String unidadeMedidaSugerida) { }

    public record ConfirmarRequest(
            String nomeArquivo,
            String contentType,
            String arquivoBase64,
            String hashSha256,
            Long localEstoqueId,
            String chaveAcesso,
            String numero,
            String serie,
            String emitenteNome,
            String emitenteCnpj,
            LocalDateTime dataEmissao,
            List<ItemConfirmacao> itens) { }

    public record ItemConfirmacao(
            boolean importar,
            Long materialId,
            String codigoProduto,
            String nome,
            String partNumber,
            String descricao,
            String ncm,
            String cfop,
            String unidadeFiscal,
            BigDecimal quantidade,
            BigDecimal valorUnitario,
            String categoria,
            String tipoControle,
            String unidadeMedida) { }

    public record Resultado(
            Long importacaoId,
            String numero,
            int itensProcessados,
            int materiaisCriados,
            int materiaisExistentes,
            BigDecimal valorTotal) { }

    public record Historico(
            Long id,
            String nomeArquivo,
            String numero,
            String emitenteNome,
            LocalDateTime dataEmissao,
            LocalDateTime dataImportacao,
            String importadoPor,
            String localEstoque,
            int itensProcessados,
            BigDecimal valorTotal) { }

    public record Detalhe(
            Long id,
            String nomeArquivo,
            String tipoArquivo,
            String chaveAcesso,
            String numero,
            String serie,
            String emitenteNome,
            String emitenteCnpj,
            LocalDateTime dataEmissao,
            BigDecimal valorTotal,
            LocalDateTime dataImportacao,
            String importadoPor,
            String localEstoque,
            int materiaisCriados,
            int materiaisExistentes,
            List<ItemDetalhe> itens) { }

    public record ItemDetalhe(
            Long id,
            Long materialId,
            String material,
            String partNumber,
            String codigoProduto,
            String descricao,
            String ncm,
            String cfop,
            String unidadeFiscal,
            BigDecimal quantidade,
            BigDecimal valorUnitario,
            BigDecimal valorTotal,
            String acao) { }
}
