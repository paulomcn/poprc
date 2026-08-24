package com.poprc.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.poprc.demo.model.Funcionario;
import com.poprc.demo.model.PerfilAcesso;
import com.poprc.demo.repository.FuncionarioRepository;
import com.poprc.demo.service.AutenticacaoLocalService;
import com.poprc.demo.service.AuditoriaAcessoService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class FuncionarioControllerTest {

    private FuncionarioRepository funcionarioRepository;
    private FuncionarioController controller;

    @BeforeEach
    void setUp() {
        funcionarioRepository = mock(FuncionarioRepository.class);
        controller = new FuncionarioController(
                funcionarioRepository,
                mock(AutenticacaoLocalService.class),
                mock(AuditoriaAcessoService.class));
    }

    @Test
    void naoPersisteFuncionarioQuandoSenhaTemporariaNaoTemCpf() {
        Funcionario funcionario = novoFuncionario();
        funcionario.setSenha("Temporaria123");

        ResponseEntity<Map<String, Object>> resposta = controller.inserirFuncionario(funcionario, null);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry(
                "erro", "Informe o CPF para configurar o acesso por senha.");
        verify(funcionarioRepository, never()).save(any(Funcionario.class));
    }

    @Test
    void impedeDesativarUltimoAdministradorAtivo() {
        Funcionario administrador = novoFuncionario();
        administrador.setId(1L);
        administrador.setPerfilAcesso(PerfilAcesso.ADMIN);
        administrador.setAtivo(true);
        Funcionario atualizacao = novoFuncionario();
        atualizacao.setPerfilAcesso(PerfilAcesso.ADMIN);
        atualizacao.setAtivo(false);
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(administrador));
        when(funcionarioRepository.countByPerfilAcessoAndAtivoTrue(PerfilAcesso.ADMIN)).thenReturn(1L);

        ResponseEntity<Map<String, Object>> resposta = controller.atualizarFuncionario(
                1L, atualizacao, null);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry(
                "erro", "O último administrador ativo não pode ser desativado nem ter o perfil alterado.");
        verify(funcionarioRepository, never()).save(any(Funcionario.class));
    }

    private Funcionario novoFuncionario() {
        Funcionario funcionario = new Funcionario();
        funcionario.setNome("Tecnico Teste");
        funcionario.setFuncao("Tecnico");
        funcionario.setCidade("Natal");
        return funcionario;
    }
}
