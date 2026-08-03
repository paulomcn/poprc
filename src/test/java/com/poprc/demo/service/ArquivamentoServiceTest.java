package com.poprc.demo.service;

import com.poprc.demo.model.Contrato;
import com.poprc.demo.model.OrdemRetirada;
import com.poprc.demo.model.OrdemServico;
import com.poprc.demo.model.Projeto;
import com.poprc.demo.model.StatusOS;
import com.poprc.demo.repository.ComarcaRepository;
import com.poprc.demo.repository.ContratoRepository;
import com.poprc.demo.repository.OrdemRetiradaRepository;
import com.poprc.demo.repository.OrdemServicoRepository;
import com.poprc.demo.repository.ProjetoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArquivamentoServiceTest {

    private ContratoRepository contratoRepository;
    private ProjetoRepository projetoRepository;
    private ComarcaRepository comarcaRepository;
    private OrdemServicoRepository ordemServicoRepository;
    private OrdemRetiradaRepository ordemRetiradaRepository;
    private ArquivamentoService service;

    @BeforeEach
    void setUp() {
        contratoRepository = mock(ContratoRepository.class);
        projetoRepository = mock(ProjetoRepository.class);
        comarcaRepository = mock(ComarcaRepository.class);
        ordemServicoRepository = mock(OrdemServicoRepository.class);
        ordemRetiradaRepository = mock(OrdemRetiradaRepository.class);
        service = new ArquivamentoService(
                contratoRepository,
                projetoRepository,
                comarcaRepository,
                ordemServicoRepository,
                ordemRetiradaRepository);
    }

    @Test
    void deveArquivarOsCriadaPorEnganoSemRetirada() {
        OrdemServico ordem = new OrdemServico();
        ordem.setId(10L);
        ordem.setStatus(StatusOS.ABERTA);
        when(ordemServicoRepository.findById(10L)).thenReturn(Optional.of(ordem));
        when(ordemRetiradaRepository.findByOrdemServicoIdOrderByDataGeracaoDesc(10L)).thenReturn(List.of());
        when(ordemServicoRepository.save(ordem)).thenReturn(ordem);

        OrdemServico arquivada = service.arquivarOrdemServico(10L, "Gestor", "Cadastro duplicado");

        assertTrue(arquivada.getArquivado());
        assertEquals("Gestor", arquivada.getArquivadoPor());
        assertEquals("Cadastro duplicado", arquivada.getMotivoArquivamento());
        assertNotNull(arquivada.getArquivadoEm());
        verify(ordemServicoRepository).save(ordem);
    }

    @Test
    void deveBloquearOsComOrdemRetiradaAberta() {
        OrdemServico ordem = new OrdemServico();
        ordem.setId(11L);
        ordem.setStatus(StatusOS.CONCLUIDA);
        OrdemRetirada retirada = new OrdemRetirada();
        retirada.setStatus("RETIRADA");
        when(ordemServicoRepository.findById(11L)).thenReturn(Optional.of(ordem));
        when(ordemRetiradaRepository.findByOrdemServicoIdOrderByDataGeracaoDesc(11L))
                .thenReturn(List.of(retirada));

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> service.arquivarOrdemServico(11L, "Gestor", "Encerramento"));

        assertTrue(erro.getMessage().contains("ainda não devolvida"));
    }

    @Test
    void deveBloquearContratoComProjetoAtivo() {
        Contrato contrato = new Contrato();
        contrato.setId(20L);
        Projeto projeto = new Projeto();
        projeto.setArquivado(false);
        when(contratoRepository.findById(20L)).thenReturn(Optional.of(contrato));
        when(projetoRepository.findByContratoId(20L)).thenReturn(List.of(projeto));

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> service.arquivarContrato(20L, "Gestor", "Contrato encerrado"));

        assertTrue(erro.getMessage().contains("projetos ativos"));
    }

    @Test
    void deveExigirRestauracaoDoContratoAntesDoProjeto() {
        Contrato contrato = new Contrato();
        contrato.setArquivado(true);
        Projeto projeto = new Projeto();
        projeto.setId(30L);
        projeto.setArquivado(true);
        projeto.setContrato(contrato);
        when(projetoRepository.findById(30L)).thenReturn(Optional.of(projeto));

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> service.restaurarProjeto(30L));

        assertTrue(erro.getMessage().contains("Restaure primeiro o contrato"));
    }

    @Test
    void deveRestaurarContratoELimparMetadados() {
        Contrato contrato = new Contrato();
        contrato.setId(40L);
        contrato.setArquivado(true);
        contrato.setArquivadoPor("Gestor");
        contrato.setMotivoArquivamento("Teste");
        when(contratoRepository.findById(40L)).thenReturn(Optional.of(contrato));
        when(contratoRepository.save(any(Contrato.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Contrato restaurado = service.restaurarContrato(40L);

        assertEquals(false, restaurado.getArquivado());
        assertEquals(null, restaurado.getArquivadoPor());
        assertEquals(null, restaurado.getMotivoArquivamento());
    }
}
