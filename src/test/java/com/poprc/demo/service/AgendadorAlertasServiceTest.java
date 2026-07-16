package com.poprc.demo.service;

import com.poprc.demo.model.ConfiguracaoNotificacao;
import com.poprc.demo.model.LocalEstoque;
import com.poprc.demo.model.Material;
import com.poprc.demo.model.OrdemServico;
import com.poprc.demo.model.SaldoMaterialLocal;
import com.poprc.demo.model.StatusOS;
import com.poprc.demo.model.TipoControleEstoque;
import com.poprc.demo.model.UnidadeMedida;
import com.poprc.demo.repository.ConfiguracaoNotificacaoRepository;
import com.poprc.demo.repository.ContratoRepository;
import com.poprc.demo.repository.OrdemServicoRepository;
import com.poprc.demo.repository.SaldoMaterialLocalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgendadorAlertasServiceTest {

    private SaldoMaterialLocalRepository saldoRepository;
    private OrdemServicoRepository osRepository;
    private ConfiguracaoNotificacaoRepository configRepository;
    private NotificacaoOperacionalService notificacaoService;
    private AgendadorAlertasService service;

    @BeforeEach
    void setUp() {
        osRepository = mock(OrdemServicoRepository.class);
        saldoRepository = mock(SaldoMaterialLocalRepository.class);
        ContratoRepository contratoRepository = mock(ContratoRepository.class);
        configRepository = mock(ConfiguracaoNotificacaoRepository.class);
        notificacaoService = mock(NotificacaoOperacionalService.class);
        when(configRepository.findById(1L)).thenReturn(Optional.of(
                new ConfiguracaoNotificacao(1L, "", "", false, true, false, 24, 30)));
        service = new AgendadorAlertasService(
                osRepository, saldoRepository, contratoRepository, configRepository, notificacaoService);
    }

    @Test
    void deveAlertarPorSaldoLivreDoDepositoEUsarChaveEstavel() {
        Material material = material(10L, "Conector", TipoControleEstoque.UNIDADE,
                UnidadeMedida.UNIDADE, "10");
        SaldoMaterialLocal saldo = saldo(20L, "Central", material, 12, 3, "0", "0");
        when(saldoRepository.findAllByOrderByMaterialNomeAscLocalEstoqueNomeAsc())
                .thenReturn(List.of(saldo));
        when(notificacaoService.registrarSeAusente(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(true);

        AgendadorAlertasService.VarreduraResultado resultado = service.executarVarreduraDiariaDeAlertas();

        assertEquals(1, resultado.alertasEncontrados());
        assertEquals(1, resultado.notificacoesCriadas());
        ArgumentCaptor<String> mensagem = ArgumentCaptor.forClass(String.class);
        verify(notificacaoService).registrarSeAusente(
                eq("ESTOQUE_CRITICO:10:20"), eq("ESTOQUE_CRITICO"), eq("ALERTA"),
                eq("Estoque crítico: Conector em Central"), mensagem.capture(), eq(null), eq(null));
        assertTrue(mensagem.getValue().contains("Saldo livre: 9 UNIDADE"));
        assertTrue(mensagem.getValue().contains("Mínimo configurado: 10 UNIDADE"));
    }

    @Test
    void deveUsarMetragemParaBobinaESepararDepositos() {
        Material material = material(11L, "Cabo óptico", TipoControleEstoque.BOBINA,
                UnidadeMedida.BOBINA, "50");
        SaldoMaterialLocal critico = saldo(21L, "Norte", material, 0, 0, "60", "15");
        SaldoMaterialLocal adequado = saldo(22L, "Sul", material, 0, 0, "80", "10");
        when(saldoRepository.findAllByOrderByMaterialNomeAscLocalEstoqueNomeAsc())
                .thenReturn(List.of(critico, adequado));

        AgendadorAlertasService.VarreduraResultado resultado = service.executarVarreduraDiariaDeAlertas();

        assertEquals(1, resultado.alertasEncontrados());
        verify(notificacaoService).registrarSeAusente(
                eq("ESTOQUE_CRITICO:11:21"), eq("ESTOQUE_CRITICO"), eq("ALERTA"),
                eq("Estoque crítico: Cabo óptico em Norte"),
                eq("Depósito: Norte. Saldo livre: 45 m. Mínimo configurado: 50 m."), eq(null), eq(null));
    }

    @Test
    void deveIgnorarMinimoDesativadoEDepositoInativo() {
        Material semMinimo = material(12L, "Abraçadeira", TipoControleEstoque.UNIDADE,
                UnidadeMedida.UNIDADE, "0");
        Material material = material(13L, "Switch", TipoControleEstoque.UNIDADE,
                UnidadeMedida.UNIDADE, "5");
        SaldoMaterialLocal saldoSemMinimo = saldo(23L, "Central", semMinimo, 0, 0, "0", "0");
        SaldoMaterialLocal saldoInativo = saldo(24L, "Desativado", material, 0, 0, "0", "0");
        saldoInativo.getLocalEstoque().setAtivo(false);
        when(saldoRepository.findAllByOrderByMaterialNomeAscLocalEstoqueNomeAsc())
                .thenReturn(List.of(saldoSemMinimo, saldoInativo));

        AgendadorAlertasService.VarreduraResultado resultado = service.executarVarreduraDiariaDeAlertas();

        assertEquals(0, resultado.alertasEncontrados());
        verify(notificacaoService, never()).registrarSeAusente(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void minimoLocalDeveSubstituirMinimoGlobal() {
        Material material = material(14L, "Patch cord", TipoControleEstoque.UNIDADE,
                UnidadeMedida.UNIDADE, "10");
        SaldoMaterialLocal saldo = saldo(25L, "Central", material, 9, 0, "0", "0");
        saldo.setEstoqueMinimo(new BigDecimal("5"));
        when(saldoRepository.findAllByOrderByMaterialNomeAscLocalEstoqueNomeAsc())
                .thenReturn(List.of(saldo));

        AgendadorAlertasService.VarreduraResultado resultado = service.executarVarreduraDiariaDeAlertas();

        assertEquals(0, resultado.alertasEncontrados());
        verify(notificacaoService, never()).registrarSeAusente(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void deveUsarAntecedenciaConfiguradaParaDeadline() {
        when(configRepository.findById(1L)).thenReturn(Optional.of(
                new ConfiguracaoNotificacao(1L, "", "", true, false, false, 48, 30)));
        OrdemServico os = new OrdemServico();
        os.setId(30L);
        os.setNumeroOs("Contrato 01 - OS 03");
        os.setStatus(StatusOS.ABERTA);
        os.setDeadline(LocalDateTime.now().plusHours(36));
        when(osRepository.findAll()).thenReturn(List.of(os));
        when(notificacaoService.registrarSeAusente(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(true);

        AgendadorAlertasService.VarreduraResultado resultado = service.executarVarreduraDiariaDeAlertas();

        assertEquals(1, resultado.alertasEncontrados());
        verify(notificacaoService).registrarSeAusente(
                org.mockito.ArgumentMatchers.startsWith("OS_PRAZO_24H:30:"),
                eq("OS_PRAZO_24H"), eq("ALERTA"),
                eq("OS Contrato 01 - OS 03 próxima do prazo"),
                org.mockito.ArgumentMatchers.contains("menos de 48 horas"), eq(os), eq(null));
    }

    private Material material(Long id, String nome, TipoControleEstoque tipo, UnidadeMedida unidade, String minimo) {
        Material material = new Material();
        material.setId(id);
        material.setNome(nome);
        material.setTipoControle(tipo);
        material.setUnidadeMedida(unidade);
        material.setEstoqueMinimo(new BigDecimal(minimo));
        return material;
    }

    private SaldoMaterialLocal saldo(Long localId, String localNome, Material material,
            int quantidade, int reservada, String metragem, String metragemReservada) {
        LocalEstoque local = new LocalEstoque();
        local.setId(localId);
        local.setNome(localNome);
        local.setAtivo(true);
        SaldoMaterialLocal saldo = new SaldoMaterialLocal();
        saldo.setMaterial(material);
        saldo.setLocalEstoque(local);
        saldo.setQuantidadeDisponivel(quantidade);
        saldo.setQuantidadeReservada(reservada);
        saldo.setMetragemDisponivel(new BigDecimal(metragem));
        saldo.setMetragemReservada(new BigDecimal(metragemReservada));
        return saldo;
    }
}
