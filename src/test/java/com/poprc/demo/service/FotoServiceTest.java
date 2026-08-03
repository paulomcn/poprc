package com.poprc.demo.service;

import com.poprc.demo.model.EvidenciaFoto;
import com.poprc.demo.model.Funcionario;
import com.poprc.demo.model.OrdemServico;
import com.poprc.demo.model.StatusOS;
import com.poprc.demo.repository.EvidenciaFotoRepository;
import com.poprc.demo.repository.FuncionarioRepository;
import com.poprc.demo.repository.OrdemServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

class FotoServiceTest {

    private EvidenciaFotoRepository fotoRepository;
    private FuncionarioRepository funcionarioRepository;
    private OrdemServicoRepository ordemServicoRepository;
    private FotoService fotoService;

    @BeforeEach
    void setUp() {
        fotoRepository = mock(EvidenciaFotoRepository.class);
        funcionarioRepository = mock(FuncionarioRepository.class);
        ordemServicoRepository = mock(OrdemServicoRepository.class);
        fotoService = new FotoService(fotoRepository, funcionarioRepository, ordemServicoRepository);
    }

    @Test
    void deveRejeitarFormatoNaoPermitidoAntesDeConsultarBanco() {
        MockMultipartFile arquivo = new MockMultipartFile(
                "file", "evidencia.pdf", "application/pdf", "%PDF".getBytes());

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> fotoService.salvarEvidencia(arquivo, 1L, 1L, "-12.9", "-38.5"));

        assertEquals("Formato inválido. Envie uma imagem JPG ou PNG.", erro.getMessage());
        verifyNoInteractions(fotoRepository, funcionarioRepository, ordemServicoRepository);
    }

    @Test
    void deveRejeitarConteudoFalsoMesmoComMimeDeImagem() {
        MockMultipartFile arquivo = new MockMultipartFile(
                "file", "evidencia.png", "image/png", "nao-e-uma-imagem".getBytes());

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> fotoService.salvarEvidencia(arquivo, 1L, 1L, "-12.9", "-38.5"));

        assertEquals("O arquivo enviado não é uma imagem válida.", erro.getMessage());
        verifyNoInteractions(fotoRepository, funcionarioRepository, ordemServicoRepository);
    }

    @Test
    void deveRemoverRegistroDaEvidenciaDoProprioTecnico() {
        Funcionario funcionario = new Funcionario();
        funcionario.setId(7L);
        OrdemServico ordemServico = new OrdemServico();
        ordemServico.setStatus(StatusOS.EM_EXECUCAO);
        EvidenciaFoto evidencia = new EvidenciaFoto();
        evidencia.setId(11L);
        evidencia.setFuncionario(funcionario);
        evidencia.setOrdemServico(ordemServico);
        evidencia.setCaminhoArquivo(null);
        when(fotoRepository.findById(11L)).thenReturn(Optional.of(evidencia));

        fotoService.removerEvidencia(11L, 7L);

        verify(fotoRepository).delete(evidencia);
    }

    @Test
    void deveImpedirOutroTecnicoDeRemoverEvidencia() {
        Funcionario funcionario = new Funcionario();
        funcionario.setId(7L);
        OrdemServico ordemServico = new OrdemServico();
        ordemServico.setStatus(StatusOS.EM_EXECUCAO);
        EvidenciaFoto evidencia = new EvidenciaFoto();
        evidencia.setFuncionario(funcionario);
        evidencia.setOrdemServico(ordemServico);
        when(fotoRepository.findById(11L)).thenReturn(Optional.of(evidencia));

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> fotoService.removerEvidencia(11L, 8L));

        assertEquals("Apenas o técnico que enviou a evidência pode removê-la.", erro.getMessage());
        verify(fotoRepository, never()).delete(evidencia);
    }

    @Test
    void deveImpedirRemocaoAposConclusaoDaOs() {
        Funcionario funcionario = new Funcionario();
        funcionario.setId(7L);
        OrdemServico ordemServico = new OrdemServico();
        ordemServico.setStatus(StatusOS.CONCLUIDA);
        EvidenciaFoto evidencia = new EvidenciaFoto();
        evidencia.setFuncionario(funcionario);
        evidencia.setOrdemServico(ordemServico);
        when(fotoRepository.findById(11L)).thenReturn(Optional.of(evidencia));

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> fotoService.removerEvidencia(11L, 7L));

        assertEquals("As evidências não podem ser alteradas após o envio da OS para validação.", erro.getMessage());
        verify(fotoRepository, never()).delete(evidencia);
    }
}
