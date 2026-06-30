package com.poprc.demo.controller;

import com.poprc.demo.model.Projeto;
import com.poprc.demo.model.Contrato;
import com.poprc.demo.model.ProjetoStatus;
import com.poprc.demo.repository.ProjetoRepository;
import com.poprc.demo.repository.ContratoRepository;
import com.poprc.demo.service.ComarcaService; //   NOVO IMPORT
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projetos")
@CrossOrigin(origins = "*")
public class ProjetoController {

    private final ProjetoRepository projetoRepository;
    private final ContratoRepository contratoRepository;
    private final ComarcaService comarcaService; // NOVO

    public ProjetoController(ProjetoRepository projetoRepository, ContratoRepository contratoRepository,
            ComarcaService comarcaService) {
        this.projetoRepository = projetoRepository;
        this.contratoRepository = contratoRepository;
        this.comarcaService = comarcaService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> salvarProjeto(@RequestBody Projeto projeto) {
        try {
            Map<String, Object> response = new HashMap<>();

            if (projeto.getContrato() == null || projeto.getContrato().getId() == null) {
                response.put("erro", "Contrato ID é obrigatório");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            Optional<Contrato> contrato = contratoRepository.findById(projeto.getContrato().getId());
            if (!contrato.isPresent()) {
                response.put("erro", "Contrato não encontrado");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            if (projeto.getStatus() == null) {
                projeto.setStatus(ProjetoStatus.EM_ANDAMENTO);
            }
            if (projeto.getAsBuiltStatus() == null) {
                projeto.setAsBuiltStatus("PENDENTE");
            }

            projeto.setContrato(contrato.get());

            Projeto projetoSalvo = projetoRepository.save(projeto);

            // NOVO: sincroniza automaticamente com Gestão de Comarcas
            projetoSalvo.setNomeComarcaVinculada(projeto.getNomeComarcaVinculada());
            comarcaService.criarOuVincularComarcaParaProjeto(projetoSalvo);

            response.put("projeto", projetoSalvo);
            response.put("mensagem", "Projeto saved com sucesso");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, Object> erro = new HashMap<>();
            erro.put("erro", "Erro ao salvar projeto: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
        }
    }

    /**
     * GET /{id}: Buscar projeto específico por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Projeto> buscarPorId(@PathVariable Long id) {
        return projetoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * ️ PUT /{id}: Atualizar dados do projeto e Status via formulário do React
     */
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> atualizarProjeto(@PathVariable Long id,
            @RequestBody Projeto dadosAtualizados) {
        Map<String, Object> response = new HashMap<>();

        return projetoRepository.findById(id)
                .map(projeto -> {
                    projeto.setDataInicio(dadosAtualizados.getDataInicio());
                    projeto.setDataFim(dadosAtualizados.getDataFim());
                    projeto.setStatus(dadosAtualizados.getStatus());
                    projeto.setAsBuiltStatus(dadosAtualizados.getAsBuiltStatus());

                    if (dadosAtualizados.getContrato() != null && dadosAtualizados.getContrato().getId() != null) {
                        contratoRepository.findById(dadosAtualizados.getContrato().getId())
                                .ifPresent(projeto::setContrato);
                    }

                    Projeto salvo = projetoRepository.save(projeto);
                    response.put("projeto", salvo);
                    response.put("mensagem", "Projeto atualizado com sucesso!");
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Listar todos os projetos
     */
    @GetMapping
    public ResponseEntity<List<Projeto>> listarTodos() {
        List<Projeto> projetos = projetoRepository.findAll();
        return ResponseEntity.ok(projetos);
    }

    /**
     * Listar projetos vinculados a um contrato específico
     */
    @GetMapping("/contrato/{contratoId}")
    public ResponseEntity<Map<String, Object>> listarPorContrato(@PathVariable Long contratoId) {
        try {
            Optional<Contrato> contrato = contratoRepository.findById(contratoId);
            if (!contrato.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            List<Projeto> projetos = projetoRepository.findByContratoId(contratoId);
            Map<String, Object> response = new HashMap<>();
            response.put("contrato", contrato.get());
            response.put("projetos", projetos);
            response.put("total", projetos.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Listar projetos por responsável
     */
    @GetMapping("/responsavel/{responsavelId}")
    public ResponseEntity<List<Projeto>> listarPorResponsavel(@PathVariable Long responsavelId) {
        List<Projeto> projetos = projetoRepository.findByResponsavelId(responsavelId);
        return ResponseEntity.ok(projetos);
    }

    /**
     * Buscar auditoria de materiais e status de As-Built do Projeto
     */
    @GetMapping("/{id}/auditoria")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> buscarAuditoria(@PathVariable Long id) {
        Optional<Projeto> projetoOpt = projetoRepository.findById(id);
        if (!projetoOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Projeto projeto = projetoOpt.get();
        Map<String, Object> response = new HashMap<>();

        response.put("asBuiltStatus", projeto.getAsBuiltStatus() != null ? projeto.getAsBuiltStatus() : "PENDENTE");

        List<Map<String, Object>> materiaisMapeados = projeto.getMateriais().stream().map(mat -> {
            Map<String, Object> m = new HashMap<>();
            m.put("nome", mat.getMaterial() != null ? mat.getMaterial().getNome() : "Material não identificado");
            m.put("previsto", mat.getQuantidadePrevista());
            m.put("utilizado", mat.getQuantidadeUtilizada());
            return m;
        }).collect(Collectors.toList());

        response.put("materials", materiaisMapeados);

        return ResponseEntity.ok(response);
    }

    /**
     * Homologar Documento alterando o registro definitivo no Postgres
     */
    @PutMapping("/{id}/as-built/homologar")
    @Transactional
    public ResponseEntity<Map<String, String>> homologarAsBuilt(@PathVariable Long id) {
        Optional<Projeto> projetoOpt = projetoRepository.findById(id);
        if (!projetoOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Projeto projeto = projetoOpt.get();
        projeto.setAsBuiltStatus("HOMOLOGADO");
        projetoRepository.save(projeto);

        return ResponseEntity.ok(Map.of(
                "status", "HOMOLOGADO",
                "mensagem", "As-Built homologado com sucesso no banco de dados!"));
    }
}