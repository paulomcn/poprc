package com.poprc.demo.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.poprc.demo.model.Contrato;
import com.poprc.demo.model.TipoContratante;
import com.poprc.demo.repository.ContratoRepository;
import com.poprc.demo.service.ArquivamentoService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ContratoControllerTest {

    private ContratoController controller;

    @BeforeEach
    void setUp() {
        ContratoRepository contratoRepository = mock(ContratoRepository.class);
        ArquivamentoService arquivamentoService = mock(ArquivamentoService.class);
        controller = new ContratoController(contratoRepository, arquivamentoService);
        when(contratoRepository.save(any(Contrato.class))).thenAnswer(invocacao -> {
            Contrato contrato = invocacao.getArgument(0);
            contrato.setId(1L);
            return contrato;
        });
    }

    @Test
    void criaContratoPrivadoSemExigirCampoTecnicoDeArquivamento() {
        Contrato contrato = new Contrato();
        contrato.setCliente("Cliente privado");
        contrato.setContrato("CONTRATO-PRIVADO-01");
        contrato.setTipoContratante(TipoContratante.SETOR_PRIVADO);
        contrato.setArquivado(null);

        ResponseEntity<Map<String, Object>> resposta = controller.salvarContrato(contrato);

        assertEquals(HttpStatus.CREATED, resposta.getStatusCode());
        Contrato salvo = (Contrato) resposta.getBody().get("contrato");
        assertFalse(salvo.getArquivado());
        assertEquals(TipoContratante.SETOR_PRIVADO, salvo.getTipoContratante());
    }
}
