package com.poprc.demo.service;

import com.poprc.demo.model.Funcionario;
import com.poprc.demo.model.RegistroPonto;
import com.poprc.demo.model.TipoPonto;
import com.poprc.demo.repository.FuncionarioRepository;
import com.poprc.demo.repository.RegistroPontoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PontoServiceTest {

    private RegistroPontoRepository pontoRepository;
    private FuncionarioRepository funcionarioRepository;
    private PontoService pontoService;
    private Funcionario funcionario;

    @BeforeEach
    void setUp() {
        pontoRepository = mock(RegistroPontoRepository.class);
        funcionarioRepository = mock(FuncionarioRepository.class);
        pontoService = new PontoService(pontoRepository, funcionarioRepository);
        funcionario = new Funcionario();
        funcionario.setId(7L);
        funcionario.setNome("Tecnico Teste");
        when(funcionarioRepository.findById(7L)).thenReturn(Optional.of(funcionario));
    }

    @Test
    void deveRegistrarEntradaQuandoUltimoPontoForSaida() {
        RegistroPonto ultimaSaida = new RegistroPonto();
        ultimaSaida.setTipo(TipoPonto.SAIDA);
        when(pontoRepository.findTopByFuncionarioIdOrderByDataHoraDesc(7L))
                .thenReturn(Optional.of(ultimaSaida));
        when(pontoRepository.save(org.mockito.ArgumentMatchers.any(RegistroPonto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        pontoService.salvarPonto(7L, TipoPonto.ENTRADA, "-12.971400", "-38.501400");

        ArgumentCaptor<RegistroPonto> captor = ArgumentCaptor.forClass(RegistroPonto.class);
        verify(pontoRepository).save(captor.capture());
        assertEquals(TipoPonto.ENTRADA, captor.getValue().getTipo());
        assertEquals(funcionario, captor.getValue().getFuncionario());
    }

    @Test
    void deveRejeitarEntradaDuplicada() {
        RegistroPonto ultimaEntrada = new RegistroPonto();
        ultimaEntrada.setTipo(TipoPonto.ENTRADA);
        when(pontoRepository.findTopByFuncionarioIdOrderByDataHoraDesc(7L))
                .thenReturn(Optional.of(ultimaEntrada));

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> pontoService.salvarPonto(7L, TipoPonto.ENTRADA, "-12.9", "-38.5"));

        assertEquals("Já existe uma entrada aberta para este funcionário.", erro.getMessage());
        verify(pontoRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deveRejeitarSaidaSemEntradaAberta() {
        when(pontoRepository.findTopByFuncionarioIdOrderByDataHoraDesc(7L))
                .thenReturn(Optional.empty());

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> pontoService.salvarPonto(7L, TipoPonto.SAIDA, "-12.9", "-38.5"));

        assertEquals("Não existe uma entrada aberta para registrar a saída.", erro.getMessage());
        verify(pontoRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deveRejeitarCoordenadaForaDoIntervalo() {
        assertThrows(IllegalArgumentException.class,
                () -> pontoService.salvarPonto(7L, TipoPonto.ENTRADA, "91", "-38.5"));

        verify(funcionarioRepository, never()).findById(org.mockito.ArgumentMatchers.any());
    }
}
