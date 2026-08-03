package com.poprc.demo.service;

import com.poprc.demo.dto.EquipeProjetoRequest;
import com.poprc.demo.model.Funcionario;
import com.poprc.demo.model.Projeto;
import com.poprc.demo.model.ProjetoMembro;
import com.poprc.demo.repository.FuncionarioRepository;
import com.poprc.demo.repository.ProjetoMembroRepository;
import com.poprc.demo.repository.ProjetoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjetoEquipeServiceTest {

    private ProjetoRepository projetoRepository;
    private ProjetoMembroRepository membroRepository;
    private FuncionarioRepository funcionarioRepository;
    private ProjetoEquipeService service;
    private Projeto projeto;

    @BeforeEach
    void setUp() {
        projetoRepository = mock(ProjetoRepository.class);
        membroRepository = mock(ProjetoMembroRepository.class);
        funcionarioRepository = mock(FuncionarioRepository.class);
        service = new ProjetoEquipeService(projetoRepository, membroRepository, funcionarioRepository);
        projeto = new Projeto();
        projeto.setId(1L);
        when(projetoRepository.findById(1L)).thenReturn(Optional.of(projeto));
        when(membroRepository.saveAll(anyList())).thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    @Test
    void deveSalvarEquipeESincronizarResponsavelPrincipal() {
        Funcionario supervisor = funcionario(10L, "Supervisora");
        Funcionario tecnico = funcionario(11L, "Técnico");
        when(funcionarioRepository.findById(10L)).thenReturn(Optional.of(supervisor));
        when(funcionarioRepository.findById(11L)).thenReturn(Optional.of(tecnico));

        List<ProjetoMembro> equipe = service.substituirEquipe(1L, request(
                membro(10L, "LIDER_EQUIPE", true),
                membro(11L, "TECNICO", false)));

        assertEquals(2, equipe.size());
        assertEquals(supervisor, projeto.getResponsavel());
        assertTrue(equipe.get(0).getResponsavelPrincipal());
        verify(membroRepository).deleteByProjetoId(1L);
        verify(projetoRepository).save(projeto);
    }

    @Test
    void deveBloquearFuncionarioDuplicado() {
        when(funcionarioRepository.findById(10L)).thenReturn(Optional.of(funcionario(10L, "Técnico")));

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> service.substituirEquipe(1L, request(
                        membro(10L, "LIDER_EQUIPE", true),
                        membro(10L, "TECNICO", false))));

        assertTrue(erro.getMessage().contains("apenas uma vez"));
    }

    @Test
    void deveExigirExatamenteUmPrincipal() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> service.substituirEquipe(1L, request(
                        membro(10L, "TECNICO", false),
                        membro(11L, "TECNICO", false))));

        assertTrue(erro.getMessage().contains("exatamente um responsável principal"));
    }

    @Test
    void deveBloquearAlteracaoEmProjetoArquivado() {
        projeto.setArquivado(true);

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> service.substituirEquipe(1L, request(membro(10L, "TECNICO", true))));

        assertTrue(erro.getMessage().contains("projeto arquivado"));
    }

    private Funcionario funcionario(Long id, String nome) {
        Funcionario funcionario = new Funcionario();
        funcionario.setId(id);
        funcionario.setNome(nome);
        return funcionario;
    }

    private EquipeProjetoRequest request(EquipeProjetoRequest.MembroRequest... membros) {
        EquipeProjetoRequest request = new EquipeProjetoRequest();
        request.setMembros(List.of(membros));
        return request;
    }

    private EquipeProjetoRequest.MembroRequest membro(
            Long funcionarioId, String papel, boolean principal) {
        EquipeProjetoRequest.MembroRequest membro = new EquipeProjetoRequest.MembroRequest();
        membro.setFuncionarioId(funcionarioId);
        membro.setPapel(papel);
        membro.setResponsavelPrincipal(principal);
        return membro;
    }
}
