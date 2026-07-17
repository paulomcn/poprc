package com.poprc.demo.integration;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.poprc.demo.model.DocumentoAssinaturaLog;
import com.poprc.demo.model.DocumentoInterno;
import com.poprc.demo.repository.DocumentoAssinaturaLogRepository;
import com.poprc.demo.repository.DocumentoInternoRepository;

@SpringBootTest
@Transactional
class DocumentoAssinaturaLogImutabilidadeIntegrationTest {

    @Autowired
    private DocumentoInternoRepository documentoRepository;

    @Autowired
    private DocumentoAssinaturaLogRepository assinaturaLogRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void bancoBloqueiaAlteracaoDoLogDeAssinatura() {
        DocumentoAssinaturaLog log = criarLog();

        assertThrows(DataAccessException.class, () -> jdbcTemplate.update(
                "UPDATE documentos_assinaturas_log SET papel = ? WHERE id = ?",
                "ALTERADO", log.getId()));
    }

    @Test
    void bancoBloqueiaExclusaoDoLogDeAssinatura() {
        DocumentoAssinaturaLog log = criarLog();

        assertThrows(DataAccessException.class, () -> jdbcTemplate.update(
                "DELETE FROM documentos_assinaturas_log WHERE id = ?", log.getId()));
    }

    private DocumentoAssinaturaLog criarLog() {
        DocumentoInterno documento = new DocumentoInterno();
        documento.setTipo("VISTORIA_INICIAL_OS");
        documento.setStatus("REGISTRADO");
        documento.setConteudoJson("{}");
        documento.setCriadoPor("Teste");
        documento.setRecebidoPor("Teste");
        documento.setDataGeracao(LocalDateTime.now());
        documento = documentoRepository.saveAndFlush(documento);

        DocumentoAssinaturaLog log = new DocumentoAssinaturaLog();
        log.setDocumento(documento);
        log.setPapel("TECNICO");
        log.setNomeAssinante("Tecnico Teste");
        log.setRegistradoPor("JUnit");
        log.setRegistradoEm(LocalDateTime.now());
        log.setHashAssinatura("a".repeat(64));
        log.setHashDocumento("b".repeat(64));
        return assinaturaLogRepository.saveAndFlush(log);
    }
}

