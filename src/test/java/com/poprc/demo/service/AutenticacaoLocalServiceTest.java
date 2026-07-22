package com.poprc.demo.service;

import com.poprc.demo.model.Funcionario;
import com.poprc.demo.repository.FuncionarioRepository;
import com.poprc.demo.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AutenticacaoLocalServiceTest {
    private FuncionarioRepository repository;
    private PasswordEncoder encoder;
    private AutenticacaoLocalService service;
    private Funcionario funcionario;

    @BeforeEach
    void setUp() {
        repository = mock(FuncionarioRepository.class);
        encoder = mock(PasswordEncoder.class);
        service = new AutenticacaoLocalService(repository, encoder);
        funcionario = new Funcionario();
        funcionario.setId(7L);
        funcionario.setNome("Tecnico Teste");
        funcionario.setCpf("52998224725");
        funcionario.setSenhaHash("hash");
        funcionario.setAtivo(true);
        when(repository.findByCpf("52998224725")).thenReturn(Optional.of(funcionario));
        when(repository.save(funcionario)).thenReturn(funcionario);
    }

    @Test
    void deveAutenticarComCpfFormatadoESenhaCorreta() {
        when(encoder.matches("Senha123", "hash")).thenReturn(true);

        UsuarioAutenticado usuario = service.autenticar("529.982.247-25", "Senha123");

        assertEquals(7L, usuario.getFuncionarioId());
        assertEquals("CPF_SENHA", usuario.getMetodoAutenticacao());
        verify(repository).save(funcionario);
    }

    @Test
    void deveBloquearTemporariamenteAposCincoFalhas() {
        when(encoder.matches("errada", "hash")).thenReturn(false);

        for (int tentativa = 0; tentativa < 5; tentativa++) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.autenticar("52998224725", "errada"));
        }

        assertNotNull(funcionario.getBloqueadoAte());
        assertEquals(0, funcionario.getTentativasLogin());
    }

    @Test
    void deveExigirSenhaComLetrasENumeros() {
        assertThrows(IllegalArgumentException.class, () -> service.validarNovaSenha("12345678"));
        assertThrows(IllegalArgumentException.class, () -> service.validarNovaSenha("abcdefgh"));
        service.validarNovaSenha("Senha123");
        assertTrue(true);
    }

    @Test
    void devePermitirDefinirSenhaDefinitivaNoPrimeiroAcessoSemRepetirTemporaria() {
        funcionario.setTrocaSenhaObrigatoria(true);
        when(repository.findById(7L)).thenReturn(Optional.of(funcionario));
        when(encoder.encode("NovaSenha123")).thenReturn("novo-hash");

        Funcionario atualizado = service.alterarSenha(7L, null, "NovaSenha123", true);

        assertEquals("novo-hash", atualizado.getSenhaHash());
        assertEquals(false, atualizado.getTrocaSenhaObrigatoria());
        verify(repository).save(funcionario);
    }
}
