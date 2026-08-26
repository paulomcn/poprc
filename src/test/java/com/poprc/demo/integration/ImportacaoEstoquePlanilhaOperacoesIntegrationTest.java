package com.poprc.demo.integration;

import com.poprc.demo.dto.ImportacaoEstoquePlanilhaRequest;
import com.poprc.demo.dto.ImportacaoEstoquePlanilhaResultadoDTO;
import com.poprc.demo.model.Contrato;
import com.poprc.demo.model.Funcionario;
import com.poprc.demo.model.LocalEstoque;
import com.poprc.demo.model.Material;
import com.poprc.demo.model.TipoControleEstoque;
import com.poprc.demo.model.UnidadeMedida;
import com.poprc.demo.repository.ContratoRepository;
import com.poprc.demo.repository.FuncionarioRepository;
import com.poprc.demo.repository.LocalEstoqueRepository;
import com.poprc.demo.repository.MaterialRepository;
import com.poprc.demo.repository.OrdemRetiradaRepository;
import com.poprc.demo.repository.OrdemServicoRepository;
import com.poprc.demo.repository.ProjetoRepository;
import com.poprc.demo.service.ImportacaoEstoquePlanilhaService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ImportacaoEstoquePlanilhaOperacoesIntegrationTest {

    @Autowired
    private ImportacaoEstoquePlanilhaService importacaoService;
    @Autowired
    private ContratoRepository contratoRepository;
    @Autowired
    private FuncionarioRepository funcionarioRepository;
    @Autowired
    private LocalEstoqueRepository localRepository;
    @Autowired
    private MaterialRepository materialRepository;
    @Autowired
    private ProjetoRepository projetoRepository;
    @Autowired
    private OrdemServicoRepository ordemServicoRepository;
    @Autowired
    private OrdemRetiradaRepository ordemRetiradaRepository;

    @Test
    void criaOsEOrHistoricasSemDebitarNovamenteOSaldoConsolidado() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);

        Funcionario responsavel = new Funcionario();
        responsavel.setNome("Responsável " + sufixo);
        responsavel.setFuncao("Supervisor Técnico");
        responsavel.setCidade("Cuité");
        responsavel.setAtivo(true);
        responsavel = funcionarioRepository.saveAndFlush(responsavel);

        Contrato contrato = new Contrato();
        contrato.setContrato("TESTE-" + sufixo);
        contrato.setCliente("Cliente de teste");
        contrato.setArquivado(false);
        contrato = contratoRepository.saveAndFlush(contrato);

        LocalEstoque local = new LocalEstoque();
        local.setNome("Depósito " + sufixo);
        local.setAtivo(true);
        local = localRepository.saveAndFlush(local);

        Material material = new Material();
        material.setNome("Patch Cord " + sufixo);
        material.setPartNumber("PC-" + sufixo);
        material.setCategoria("MATERIAL_CONSUMO");
        material.setTipoControle(TipoControleEstoque.UNIDADE);
        material.setUnidadeMedida(UnidadeMedida.UNIDADE);
        material.setQuantidadeDisponivel(0);
        material.setQuantidadeReservada(0);
        material.setMetragemDisponivel(BigDecimal.ZERO);
        material.setMetragemReservada(BigDecimal.ZERO);
        material.setEstoqueMinimo(BigDecimal.TEN);
        material.setCustoMedio(new BigDecimal("12.50"));
        material = materialRepository.saveAndFlush(material);

        ImportacaoEstoquePlanilhaRequest request = new ImportacaoEstoquePlanilhaRequest(
                "controle-estoque.xlsx",
                UUID.randomUUID().toString().replace("-", "")
                        + UUID.randomUUID().toString().replace("-", ""),
                local.getId(),
                true,
                true,
                contrato.getId(),
                responsavel.getId(),
                BigDecimal.TEN,
                List.of(new ImportacaoEstoquePlanilhaRequest.ItemImportacao(
                        material.getNome(), null, new BigDecimal("8"), new BigDecimal("12.50"), 5)),
                List.of(new ImportacaoEstoquePlanilhaRequest.EntradaImportacao(
                        "ESTOQUE_INICIAL",
                        "ESTOQUE INICIAL",
                        null,
                        null,
                        material.getNome(),
                        BigDecimal.TEN,
                        new BigDecimal("12.50"),
                        5,
                        2)),
                List.of(new ImportacaoEstoquePlanilhaRequest.RetiradaImportacao(
                        "ORDEM DE RETIRADA - CUITÉ",
                        null,
                        "Cuité",
                        material.getNome(),
                        BigDecimal.TEN,
                        new BigDecimal("2"),
                        new BigDecimal("8"),
                        new BigDecimal("12.50"),
                        LocalDate.of(2026, 8, 20),
                        5)),
                List.of(),
                List.of(),
                List.of());

        ImportacaoEstoquePlanilhaResultadoDTO resultado =
                importacaoService.importar(request, "JUnit");

        assertEquals(1, resultado.entradasImportadas());
        assertEquals(1, resultado.projetosCriados());
        assertEquals(1, resultado.ordensServicoCriadas());
        assertEquals(1, resultado.ordensRetiradaCriadas());
        assertEquals(1, resultado.retiradasImportadas());
        assertEquals(8, materialRepository.findById(material.getId()).orElseThrow()
                .getQuantidadeDisponivel());
        assertEquals(1, projetoRepository.findByContratoId(contrato.getId()).size());
        var ordensServico = ordemServicoRepository.findByContratoId(contrato.getId());
        assertEquals(1, ordensServico.size());
        assertTrue(ordensServico.getFirst().getTitulo().contains("Cuité"));
        assertEquals(1, ordemRetiradaRepository
                .findByOrdemServicoIdOrderByDataGeracaoDesc(ordensServico.getFirst().getId()).size());
    }
}
