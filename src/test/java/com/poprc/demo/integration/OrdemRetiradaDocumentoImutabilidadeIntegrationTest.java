package com.poprc.demo.integration;

import com.poprc.demo.model.OrdemRetirada;
import com.poprc.demo.model.OrdemRetiradaDocumento;
import com.poprc.demo.repository.OrdemRetiradaDocumentoRepository;
import com.poprc.demo.repository.OrdemRetiradaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrdemRetiradaDocumentoImutabilidadeIntegrationTest {

    @Autowired
    private OrdemRetiradaRepository ordemRepository;

    @Autowired
    private OrdemRetiradaDocumentoRepository documentoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void bancoBloqueiaAlteracaoDoDocumentoArquivado() {
        OrdemRetiradaDocumento documento = criarDocumento();

        assertThrows(DataAccessException.class, () -> jdbcTemplate.update(
                "UPDATE ordens_retirada_documentos SET status_or = ? WHERE id = ?",
                "ALTERADO", documento.getId()));
    }

    @Test
    void bancoBloqueiaExclusaoDoDocumentoArquivado() {
        OrdemRetiradaDocumento documento = criarDocumento();

        assertThrows(DataAccessException.class, () -> jdbcTemplate.update(
                "DELETE FROM ordens_retirada_documentos WHERE id = ?", documento.getId()));
    }

    private OrdemRetiradaDocumento criarDocumento() {
        OrdemRetirada ordem = new OrdemRetirada();
        ordem.setNumeroOr("OR-TESTE-" + System.nanoTime());
        ordem.setStatus("GERADA");
        ordem.setDataGeracao(LocalDateTime.now());
        ordem = ordemRepository.saveAndFlush(ordem);

        OrdemRetiradaDocumento documento = new OrdemRetiradaDocumento();
        documento.setOrdemRetirada(ordem);
        documento.setFase("GERACAO");
        documento.setStatusOr("GERADA");
        documento.setPdfPath("/uploads/documentos/ordens-retirada/teste.pdf");
        documento.setPdfHash("a".repeat(64));
        documento.setGeradoEm(LocalDateTime.now());
        return documentoRepository.saveAndFlush(documento);
    }
}
