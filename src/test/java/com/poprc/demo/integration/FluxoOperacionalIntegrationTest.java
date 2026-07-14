package com.poprc.demo.integration;

import com.poprc.demo.dto.CriarOrdemServicoRequest;
import com.poprc.demo.dto.DevolverOrdemRetiradaRequest;
import com.poprc.demo.dto.ExecutarOrdemRetiradaRequest;
import com.poprc.demo.model.Comarca;
import com.poprc.demo.model.Contrato;
import com.poprc.demo.model.Funcionario;
import com.poprc.demo.model.Material;
import com.poprc.demo.model.MaterialItem;
import com.poprc.demo.model.MovimentacaoEstoque;
import com.poprc.demo.model.OrdemRetirada;
import com.poprc.demo.model.OrdemRetiradaItem;
import com.poprc.demo.model.OrdemServico;
import com.poprc.demo.model.Projeto;
import com.poprc.demo.model.ProjetoStatus;
import com.poprc.demo.model.TipoControleEstoque;
import com.poprc.demo.model.TipoMovimentacao;
import com.poprc.demo.model.UnidadeMedida;
import com.poprc.demo.repository.ComarcaRepository;
import com.poprc.demo.repository.ContratoRepository;
import com.poprc.demo.repository.FuncionarioRepository;
import com.poprc.demo.repository.MaterialRepository;
import com.poprc.demo.repository.MovimentacaoEstoqueRepository;
import com.poprc.demo.repository.OrdemRetiradaRepository;
import com.poprc.demo.repository.ProjetoRepository;
import com.poprc.demo.service.ComarcaService;
import com.poprc.demo.service.EstoqueService;
import com.poprc.demo.service.OrdemRetiradaService;
import com.poprc.demo.service.OrdemServicoService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class FluxoOperacionalIntegrationTest {

    private static final String ASSINATURA = "data:image/png;base64,dGVzdGU=";

    @Autowired
    private OrdemServicoService ordemServicoService;
    @Autowired
    private OrdemRetiradaService ordemRetiradaService;
    @Autowired
    private ComarcaService comarcaService;
    @Autowired
    private EstoqueService estoqueService;
    @Autowired
    private FuncionarioRepository funcionarioRepository;
    @Autowired
    private ContratoRepository contratoRepository;
    @Autowired
    private ProjetoRepository projetoRepository;
    @Autowired
    private ComarcaRepository comarcaRepository;
    @Autowired
    private MaterialRepository materialRepository;
    @Autowired
    private OrdemRetiradaRepository ordemRetiradaRepository;
    @Autowired
    private MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void executaCicloCompletoComRastreabilidadeEHomologacaoDivergente() {
        Cenario cenario = prepararCenario(true);
        OrdemServico os = criarOrdemServico(cenario, BigDecimal.TEN, BigDecimal.ONE);

        assertEquals(cenario.numeroContrato() + " - OS 01", os.getNumeroOs());
        OrdemRetirada or = unicaOrDaOs(os);
        assertEquals(os.getNumeroOs() + " - OR 01", or.getNumeroOr());
        assertEquals("GERADA", or.getStatus());

        Material consumoReservado = materialRepository.findById(cenario.consumoId()).orElseThrow();
        Material ferramentaReservada = materialRepository.findById(cenario.ferramentaId()).orElseThrow();
        assertEquals(10, consumoReservado.getQuantidadeReservada());
        assertEquals(1, ferramentaReservada.getQuantidadeReservada());

        or = ordemRetiradaService.executarRetirada(or.getId(), retiradaAssinada());

        assertEquals("RETIRADA", or.getStatus());
        assertNotNull(or.getDataRetirada());
        assertEquals("Conferente Teste", or.getConferidoPor());
        assertEquals("Técnico Teste", or.getLevadoPor());
        assertEquals(90, materialRepository.findById(cenario.consumoId()).orElseThrow().getQuantidadeDisponivel());
        assertEquals(4, materialRepository.findById(cenario.ferramentaId()).orElseThrow().getQuantidadeDisponivel());

        List<MovimentacaoEstoque> aposRetirada = movimentacaoEstoqueRepository
                .findByComarcaIdOrderByDataMovimentacaoDesc(cenario.comarcaId());
        assertEquals(2, contarMovimentos(aposRetirada, TipoMovimentacao.RETIRADA_OR));
        assertTrue(aposRetirada.stream()
                .filter(movimento -> TipoMovimentacao.RETIRADA_OR.equals(movimento.getTipo()))
                .allMatch(movimento -> "Conferente Teste".equals(movimento.getAutorizadoPor())
                        && "Técnico Teste".equals(movimento.getRetiradoPor())
                        && movimento.getOrdemServico() != null
                        && movimento.getOrdemRetirada() != null));

        or = ordemRetiradaService.devolver(or.getId(), devolucao(or, BigDecimal.valueOf(3), BigDecimal.ONE));

        assertEquals("DEVOLVIDA", or.getStatus());
        assertNotNull(or.getDataDevolucao());
        assertEquals(93, materialRepository.findById(cenario.consumoId()).orElseThrow().getQuantidadeDisponivel());
        assertEquals(5, materialRepository.findById(cenario.ferramentaId()).orElseThrow().getQuantidadeDisponivel());

        List<MovimentacaoEstoque> aposDevolucao = movimentacaoEstoqueRepository
                .findByComarcaIdOrderByDataMovimentacaoDesc(cenario.comarcaId());
        assertEquals(2, contarMovimentos(aposDevolucao, TipoMovimentacao.DEVOLUCAO_OR));
        assertTrue(aposDevolucao.stream()
                .filter(movimento -> TipoMovimentacao.DEVOLUCAO_OR.equals(movimento.getTipo()))
                .allMatch(movimento -> "Almoxarife Teste".equals(movimento.getAutorizadoPor())
                        && "Técnico Teste".equals(movimento.getRetiradoPor())));

        MaterialItem consumo = itemDaOr(or, cenario.consumoId());
        MaterialItem ferramenta = itemDaOr(or, cenario.ferramentaId());
        comarcaService.atualizarQuantidadeAuditada(consumo.getId(), BigDecimal.valueOf(7));
        comarcaService.atualizarQuantidadeAuditada(ferramenta.getId(), BigDecimal.ONE);
        Map<String, Object> auditoria = comarcaService.homologarAsBuilt(cenario.comarcaId());

        assertEquals("HOMOLOGADO_COM_DIVERGENCIA", auditoria.get("asBuiltStatus"));
        assertEquals("HOMOLOGADO_COM_DIVERGENCIA",
                comarcaRepository.findById(cenario.comarcaId()).orElseThrow().getAsBuiltStatus());
    }

    @Test
    void bloqueiaRetiradaSemAsDuasAssinaturasSemBaixarEstoque() {
        Cenario cenario = prepararCenario(false);
        OrdemServico os = criarOrdemServico(cenario, BigDecimal.valueOf(4), null);
        OrdemRetirada or = unicaOrDaOs(os);

        ExecutarOrdemRetiradaRequest request = retiradaAssinada();
        request.setAssinaturaRetiranteBase64(null);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> ordemRetiradaService.executarRetirada(or.getId(), request));

        assertTrue(erro.getMessage().contains("Assinatura de quem levou"));
        assertEquals("GERADA", ordemRetiradaRepository.findById(or.getId()).orElseThrow().getStatus());
        Material material = materialRepository.findById(cenario.consumoId()).orElseThrow();
        assertEquals(100, material.getQuantidadeDisponivel());
        assertEquals(4, material.getQuantidadeReservada());
        assertFalse(movimentacaoEstoqueRepository.findByComarcaIdOrderByDataMovimentacaoDesc(cenario.comarcaId())
                .stream().anyMatch(movimento -> TipoMovimentacao.RETIRADA_OR.equals(movimento.getTipo())));
    }

    @Test
    void bloqueiaDevolucaoQuandoFerramentaNaoRetornaIntegralmente() {
        Cenario cenario = prepararCenario(true);
        OrdemServico os = criarOrdemServico(cenario, BigDecimal.valueOf(2), BigDecimal.ONE);
        OrdemRetirada or = ordemRetiradaService.executarRetirada(unicaOrDaOs(os).getId(), retiradaAssinada());

        DevolverOrdemRetiradaRequest request = devolucao(or, BigDecimal.ZERO, BigDecimal.ZERO);
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> ordemRetiradaService.devolver(or.getId(), request));

        assertTrue(erro.getMessage().contains("Ferramentas devem retornar obrigatoriamente"));
    }

    private Cenario prepararCenario(boolean comFerramenta) {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);

        Funcionario responsavel = new Funcionario();
        responsavel.setNome("Responsável " + sufixo);
        responsavel.setFuncao("Técnico");
        responsavel = funcionarioRepository.save(responsavel);

        Contrato contrato = new Contrato();
        contrato.setCliente("Cliente Integração");
        contrato.setContrato("Contrato INT-" + sufixo);
        contrato.setVigenciaInicio(LocalDate.now());
        contrato.setVigenciaFim(LocalDate.now().plusYears(1));
        contrato = contratoRepository.save(contrato);

        Projeto projeto = new Projeto();
        projeto.setContrato(contrato);
        projeto.setResponsavel(responsavel);
        projeto.setStatus(ProjetoStatus.EM_ANDAMENTO);
        projeto.setDataInicio(LocalDate.now());
        projeto.setDataFim(LocalDate.now().plusMonths(1));
        projeto = projetoRepository.save(projeto);

        Comarca comarca = new Comarca();
        comarca.setNomeComarca("Comarca Integração " + sufixo);
        comarca.setProjeto(projeto);
        comarca.setPercentualConcluido(BigDecimal.ZERO);
        comarca = comarcaRepository.save(comarca);

        Material consumo = novoMaterial("Cabo Integração", "CAB-" + sufixo, "MATERIAL_CONSUMO", 100);
        Material ferramenta = comFerramenta
                ? novoMaterial("Alicate Integração", "ALI-" + sufixo, "FERRAMENTA", 5)
                : null;

        entityManager.flush();
        entityManager.clear();
        return new Cenario(contrato.getId(), projeto.getId(), comarca.getId(), consumo.getId(),
                ferramenta != null ? ferramenta.getId() : null, contrato.getContrato());
    }

    private Material novoMaterial(String nome, String partNumber, String categoria, int quantidade) {
        Material material = new Material();
        material.setNome(nome);
        material.setPartNumber(partNumber);
        material.setCategoria(categoria);
        material.setTipoControle(TipoControleEstoque.UNIDADE);
        material.setUnidadeMedida(UnidadeMedida.UNIDADE);
        material.setQuantidadeDisponivel(quantidade);
        material.setLocalizacao("Estoque Central");
        return estoqueService.cadastrarMaterial(material);
    }

    private OrdemServico criarOrdemServico(Cenario cenario, BigDecimal quantidadeConsumo,
            BigDecimal quantidadeFerramenta) {
        CriarOrdemServicoRequest request = new CriarOrdemServicoRequest();
        request.setContratoId(cenario.contratoId());
        request.setProjetoId(cenario.projetoId());
        request.setDescricao("Execução do cenário de integração");
        LocalDateTime inicio = LocalDateTime.now().plusDays(1);
        request.setDataHoraInicio(inicio);
        request.setDataHoraFim(inicio.plusHours(8));
        request.setDeadline(inicio.plusDays(1));

        CriarOrdemServicoRequest.MaterialPrevistoRequest consumo = materialPrevisto(
                cenario.consumoId(), quantidadeConsumo);
        if (quantidadeFerramenta != null) {
            request.setMateriais(List.of(consumo,
                    materialPrevisto(cenario.ferramentaId(), quantidadeFerramenta)));
        } else {
            request.setMateriais(List.of(consumo));
        }
        return ordemServicoService.criar(request);
    }

    private CriarOrdemServicoRequest.MaterialPrevistoRequest materialPrevisto(Long materialId,
            BigDecimal quantidade) {
        CriarOrdemServicoRequest.MaterialPrevistoRequest item =
                new CriarOrdemServicoRequest.MaterialPrevistoRequest();
        item.setMaterialId(materialId);
        item.setQuantidadePrevista(quantidade);
        return item;
    }

    private OrdemRetirada unicaOrDaOs(OrdemServico os) {
        List<OrdemRetirada> ordens = ordemRetiradaRepository
                .findByOrdemServicoIdOrderByDataGeracaoDesc(os.getId());
        assertEquals(1, ordens.size());
        return ordens.getFirst();
    }

    private ExecutarOrdemRetiradaRequest retiradaAssinada() {
        ExecutarOrdemRetiradaRequest request = new ExecutarOrdemRetiradaRequest();
        request.setConferidoPor("Conferente Teste");
        request.setLevadoPor("Técnico Teste");
        request.setAssinaturaConferenteBase64(ASSINATURA);
        request.setAssinaturaRetiranteBase64(ASSINATURA);
        request.setAlocacoes(List.of());
        return request;
    }

    private DevolverOrdemRetiradaRequest devolucao(OrdemRetirada or, BigDecimal consumo,
            BigDecimal ferramenta) {
        DevolverOrdemRetiradaRequest request = new DevolverOrdemRetiradaRequest();
        request.setDevolvidoPor("Técnico Teste");
        request.setRecebidoPor("Almoxarife Teste");
        request.setAssinaturaRecebimentoBase64(ASSINATURA);
        request.setAlocacoes(List.of());
        request.setItens(or.getItens().stream()
                .map(item -> itemDevolucao(item, "FERRAMENTA".equals(item.getCategoria()) ? ferramenta : consumo))
                .toList());
        return request;
    }

    private DevolverOrdemRetiradaRequest.ItemDevolucaoRequest itemDevolucao(OrdemRetiradaItem item,
            BigDecimal quantidade) {
        DevolverOrdemRetiradaRequest.ItemDevolucaoRequest devolucao =
                new DevolverOrdemRetiradaRequest.ItemDevolucaoRequest();
        devolucao.setItemId(item.getId());
        devolucao.setQuantidadeDevolvida(quantidade);
        return devolucao;
    }

    private MaterialItem itemDaOr(OrdemRetirada or, Long materialId) {
        return or.getItens().stream()
                .filter(item -> item.getMaterial().getId().equals(materialId))
                .map(OrdemRetiradaItem::getMaterialItem)
                .findFirst()
                .orElseThrow();
    }

    private long contarMovimentos(List<MovimentacaoEstoque> movimentos, TipoMovimentacao tipo) {
        return movimentos.stream().filter(movimento -> tipo.equals(movimento.getTipo())).count();
    }

    private record Cenario(Long contratoId, Long projetoId, Long comarcaId, Long consumoId,
            Long ferramentaId, String numeroContrato) {
    }
}
