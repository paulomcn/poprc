package com.poprc.demo.service;

import com.poprc.demo.model.Contrato;
import com.poprc.demo.model.Faturamento;
import com.poprc.demo.model.Projeto;
import com.poprc.demo.model.SituacaoFaturamento;
import com.poprc.demo.repository.ContratoRepository;
import com.poprc.demo.repository.FaturamentoRepository;
import com.poprc.demo.repository.ProjetoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FaturamentoServiceTest {

    private FaturamentoRepository faturamentoRepository;
    private ContratoRepository contratoRepository;
    private ProjetoRepository projetoRepository;
    private FaturamentoService service;
    private Contrato contrato;
    private Projeto projeto;

    @BeforeEach
    void setUp() {
        faturamentoRepository = mock(FaturamentoRepository.class);
        contratoRepository = mock(ContratoRepository.class);
        projetoRepository = mock(ProjetoRepository.class);
        service = new FaturamentoService(faturamentoRepository, contratoRepository, projetoRepository);

        contrato = new Contrato();
        contrato.setId(1L);
        projeto = new Projeto();
        projeto.setId(10L);
        projeto.setContrato(contrato);
        projeto.setArquivado(false);
        when(contratoRepository.findById(1L)).thenReturn(Optional.of(contrato));
        when(projetoRepository.findById(10L)).thenReturn(Optional.of(projeto));
        when(faturamentoRepository.save(any(Faturamento.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    @Test
    void deveCriarMedicaoVinculadaAoProjetoComoAFaturar() {
        Faturamento medicao = medicaoValida();
        medicao.setNumeroNotaFiscal("não deve permanecer");

        Faturamento salvo = service.registrarMedicao(medicao, 1L, 10L);

        assertEquals(contrato, salvo.getContrato());
        assertEquals(projeto, salvo.getProjeto());
        assertEquals(SituacaoFaturamento.A_FATURAR, salvo.getSituacao());
        assertNull(salvo.getNumeroNotaFiscal());
    }

    @Test
    void deveRejeitarProjetoDeOutroContrato() {
        Contrato outroContrato = new Contrato();
        outroContrato.setId(2L);
        projeto.setContrato(outroContrato);

        assertThrows(IllegalArgumentException.class,
                () -> service.registrarMedicao(medicaoValida(), 1L, 10L));
    }

    @Test
    void deveImpedirEdicaoDepoisDaEmissaoDaNota() {
        Faturamento existente = medicaoValida();
        existente.setSituacao(SituacaoFaturamento.FATURADO);
        when(faturamentoRepository.findById(5L)).thenReturn(Optional.of(existente));

        assertThrows(IllegalStateException.class,
                () -> service.atualizarMedicao(5L, medicaoValida(), 1L, 10L));
    }

    @Test
    void deveMarcarFaturamentoVencidoComoAtrasado() {
        Faturamento vencido = medicaoValida();
        vencido.setSituacao(SituacaoFaturamento.FATURADO);
        vencido.setDataVencimento(LocalDate.now().minusDays(1));
        when(faturamentoRepository.findAll()).thenReturn(List.of(vencido));
        when(faturamentoRepository.saveAll(anyList()))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        List<Faturamento> resultado = service.listarTodos();

        assertEquals(SituacaoFaturamento.EM_ATRASO, resultado.get(0).getSituacao());
        verify(faturamentoRepository).saveAll(anyList());
    }

    private Faturamento medicaoValida() {
        Faturamento faturamento = new Faturamento();
        faturamento.setServicosExecutados("Instalação e certificação");
        faturamento.setValorMedicao(new BigDecimal("1500.00"));
        return faturamento;
    }
}
