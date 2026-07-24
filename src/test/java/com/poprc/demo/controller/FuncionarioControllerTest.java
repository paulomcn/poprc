package com.poprc.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.poprc.demo.model.Funcionario;
import com.poprc.demo.repository.FuncionarioRepository;
import com.poprc.demo.service.AutenticacaoLocalService;
import java.util.Map;
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
                mock(AutenticacaoLocalService.class));
    }

    @Test
    void naoPersisteFuncionarioQuandoSenhaTemporariaNaoTemCpf() {
        Funcionario funcionario = novoFuncionario();
        funcionario.setSenha("Temporaria123");

        ResponseEntity<Map<String, Object>> resposta = controller.inserirFuncionario(funcionario);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry(
                "erro", "Informe o CPF para configurar o acesso por senha.");
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
