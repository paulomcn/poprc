package com.poprc.demo.integration;

import com.poprc.demo.dto.CriarOrdemServicoRequest;
import com.poprc.demo.dto.DevolverOrdemRetiradaRequest;
import com.poprc.demo.dto.ExecutarOrdemRetiradaRequest;
import com.poprc.demo.model.Comarca;
import com.poprc.demo.model.Contrato;
import com.poprc.demo.model.Funcionario;
import com.poprc.demo.model.Material;
import com.poprc.demo.model.Projeto;
import com.poprc.demo.model.ProjetoStatus;
import com.poprc.demo.model.TipoControleEstoque;
import com.poprc.demo.model.UnidadeMedida;
import com.poprc.demo.repository.ComarcaRepository;
import com.poprc.demo.repository.ContratoRepository;
import com.poprc.demo.repository.FuncionarioRepository;
import com.poprc.demo.repository.HistoricoStatusOSRepository;
import com.poprc.demo.repository.MaterialRepository;
import com.poprc.demo.repository.OrdemRetiradaRepository;
import com.poprc.demo.repository.ProjetoRepository;
import com.poprc.demo.service.ComarcaService;
import com.poprc.demo.service.EstoqueService;
import com.poprc.demo.service.OrdemRetiradaPdfService;
import com.poprc.demo.service.OrdemRetiradaService;
import com.poprc.demo.service.OrdemServicoService;
import com.poprc.demo.service.SaldoLocalService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class OrdemRetiradaConcorrenciaIntegrationTest {
    private static final String ASSINATURA = "data:image/png;base64,dGVzdGU=";
    @Autowired private OrdemServicoService osService;
    @Autowired private OrdemRetiradaService orService;
    @Autowired private ComarcaService comarcaService;
    @Autowired private EstoqueService estoque;
    @Autowired private SaldoLocalService saldos;
    @Autowired private FuncionarioRepository funcionarios;
    @Autowired private ContratoRepository contratos;
    @Autowired private ProjetoRepository projetos;
    @Autowired private ComarcaRepository comarcas;
    @Autowired private MaterialRepository materiais;
    @Autowired private OrdemRetiradaRepository retiradas;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbc;

    // Arquivos e historicos append-only reais sao cobertos no piloto HTTP com rollback.
    // Estas duas fronteiras sao substituidas para limpar somente as fixtures concorrentes apos seus commits.
    @MockitoBean private OrdemRetiradaPdfService pdfService;
    @MockitoBean private HistoricoStatusOSRepository historico;

    private Long materialId;
    private Long funcionarioId;
    private Long contratoId;
    private Long localId;
    private final List<Operacao> operacoes = new ArrayList<>();

    @BeforeEach
    void prepararDuasOrsComOMesmoMaterial() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Funcionario funcionario = new Funcionario();
            funcionario.setNome("Tecnico concorrencia");
            funcionario.setFuncao("Tecnico");
            funcionarioId = funcionarios.save(funcionario).getId();
            Contrato contrato = new Contrato();
            contrato.setContrato("CONCORRENCIA-" + UUID.randomUUID());
            contrato.setCliente("Teste de concorrencia");
            contrato.setVigenciaInicio(LocalDate.now());
            contrato.setVigenciaFim(LocalDate.now().plusYears(1));
            contratoId = contratos.save(contrato).getId();
            Material material = new Material();
            material.setNome("Material concorrencia OR");
            material.setPartNumber("OR-CONC-" + UUID.randomUUID());
            material.setCategoria("MATERIAL_CONSUMO");
            material.setTipoControle(TipoControleEstoque.UNIDADE);
            material.setUnidadeMedida(UnidadeMedida.UNIDADE);
            material.setQuantidadeDisponivel(20);
            material.setCustoMedio(new BigDecimal("2.50"));
            material.setLocalizacao("Deposito OR " + UUID.randomUUID());
            materialId = estoque.cadastrarMaterial(material).getId();
            localId = saldos.listarSaldos(materialId).getFirst().getLocalEstoque().getId();
            operacoes.add(criarOperacao());
            operacoes.add(criarOperacao());
        });
    }

    @RepeatedTest(3)
    void mesmaOrNaoPodeSerRetiradaDuasVezesSimultaneamente() throws Exception {
        Operacao primeira = operacoes.getFirst();
        List<String> resultados = executarSimultaneamente(() -> retirar(primeira), () -> retirar(primeira));
        assertThat(resultados).containsExactlyInAnyOrder("OK", "Esta OR não está disponível para retirada.");
        conferirSaldo(16, 4);
        conferirMovimentos(1, 0);
        assertThat(retiradas.findById(primeira.orId()).orElseThrow().getStatus()).isEqualTo("RETIRADA");
        verify(historico, times(1)).save(argThat(evento -> "MATERIAIS_RETIRADOS".equals(evento.getEvento())
                && primeira.osId().equals(evento.getOrdemServico().getId())));
    }

    @RepeatedTest(3)
    void mesmaOrNaoPodeSerDevolvidaDuasVezesSimultaneamente() throws Exception {
        Operacao primeira = operacoes.getFirst();
        retirar(primeira);
        List<String> resultados = executarSimultaneamente(() -> devolver(primeira), () -> devolver(primeira));
        assertThat(resultados).containsExactlyInAnyOrder("OK", "A OR precisa estar retirada para registrar devolução.");
        conferirSaldo(18, 4);
        conferirMovimentos(1, 1);
        assertThat(retiradas.findById(primeira.orId()).orElseThrow().getStatus()).isEqualTo("DEVOLVIDA");
        assertThat(jdbc.queryForObject("select quantidade_devolvida from ordem_retirada_itens where id = ?",
                BigDecimal.class, primeira.itemId())).isEqualByComparingTo("2");
    }

    @RepeatedTest(3)
    void duasOrsDiferentesNaoPerdemDebitosOuCreditosDoMaterialCompartilhado() throws Exception {
        Operacao primeira = operacoes.getFirst();
        Operacao segunda = operacoes.getLast();
        assertThat(executarSimultaneamente(() -> retirar(primeira), () -> retirar(segunda))).containsOnly("OK");
        conferirSaldo(12, 0);
        conferirMovimentos(2, 0);
        assertThat(executarSimultaneamente(() -> devolver(primeira), () -> devolver(segunda))).containsOnly("OK");
        conferirSaldo(16, 0);
        conferirMovimentos(2, 2);
    }

    @RepeatedTest(3)
    void retiradaEDevolucaoSimultaneasMantemSaldoEReservasConsistentes() throws Exception {
        Operacao primeira = operacoes.getFirst();
        Operacao segunda = operacoes.getLast();
        retirar(primeira);
        assertThat(executarSimultaneamente(() -> devolver(primeira), () -> retirar(segunda))).containsOnly("OK");
        conferirSaldo(14, 0);
        conferirMovimentos(2, 1);
    }

    private Operacao criarOperacao() {
        Projeto projeto = new Projeto();
        projeto.setContrato(contratos.findById(contratoId).orElseThrow());
        projeto.setResponsavel(funcionarios.findById(funcionarioId).orElseThrow());
        projeto.setDataInicio(LocalDate.now());
        projeto.setStatus(ProjetoStatus.EM_ANDAMENTO);
        projeto = projetos.save(projeto);
        Comarca comarca = new Comarca();
        comarca.setNomeComarca("Obra concorrencia " + projeto.getId());
        comarca.setProjeto(projeto);
        comarca.setFotoVistoriaUrl("/uploads/teste/concorrencia.png");
        comarca.setAssinaturaBase64(ASSINATURA);
        comarca = comarcas.save(comarca);
        CriarOrdemServicoRequest request = new CriarOrdemServicoRequest();
        request.setContratoId(contratoId);
        request.setProjetoId(projeto.getId());
        request.setDescricao("Teste concorrente de OR");
        request.setDataHoraInicio(LocalDateTime.now());
        request.setDataHoraFim(LocalDateTime.now().plusHours(4));
        request.setDeadline(LocalDateTime.now().plusDays(1));
        CriarOrdemServicoRequest.MaterialPrevistoRequest item = new CriarOrdemServicoRequest.MaterialPrevistoRequest();
        item.setMaterialId(materialId);
        item.setQuantidadePrevista(BigDecimal.valueOf(4));
        request.setMateriais(List.of(item));
        var os = osService.criar(request);
        comarcaService.avancarParaInfraestrutura(comarca.getId());
        var or = retiradas.findByOrdemServicoIdOrderByDataGeracaoDesc(os.getId()).getFirst();
        return new Operacao(projeto.getId(), comarca.getId(), os.getId(), or.getId(), or.getItens().getFirst().getId());
    }

    private void retirar(Operacao operacao) {
        ExecutarOrdemRetiradaRequest request = new ExecutarOrdemRetiradaRequest();
        request.setConferidoPor("Estoque concorrencia");
        request.setLevadoPor("Tecnico concorrencia");
        request.setAssinaturaConferenteBase64(ASSINATURA);
        request.setAssinaturaRetiranteBase64(ASSINATURA);
        orService.executarRetirada(operacao.orId(), request);
    }

    private void devolver(Operacao operacao) {
        DevolverOrdemRetiradaRequest request = new DevolverOrdemRetiradaRequest();
        request.setDevolvidoPor("Tecnico concorrencia");
        request.setRecebidoPor("Estoque concorrencia");
        request.setAssinaturaRecebimentoBase64(ASSINATURA);
        DevolverOrdemRetiradaRequest.ItemDevolucaoRequest item = new DevolverOrdemRetiradaRequest.ItemDevolucaoRequest();
        item.setItemId(operacao.itemId());
        item.setQuantidadeDevolvida(BigDecimal.valueOf(2));
        request.setItens(List.of(item));
        orService.devolver(operacao.orId(), request);
    }

    private List<String> executarSimultaneamente(Runnable primeira, Runnable segunda) throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch inicio = new CountDownLatch(1);
        try {
            var a = executor.submit(() -> tentar(primeira, prontas, inicio));
            var b = executor.submit(() -> tentar(segunda, prontas, inicio));
            assertThat(prontas.await(5, TimeUnit.SECONDS)).isTrue();
            inicio.countDown();
            return List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
        } finally {
            inicio.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(20, TimeUnit.SECONDS)).isTrue();
        }
    }

    private String tentar(Runnable operacao, CountDownLatch prontas, CountDownLatch inicio) throws Exception {
        prontas.countDown();
        assertThat(inicio.await(5, TimeUnit.SECONDS)).isTrue();
        try {
            operacao.run();
            return "OK";
        } catch (IllegalArgumentException ex) {
            return ex.getMessage();
        }
    }

    private void conferirSaldo(int quantidade, int reserva) {
        Material material = materiais.findById(materialId).orElseThrow();
        assertThat(material.getQuantidadeDisponivel()).isEqualTo(quantidade);
        assertThat(material.getQuantidadeReservada()).isEqualTo(reserva);
        assertThat(saldos.listarSaldos(materialId).stream().mapToInt(s -> s.getQuantidadeDisponivel()).sum()).isEqualTo(quantidade);
        assertThat(saldos.listarSaldos(materialId).stream().mapToInt(s -> s.getQuantidadeReservada()).sum()).isEqualTo(reserva);
    }

    private void conferirMovimentos(int saidas, int retornos) {
        assertThat(jdbc.queryForObject("select count(*) from movimentacoes_estoque where material_id = ? "
                + "and tipo = 'RETIRADA_OR'", Integer.class, materialId)).isEqualTo(saidas);
        assertThat(jdbc.queryForObject("select count(*) from movimentacoes_estoque where material_id = ? "
                + "and tipo = 'DEVOLUCAO_OR'", Integer.class, materialId)).isEqualTo(retornos);
    }

    @AfterEach
    void removerSomenteOsRegistrosDoCenario() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.update("delete from movimentacoes_estoque where material_id = ?", materialId);
            for (Operacao op : operacoes) {
                jdbc.update("delete from ordem_retirada_itens where ordem_retirada_id = ?", op.orId());
                jdbc.update("delete from ordens_retirada where id = ?", op.orId());
                jdbc.update("delete from comarca_materiais where comarca_id = ?", op.comarcaId());
                jdbc.update("update comarcas set ordem_servico_id = null where id = ?", op.comarcaId());
                jdbc.update("delete from ordens_servico where id = ?", op.osId());
                jdbc.update("delete from materiais_projeto where projeto_id = ?", op.projetoId());
                jdbc.update("delete from comarcas where id = ?", op.comarcaId());
                jdbc.update("delete from projetos where id = ?", op.projetoId());
            }
            jdbc.update("delete from contratos where id = ?", contratoId);
            jdbc.update("delete from saldos_materiais_locais where material_id = ?", materialId);
            jdbc.update("delete from materiais where id = ?", materialId);
            jdbc.update("delete from locais_estoque where id = ?", localId);
            jdbc.update("delete from funcionarios where id = ?", funcionarioId);
        });
    }

    private record Operacao(Long projetoId, Long comarcaId, Long osId, Long orId, Long itemId) { }
}
