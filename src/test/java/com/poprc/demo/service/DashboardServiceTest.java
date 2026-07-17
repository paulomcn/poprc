package com.poprc.demo.service;

import com.poprc.demo.dto.DashboardIndicadoresDTO;
import com.poprc.demo.model.Comarca;
import com.poprc.demo.model.Contrato;
import com.poprc.demo.model.OrdemServico;
import com.poprc.demo.model.Projeto;
import com.poprc.demo.model.StatusOS;
import com.poprc.demo.repository.ComarcaRepository;
import com.poprc.demo.repository.ContratoRepository;
import com.poprc.demo.repository.FaturamentoRepository;
import com.poprc.demo.repository.OrdemServicoRepository;
import com.poprc.demo.repository.PrestacaoContasRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardServiceTest {

    private ContratoRepository contratoRepository;
    private FaturamentoRepository faturamentoRepository;
    private ComarcaRepository comarcaRepository;
    private PrestacaoContasRepository prestacaoContasRepository;
    private OrdemServicoRepository ordemServicoRepository;
    private DashboardService service;

    @BeforeEach
    void setUp() {
        contratoRepository = mock(ContratoRepository.class);
        faturamentoRepository = mock(FaturamentoRepository.class);
        comarcaRepository = mock(ComarcaRepository.class);
        prestacaoContasRepository = mock(PrestacaoContasRepository.class);
        ordemServicoRepository = mock(OrdemServicoRepository.class);
        service = new DashboardService(
                contratoRepository,
                faturamentoRepository,
                comarcaRepository,
                prestacaoContasRepository,
                ordemServicoRepository);

        when(faturamentoRepository.findAll()).thenReturn(List.of());
        when(prestacaoContasRepository.findAll()).thenReturn(List.of());
    }

    @Test
    void deveCalcularPrazosEFluxoComDadosNaoArquivados() {
        Contrato contrato = contrato(1L, "CT-01", false);
        Contrato arquivado = contrato(2L, "CT-02", true);
        when(contratoRepository.findAll()).thenReturn(List.of(contrato, arquivado));

        OrdemServico atrasada = ordem(1L, contrato, StatusOS.ABERTA, LocalDateTime.now().minusHours(2), false);
        OrdemServico proxima = ordem(2L, contrato, StatusOS.EM_EXECUCAO, LocalDateTime.now().plusHours(5), false);
        OrdemServico concluida = ordem(3L, contrato, StatusOS.CONCLUIDA, LocalDateTime.now().minusDays(1), false);
        OrdemServico ignorada = ordem(4L, arquivado, StatusOS.ABERTA, LocalDateTime.now().minusDays(2), false);
        when(ordemServicoRepository.findAll()).thenReturn(List.of(atrasada, proxima, concluida, ignorada));

        Projeto projeto = new Projeto();
        projeto.setContrato(contrato);
        when(comarcaRepository.findAll()).thenReturn(List.of(
                obra(projeto, 1, false),
                obra(projeto, 2, false),
                obra(projeto, 3, false),
                obra(projeto, 3, true)));

        DashboardIndicadoresDTO resultado = service.calcularIndicadores("CT-01", null, null);

        assertEquals(1L, resultado.getContratosAtivos());
        assertEquals(new BigDecimal("1000"), resultado.getValorTotalContratado());
        assertEquals(3L, resultado.getTotalOrdensServico());
        assertEquals(1L, resultado.getOrdensAbertas());
        assertEquals(1L, resultado.getOrdensEmExecucao());
        assertEquals(1L, resultado.getOrdensConcluidas());
        assertEquals(1L, resultado.getTotalComarcasEmAtraso());
        assertEquals(1L, resultado.getOrdensProximasPrazo());
        assertEquals(1L, resultado.getObrasEmVistoria());
        assertEquals(1L, resultado.getObrasEmInfraestrutura());
        assertEquals(1L, resultado.getObrasEmViradaRede());
        assertEquals(1L, resultado.getTotalComarcasConcluidas());
    }

    private Contrato contrato(Long id, String numero, boolean arquivado) {
        Contrato contrato = new Contrato();
        contrato.setId(id);
        contrato.setContrato(numero);
        contrato.setStatus("ATIVO");
        contrato.setValorGlobal(new BigDecimal("1000"));
        contrato.setArquivado(arquivado);
        return contrato;
    }

    private OrdemServico ordem(
            Long id,
            Contrato contrato,
            StatusOS status,
            LocalDateTime deadline,
            boolean arquivada) {
        OrdemServico ordem = new OrdemServico();
        ordem.setId(id);
        ordem.setContrato(contrato);
        ordem.setStatus(status);
        ordem.setDeadline(deadline);
        ordem.setArquivado(arquivada);
        return ordem;
    }

    private Comarca obra(Projeto projeto, int etapa, boolean concluida) {
        Comarca comarca = new Comarca();
        comarca.setProjeto(projeto);
        comarca.setEtapaAtual(etapa);
        comarca.setViradaRedeConcluida(concluida);
        comarca.setArquivado(false);
        return comarca;
    }
}
