package com.poprc.demo.integration;

import com.poprc.demo.dto.ImportacaoEstoquePlanilhaRequest;
import com.poprc.demo.dto.AtualizacaoCustosPlanilhaRequest;
import com.poprc.demo.dto.ImportacaoEstoquePlanilhaResultadoDTO;
import com.poprc.demo.dto.SincronizacaoSaldosPlanilhaRequest;
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
import com.poprc.demo.repository.MaterialItemRepository;
import com.poprc.demo.repository.OrdemRetiradaItemRepository;
import com.poprc.demo.repository.OrdemRetiradaRepository;
import com.poprc.demo.repository.OrdemServicoRepository;
import com.poprc.demo.repository.ProjetoRepository;
import com.poprc.demo.service.ImportacaoEstoquePlanilhaService;
import com.poprc.demo.service.SaldoLocalService;
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
    @Autowired
    private OrdemRetiradaItemRepository ordemRetiradaItemRepository;
    @Autowired
    private MaterialItemRepository materialItemRepository;
    @Autowired
    private SaldoLocalService saldoLocalService;

    @Test
    void sincronizaSaldoComAjusteAuditavelSemCriarOperacoesOuAlterarCusto() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        LocalEstoque local = new LocalEstoque();
        local.setNome("Depósito saldo " + sufixo);
        local.setAtivo(true);
        local = localRepository.saveAndFlush(local);

        Material material = new Material();
        material.setNome("Material saldo " + sufixo);
        material.setPartNumber("SALDO-" + sufixo);
        material.setCategoria("MATERIAL_CONSUMO");
        material.setTipoControle(TipoControleEstoque.UNIDADE);
        material.setUnidadeMedida(UnidadeMedida.UNIDADE);
        material.setQuantidadeDisponivel(10);
        material.setQuantidadeReservada(2);
        material.setMetragemDisponivel(BigDecimal.ZERO);
        material.setMetragemReservada(BigDecimal.ZERO);
        material.setEstoqueMinimo(BigDecimal.TEN);
        material.setCustoMedio(new BigDecimal("5.0000"));
        material = materialRepository.saveAndFlush(material);
        saldoLocalService.creditar(material, local.getId(), BigDecimal.TEN);

        long projetosAntes = projetoRepository.count();
        long osAntes = ordemServicoRepository.count();
        long orAntes = ordemRetiradaRepository.count();
        SincronizacaoSaldosPlanilhaRequest request = new SincronizacaoSaldosPlanilhaRequest(
                "saldos.xlsx",
                UUID.randomUUID().toString().replace("-", "")
                        + UUID.randomUUID().toString().replace("-", ""),
                local.getId(),
                List.of(new SincronizacaoSaldosPlanilhaRequest.ItemSaldo(
                        material.getId(), material.getNome(), new BigDecimal("12"), 5)));

        ImportacaoEstoquePlanilhaResultadoDTO resultado =
                importacaoService.sincronizarSaldos(request, "JUnit");
        materialRepository.flush();
        Material atualizado = materialRepository.findById(material.getId()).orElseThrow();

        assertEquals(12, atualizado.getQuantidadeDisponivel());
        assertEquals(2, atualizado.getQuantidadeReservada());
        assertEquals(0, new BigDecimal("5.0000").compareTo(atualizado.getCustoMedio()));
        assertEquals(projetosAntes, projetoRepository.count());
        assertEquals(osAntes, ordemServicoRepository.count());
        assertEquals(orAntes, ordemRetiradaRepository.count());
        assertEquals("SALDOS_SINCRONIZADOS", resultado.resultado());
        assertEquals("SINCRONIZACAO_SALDOS",
                importacaoService.detalhar(resultado.importacaoId()).tipoImportacao());
        assertEquals("SALDO_AUMENTADO",
                importacaoService.detalhar(resultado.importacaoId()).itens().getFirst().acao());
    }

    @Test
    void atualizaCustoEmImportacaoDedicadaSemAlterarSaldo() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        Material material = new Material();
        material.setNome("Material custo " + sufixo);
        material.setPartNumber("CUSTO-" + sufixo);
        material.setCategoria("MATERIAL_CONSUMO");
        material.setTipoControle(TipoControleEstoque.UNIDADE);
        material.setUnidadeMedida(UnidadeMedida.UNIDADE);
        material.setQuantidadeDisponivel(23);
        material.setQuantidadeReservada(4);
        material.setMetragemDisponivel(BigDecimal.ZERO);
        material.setMetragemReservada(BigDecimal.ZERO);
        material.setEstoqueMinimo(BigDecimal.TEN);
        material.setCustoMedio(new BigDecimal("12.5000"));
        material = materialRepository.saveAndFlush(material);

        AtualizacaoCustosPlanilhaRequest request = new AtualizacaoCustosPlanilhaRequest(
                "custos.xlsx",
                UUID.randomUUID().toString().replace("-", "")
                        + UUID.randomUUID().toString().replace("-", ""),
                List.of(new AtualizacaoCustosPlanilhaRequest.ItemCusto(
                        material.getId(), material.getNome(), new BigDecimal("19.9000"), 2)));

        ImportacaoEstoquePlanilhaResultadoDTO resultado =
                importacaoService.atualizarCustos(request, "JUnit");
        materialRepository.flush();
        Material atualizado = materialRepository.findById(material.getId()).orElseThrow();

        assertEquals(23, atualizado.getQuantidadeDisponivel());
        assertEquals(4, atualizado.getQuantidadeReservada());
        assertEquals(0, new BigDecimal("19.9000").compareTo(atualizado.getCustoMedio()));
        assertEquals(1, resultado.itensProcessados());
        assertEquals(1, resultado.materiaisAtualizados());
        assertEquals(0, resultado.ajustesPositivos());
        assertEquals(0, resultado.ajustesNegativos());
        assertEquals("CUSTOS_ATUALIZADOS", resultado.resultado());
    }

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
        var ordensRetirada = ordemRetiradaRepository
                .findByOrdemServicoIdOrderByDataGeracaoDesc(ordensServico.getFirst().getId());
        assertEquals(1, ordensRetirada.size());
        Long materialId = material.getId();
        var retiradaImportada = importacaoService.listarRetiradasImportadas().stream()
                .filter(retirada -> retirada.materialId().equals(materialId))
                .findFirst()
                .orElseThrow();
        assertEquals(ordensRetirada.getFirst().getId(), retiradaImportada.ordemRetiradaId());
        assertEquals(ordensRetirada.getFirst().getNumeroOr(), retiradaImportada.numeroOr());
    }

    @Test
    void cadastraMaterialFracionadoQuandoSaldoDaPlanilhaPossuiDecimais() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);

        LocalEstoque local = new LocalEstoque();
        local.setNome("Depósito fracionado " + sufixo);
        local.setAtivo(true);
        local = localRepository.saveAndFlush(local);

        String nomeMaterial = "Caixa de cabo " + sufixo;
        ImportacaoEstoquePlanilhaRequest request = new ImportacaoEstoquePlanilhaRequest(
                "controle-estoque-fracionado.xlsx",
                UUID.randomUUID().toString().replace("-", "")
                        + UUID.randomUUID().toString().replace("-", ""),
                local.getId(),
                true,
                false,
                null,
                null,
                BigDecimal.TEN,
                List.of(new ImportacaoEstoquePlanilhaRequest.ItemImportacao(
                        nomeMaterial, null, new BigDecimal("2.94"), new BigDecimal("2196.00"), 41)),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());

        ImportacaoEstoquePlanilhaResultadoDTO resultado = importacaoService.importar(request, "JUnit");

        assertEquals(1, resultado.materiaisCriados());
        Material material = materialRepository.findAll().stream()
                .filter(atual -> nomeMaterial.equals(atual.getNome()))
                .findFirst()
                .orElseThrow();
        assertEquals(TipoControleEstoque.FRACIONADO, material.getTipoControle());
        assertEquals(0, new BigDecimal("2.94").compareTo(material.getMetragemDisponivel()));
    }

    @Test
    void registraFaltaSemCriarSaldoNegativoNaOrAvulsa() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);

        Funcionario responsavel = new Funcionario();
        responsavel.setNome("Responsável " + sufixo);
        responsavel.setFuncao("Supervisor Técnico");
        responsavel.setCidade("Cabedelo");
        responsavel.setAtivo(true);
        responsavel = funcionarioRepository.saveAndFlush(responsavel);

        Contrato contrato = new Contrato();
        contrato.setContrato("OR-" + sufixo);
        contrato.setCliente("Cliente OR avulsa");
        contrato.setArquivado(false);
        contrato = contratoRepository.saveAndFlush(contrato);

        LocalEstoque local = new LocalEstoque();
        local.setNome("Depósito OR " + sufixo);
        local.setAtivo(true);
        local = localRepository.saveAndFlush(local);

        Material material = new Material();
        material.setNome("Porca gaiola " + sufixo);
        material.setPartNumber("PG-" + sufixo);
        material.setCategoria("MATERIAL_CONSUMO");
        material.setTipoControle(TipoControleEstoque.UNIDADE);
        material.setUnidadeMedida(UnidadeMedida.UNIDADE);
        material.setQuantidadeDisponivel(17);
        material.setQuantidadeReservada(0);
        material.setMetragemDisponivel(BigDecimal.ZERO);
        material.setMetragemReservada(BigDecimal.ZERO);
        material.setEstoqueMinimo(BigDecimal.TEN);
        material.setCustoMedio(new BigDecimal("2.00"));
        material = materialRepository.saveAndFlush(material);
        saldoLocalService.creditar(material, local.getId(), new BigDecimal("17"));

        ImportacaoEstoquePlanilhaRequest request = new ImportacaoEstoquePlanilhaRequest(
                "OR-CABEDELO.xlsx",
                UUID.randomUUID().toString().replace("-", "")
                        + UUID.randomUUID().toString().replace("-", ""),
                local.getId(),
                false,
                true,
                contrato.getId(),
                responsavel.getId(),
                null,
                List.of(new ImportacaoEstoquePlanilhaRequest.ItemImportacao(
                        material.getNome(), null, new BigDecimal("17"), new BigDecimal("2.00"), 8)),
                List.of(),
                List.of(new ImportacaoEstoquePlanilhaRequest.RetiradaImportacao(
                        "ORDEM DE RETIRADA - CABEDELO",
                        null,
                        "Cabedelo",
                        material.getNome(),
                        new BigDecimal("17"),
                        new BigDecimal("32"),
                        new BigDecimal("-15"),
                        new BigDecimal("2.00"),
                        LocalDate.of(2026, 8, 28),
                        8)),
                List.of(),
                List.of(),
                List.of());

        ImportacaoEstoquePlanilhaResultadoDTO resultado = importacaoService.importar(request, "JUnit");

        assertEquals(1, resultado.faltasIdentificadas());
        assertEquals(0, materialRepository.findById(material.getId()).orElseThrow()
                .getQuantidadeDisponivel());
        String numeroContrato = contrato.getContrato();
        var ordemRetirada = ordemRetiradaRepository.findAll().stream()
                .filter(ordem -> ordem.getNumeroOr().startsWith(numeroContrato))
                .findFirst()
                .orElseThrow();
        var itemOr = ordemRetiradaItemRepository.findAll().stream()
                .filter(item -> item.getOrdemRetirada().getId().equals(ordemRetirada.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(0, new BigDecimal("32").compareTo(itemOr.getQuantidadeSolicitada()));
        assertEquals(0, new BigDecimal("17").compareTo(itemOr.getQuantidadeRetirada()));
        var materialItem = materialItemRepository
                .findByComarcaIdOrderByIdAsc(ordemRetirada.getComarca().getId())
                .getFirst();
        assertEquals(0, new BigDecimal("32").compareTo(materialItem.getQuantidadePrevista()));
        assertEquals(0, new BigDecimal("17").compareTo(materialItem.getQuantidadeAuditada()));
    }

    @Test
    void converteCaixaDeCaboLegadaEmMetragemAoImportarOrAvulsa() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);

        Funcionario responsavel = new Funcionario();
        responsavel.setNome("Responsável cabo " + sufixo);
        responsavel.setFuncao("Supervisor Técnico");
        responsavel.setCidade("Cabedelo");
        responsavel.setAtivo(true);
        responsavel = funcionarioRepository.saveAndFlush(responsavel);

        Contrato contrato = new Contrato();
        contrato.setContrato("CABO-" + sufixo);
        contrato.setCliente("Cliente cabo");
        contrato.setArquivado(false);
        contrato = contratoRepository.saveAndFlush(contrato);

        LocalEstoque local = new LocalEstoque();
        local.setNome("Depósito cabo " + sufixo);
        local.setAtivo(true);
        local = localRepository.saveAndFlush(local);

        Material material = new Material();
        material.setNome("CAIXA DE CABO CAT6A " + sufixo);
        material.setPartNumber("CX-CABO-" + sufixo);
        material.setCategoria("MATERIAL_CONSUMO");
        material.setTipoControle(TipoControleEstoque.FRACIONADO);
        material.setUnidadeMedida(UnidadeMedida.UNIDADE);
        material.setQuantidadeDisponivel(0);
        material.setQuantidadeReservada(0);
        material.setMetragemDisponivel(new BigDecimal("2.94"));
        material.setMetragemReservada(BigDecimal.ZERO);
        material.setEstoqueMinimo(BigDecimal.TEN);
        material.setCustoMedio(new BigDecimal("2196.00"));
        material = materialRepository.saveAndFlush(material);
        saldoLocalService.creditar(material, local.getId(), new BigDecimal("2.94"));

        ImportacaoEstoquePlanilhaRequest request = new ImportacaoEstoquePlanilhaRequest(
                "OR-CABEDELO-CABO.xlsx",
                UUID.randomUUID().toString().replace("-", "")
                        + UUID.randomUUID().toString().replace("-", ""),
                local.getId(),
                false,
                true,
                contrato.getId(),
                responsavel.getId(),
                null,
                List.of(new ImportacaoEstoquePlanilhaRequest.ItemImportacao(
                        material.getNome(), null, new BigDecimal("896.700"),
                        new BigDecimal("7.2000"), 44)),
                List.of(),
                List.of(new ImportacaoEstoquePlanilhaRequest.RetiradaImportacao(
                        "ORDEM DE RETIRADA - CABEDELO",
                        null,
                        "Cabedelo",
                        material.getNome(),
                        new BigDecimal("896.700"),
                        new BigDecimal("915.000"),
                        new BigDecimal("-18.300"),
                        new BigDecimal("7.2000"),
                        LocalDate.of(2026, 8, 28),
                        44)),
                List.of(),
                List.of(),
                List.of());

        ImportacaoEstoquePlanilhaResultadoDTO resultado = importacaoService.importar(request, "JUnit");

        assertEquals(1, resultado.faltasIdentificadas());
        Material convertido = materialRepository.findById(material.getId()).orElseThrow();
        assertEquals(TipoControleEstoque.METRAGEM, convertido.getTipoControle());
        assertEquals(UnidadeMedida.METRO, convertido.getUnidadeMedida());
        assertEquals(0, BigDecimal.ZERO.compareTo(convertido.getMetragemDisponivel()));
        assertEquals(0, new BigDecimal("7.2000").compareTo(convertido.getCustoMedio()));
        assertEquals(0, new BigDecimal("3050").compareTo(convertido.getEstoqueMinimo()));

        String numeroContrato = contrato.getContrato();
        var ordemRetirada = ordemRetiradaRepository.findAll().stream()
                .filter(ordem -> ordem.getNumeroOr().startsWith(numeroContrato))
                .findFirst()
                .orElseThrow();
        var itemOr = ordemRetiradaItemRepository.findAll().stream()
                .filter(item -> item.getOrdemRetirada().getId().equals(ordemRetirada.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(0, new BigDecimal("915.000").compareTo(itemOr.getQuantidadeSolicitada()));
        assertEquals(0, new BigDecimal("896.700").compareTo(itemOr.getQuantidadeRetirada()));
    }
}
