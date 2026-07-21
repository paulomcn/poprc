package com.poprc.demo.service;

import com.poprc.demo.dto.PendenciaOperacionalDTO;
import com.poprc.demo.model.Comarca;
import com.poprc.demo.model.OrdemServico;
import com.poprc.demo.model.Projeto;
import com.poprc.demo.model.StatusOS;
import com.poprc.demo.repository.ComarcaRepository;
import com.poprc.demo.repository.OrdemServicoRepository;
import com.poprc.demo.repository.ProjetoRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PendenciaOperacionalService {

    public static final String AREA_OBRAS = "GESTAO_OBRAS";
    public static final String AREA_ESTOQUE = "ESTOQUE";
    public static final String AREA_TECNICO = "TECNICO";
    public static final String AREA_ADMINISTRACAO = "ADMINISTRACAO";
    public static final String AREA_AUDITORIA = "AUDITORIA";

    private final OrdemServicoRepository ordemServicoRepository;
    private final ComarcaRepository comarcaRepository;
    private final ProjetoRepository projetoRepository;

    @Transactional(readOnly = true)
    public List<PendenciaOperacionalDTO> listar(String area, Long funcionarioId) {
        String areaNormalizada = normalizarArea(area);
        Set<Long> projetosDoFuncionario = funcionarioId == null
                ? Set.of()
                : projetoRepository.findByResponsavelId(funcionarioId).stream()
                        .map(Projeto::getId)
                        .collect(Collectors.toSet());
        Map<Long, Comarca> comarcaPorOs = comarcaRepository.findAll().stream()
                .filter(comarca -> comarca.getOrdemServico() != null
                        && comarca.getOrdemServico().getId() != null)
                .collect(Collectors.toMap(
                        comarca -> comarca.getOrdemServico().getId(),
                        Function.identity(),
                        (primeira, ignorada) -> primeira));

        List<PendenciaOperacionalDTO> pendencias = new ArrayList<>();
        ordemServicoRepository.findAll().stream()
                .filter(os -> !Boolean.TRUE.equals(os.getArquivado()))
                .filter(os -> funcionarioId == null
                        || (os.getProjeto() != null && projetosDoFuncionario.contains(os.getProjeto().getId())))
                .forEach(os -> {
                    Comarca comarca = comarcaPorOs.get(os.getId());
                    PendenciaOperacionalDTO pendencia = pendenciaDoStatus(os, comarca);
                    if (pendencia != null && correspondeArea(pendencia, areaNormalizada)) {
                        pendencias.add(pendencia);
                    }
                    if (comarca != null && Boolean.TRUE.equals(comarca.getFaltouMaterial())) {
                        PendenciaOperacionalDTO falta = pendenciaMaterialFaltante(os, comarca);
                        if (correspondeArea(falta, areaNormalizada)) {
                            pendencias.add(falta);
                        }
                    }
                });

        return pendencias.stream()
                .sorted(Comparator
                        .comparingInt((PendenciaOperacionalDTO item) -> pesoPrioridade(item.prioridade()))
                        .thenComparing(PendenciaOperacionalDTO::deadline,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(PendenciaOperacionalDTO::numeroOs,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    private PendenciaOperacionalDTO pendenciaDoStatus(OrdemServico os, Comarca comarca) {
        StatusOS status = os.getStatus() != null ? os.getStatus() : StatusOS.ABERTA;
        return switch (status) {
            case ABERTA -> criar(os, comarca, "PREPARAR_OS", AREA_ADMINISTRACAO,
                    "Preparar ordem de serviço", "A OS ainda precisa entrar no fluxo operacional.",
                    "Revisar OS", "/ordens-servico");
            case AGUARDANDO_VISTORIA -> criar(os, comarca, "REALIZAR_VISTORIA", AREA_OBRAS,
                    "Realizar vistoria", "Envie a foto e colete a assinatura para liberar a retirada.",
                    "Abrir vistoria", "/obras");
            case AGUARDANDO_RETIRADA -> criar(os, comarca, "EXECUTAR_RETIRADA", AREA_ESTOQUE,
                    "Executar retirada", "A vistoria foi aprovada e a OR está pronta para execução.",
                    "Abrir estoque", "/estoque");
            case EM_EXECUCAO -> criar(os, comarca, "REGISTRAR_EXECUCAO", AREA_TECNICO,
                    "Registrar execução em campo", "Registre atividades e evidências e envie o relatório.",
                    "Abrir OS", "/tecnico/os/" + os.getId());
            case AGUARDANDO_VALIDACAO -> criar(os, comarca, "VALIDAR_RELATORIO", AREA_ADMINISTRACAO,
                    "Validar relatório técnico", "A execução aguarda aprovação ou devolução para ajuste.",
                    "Validar relatório", "/ordens-servico");
            case AGUARDANDO_DEVOLUCAO -> criar(os, comarca, "REGISTRAR_DEVOLUCAO", AREA_ESTOQUE,
                    "Registrar devolução", "O relatório foi aprovado e os itens da OR devem ser conciliados.",
                    "Abrir devolução", "/estoque");
            case AGUARDANDO_AUDITORIA -> criar(os, comarca, "HOMOLOGAR_AUDITORIA", AREA_AUDITORIA,
                    "Homologar auditoria", "A devolução terminou e o As-Built aguarda homologação.",
                    "Abrir auditoria", "/auditoria/tecnica");
            case AGUARDANDO_ENCERRAMENTO -> criar(os, comarca, "ENCERRAR_OBRA", AREA_OBRAS,
                    "Encerrar obra", "Preencha e assine o documento final para concluir a obra.",
                    "Abrir encerramento", "/obras");
            case CONCLUIDA, FATURADA -> null;
        };
    }

    private PendenciaOperacionalDTO pendenciaMaterialFaltante(OrdemServico os, Comarca comarca) {
        String descricao = comarca.getDescricaoMaterialFaltante();
        return new PendenciaOperacionalDTO(
                "MATERIAL_FALTANTE-" + os.getId(),
                "MATERIAL_FALTANTE",
                AREA_ESTOQUE,
                "CRITICA",
                "Material faltante",
                descricao == null || descricao.isBlank()
                        ? "A obra possui material faltante registrado."
                        : descricao,
                os.getId(),
                os.getNumeroOs(),
                comarca.getId(),
                comarca.getNomeComarca(),
                responsavel(os),
                os.getDeadline(),
                "Tratar falta",
                "/estoque");
    }

    private PendenciaOperacionalDTO criar(OrdemServico os, Comarca comarca, String tipo, String area,
            String titulo, String descricao, String acao, String rota) {
        return new PendenciaOperacionalDTO(
                tipo + "-" + os.getId(),
                tipo,
                area,
                prioridade(os.getDeadline()),
                titulo,
                descricao,
                os.getId(),
                os.getNumeroOs(),
                comarca != null ? comarca.getId() : null,
                comarca != null ? comarca.getNomeComarca() : null,
                responsavel(os),
                os.getDeadline(),
                acao,
                rota);
    }

    private String responsavel(OrdemServico os) {
        return os.getProjeto() != null && os.getProjeto().getResponsavel() != null
                ? os.getProjeto().getResponsavel().getNome()
                : null;
    }

    private String prioridade(LocalDateTime deadline) {
        if (deadline == null) {
            return "NORMAL";
        }
        LocalDateTime agora = LocalDateTime.now();
        if (deadline.isBefore(agora)) {
            return "CRITICA";
        }
        return deadline.isBefore(agora.plusHours(24)) ? "ALTA" : "NORMAL";
    }

    private boolean correspondeArea(PendenciaOperacionalDTO pendencia, String area) {
        return area == null || area.equals(pendencia.areaResponsavel());
    }

    private String normalizarArea(String area) {
        if (area == null || area.isBlank()) {
            return null;
        }
        String normalizada = area.trim().toUpperCase();
        if (!Set.of(AREA_OBRAS, AREA_ESTOQUE, AREA_TECNICO, AREA_ADMINISTRACAO, AREA_AUDITORIA)
                .contains(normalizada)) {
            throw new IllegalArgumentException("Área responsável inválida: " + area + ".");
        }
        return normalizada;
    }

    private int pesoPrioridade(String prioridade) {
        return switch (prioridade) {
            case "CRITICA" -> 0;
            case "ALTA" -> 1;
            default -> 2;
        };
    }
}
