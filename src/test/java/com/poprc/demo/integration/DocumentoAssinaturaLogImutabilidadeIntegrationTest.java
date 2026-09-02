package com.poprc.demo.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.poprc.demo.controller.DocumentoInternoController;
import com.poprc.demo.model.DocumentoAssinaturaLog;
import com.poprc.demo.model.DocumentoInterno;
import com.poprc.demo.repository.DocumentoAssinaturaLogRepository;
import com.poprc.demo.repository.DocumentoInternoRepository;
import com.poprc.demo.security.UsuarioAutenticado;

import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DocumentoAssinaturaLogImutabilidadeIntegrationTest {

    @Autowired
    private DocumentoInternoRepository documentoRepository;

    @Autowired
    private DocumentoAssinaturaLogRepository assinaturaLogRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DocumentoInternoController documentoController;

    @Autowired
    private EntityManager entityManager;

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

    @Test
    void hashPermaneceIntegroDepoisDeRecarregarDocumentoAssinadoDoBanco() {
        DocumentoInterno documento = new DocumentoInterno();
        documento.setTipo("ENCERRAMENTO_OS");
        documento.setStatus("PENDENTE_ASSINATURA");
        documento.setConteudoJson("{}");
        documento.setCriadoPor("Sistema");
        documento.setRecebidoPor("Sistema");
        documento.setDataGeracao(LocalDateTime.now());
        documento = documentoRepository.saveAndFlush(documento);
        entityManager.clear();

        DocumentoInternoController.AssinaturaPapelRequest request =
                new DocumentoInternoController.AssinaturaPapelRequest();
        request.setNomeAssinante("Assinatura de homologacao");
        request.setAssinaturaBase64(gerarAssinaturaPng());
        Authentication authentication = autenticacaoSistema();
        documentoController.assinarDocumentoPorPapel(
                documento.getId(), "TECNICO", request, authentication);
        entityManager.flush();
        entityManager.clear();

        ResponseEntity<Map<String, Object>> resposta = documentoController.verificarIntegridade(
                documento.getId(), authentication);

        assertEquals(Boolean.TRUE, resposta.getBody().get("integro"));
        assertEquals("INTEGRO", resposta.getBody().get("situacao"));
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

    private String gerarAssinaturaPng() {
        try {
            BufferedImage imagem = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(imagem, "png", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception ex) {
            throw new IllegalStateException("Nao foi possivel preparar a assinatura do teste.", ex);
        }
    }

    private Authentication autenticacaoSistema() {
        UsuarioAutenticado usuario = new UsuarioAutenticado(
                1L, "Sistema", null, "ADMIN", "CPF_SENHA", false);
        return new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
    }
}
