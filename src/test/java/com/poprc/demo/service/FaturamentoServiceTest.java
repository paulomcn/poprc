package com.poprc.demo.service;

import com.poprc.demo.model.Contrato;
import com.poprc.demo.model.Faturamento;
import com.poprc.demo.model.Projeto;
import com.poprc.demo.model.SituacaoFaturamento;
import com.poprc.demo.model.TipoContratante;
import com.poprc.demo.repository.ComarcaRepository;
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
    private ComarcaRepository comarcaRepository;
    private FaturamentoService service;
    private Contrato contrato;
    private Projeto projeto;

    @BeforeEach
    void setUp() {
        faturamentoRepository = mock(FaturamentoRepository.class);
        contratoRepository = mock(ContratoRepository.class);
        projetoRepository = mock(ProjetoRepository.class);
        comarcaRepository = mock(ComarcaRepository.class);
        service = new FaturamentoService(
                faturamentoRepository, contratoRepository, projetoRepository, comarcaRepository);

        contrato = new Contrato();
        contrato.setId(1L);
        contrato.setTipoContratante(TipoContratante.SETOR_PUBLICO);
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
        medicao.setServicosExecutados(null);
        medicao.setNumeroNotaFiscal("não deve permanecer");

        Faturamento salvo = service.registrarMedicao(medicao, 1L, 10L);

        assertEquals(contrato, salvo.getContrato());
        assertEquals(projeto, salvo.getProjeto());
        assertEquals(SituacaoFaturamento.A_FATURAR, salvo.getSituacao());
        assertNull(salvo.getNumeroNotaFiscal());
        assertNull(salvo.getServicosExecutados());
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

    @Test
    void deveCalcularNf14DaPlanilhaCentavoACentavo() {
        Faturamento faturamento = faturamentoEmitivel("23450.00");
        when(faturamentoRepository.findById(14L)).thenReturn(Optional.of(faturamento));

        Faturamento emitido = service.emitirNotaFiscal(
                14L, "14", LocalDate.of(2026, 7, 16), LocalDate.of(2026, 8, 30));

        assertEquals(LocalDate.of(2026, 8, 20), emitido.getCompetenciaFiscal());
        assertEquals(new BigDecimal("1125.60"), emitido.getImpostoRetido());
        assertEquals(new BigDecimal("3501.09"), emitido.getImpostoPagar());
        assertEquals(new BigDecimal("4626.69"), emitido.getImpostoTotal());
    }

    @Test
    void deveCalcularNf15DaPlanilhaCentavoACentavo() {
        Faturamento faturamento = faturamentoEmitivel("30524.00");
        when(faturamentoRepository.findById(15L)).thenReturn(Optional.of(faturamento));

        Faturamento emitido = service.emitirNotaFiscal(
                15L, "15", LocalDate.of(2026, 7, 16), LocalDate.of(2026, 8, 30));

        assertEquals(new BigDecimal("1465.15"), emitido.getImpostoRetido());
        assertEquals(new BigDecimal("4557.23"), emitido.getImpostoPagar());
        assertEquals(new BigDecimal("6022.38"), emitido.getImpostoTotal());
    }

    @Test
    void deveLevarCompetenciaDeDezembroParaJaneiro() {
        Faturamento faturamento = faturamentoEmitivel("1000.00");
        when(faturamentoRepository.findById(20L)).thenReturn(Optional.of(faturamento));

        Faturamento emitido = service.emitirNotaFiscal(
                20L, "20", LocalDate.of(2026, 12, 31), LocalDate.of(2027, 1, 31));

        assertEquals(LocalDate.of(2027, 1, 20), emitido.getCompetenciaFiscal());
    }

    @Test
    void deveBloquearNotaDuplicadaNoMesmoContrato() {
        Faturamento faturamento = faturamentoEmitivel("1000.00");
        when(faturamentoRepository.findById(30L)).thenReturn(Optional.of(faturamento));
        when(faturamentoRepository.existsByContratoIdAndNumeroNotaFiscalIgnoreCase(1L, "NF-30"))
                .thenReturn(true);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> service.emitirNotaFiscal(
                        30L, "NF-30", LocalDate.of(2026, 7, 16), LocalDate.of(2026, 8, 16)));

        assertEquals("Já existe uma nota fiscal com este número no contrato.", erro.getMessage());
    }

    @Test
    void devePersistirDataDoPagamento() {
        Faturamento faturamento = faturamentoEmitivel("1000.00");
        faturamento.setSituacao(SituacaoFaturamento.FATURADO);
        faturamento.setDataEmissao(LocalDate.now().minusDays(10));
        when(faturamentoRepository.findById(40L)).thenReturn(Optional.of(faturamento));

        LocalDate pagamento = LocalDate.now().minusDays(1);
        Faturamento pago = service.darBaixaPagamento(40L, pagamento);

        assertEquals(SituacaoFaturamento.PAGO, pago.getSituacao());
        assertEquals(pagamento, pago.getDataPagamento());
    }

    private Faturamento medicaoValida() {
        Faturamento faturamento = new Faturamento();
        faturamento.setServicosExecutados("Instalação e certificação");
        faturamento.setValorMedicao(new BigDecimal("1500.00"));
        return faturamento;
    }

    private Faturamento faturamentoEmitivel(String valor) {
        Faturamento faturamento = medicaoValida();
        faturamento.setValorMedicao(new BigDecimal(valor));
        faturamento.setContrato(contrato);
        faturamento.setProjeto(projeto);
        faturamento.setSituacao(SituacaoFaturamento.A_FATURAR);
        return faturamento;
    }
}
