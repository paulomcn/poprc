package com.poprc.demo.controller;

import com.poprc.demo.model.Projeto;
import com.poprc.demo.model.Contrato;
import com.poprc.demo.model.ProjetoStatus;
import com.poprc.demo.repository.ProjetoRepository;
import com.poprc.demo.repository.ContratoRepository;
import com.poprc.demo.service.ComarcaService;
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
    private final ComarcaService comarcaService;

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

    @GetMapping("/{id}")
    public ResponseEntity<Projeto> buscarPorId(@PathVariable Long id) {
        return projetoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> atualizarProjeto(@PathVariable Long id,
            @RequestBody Projeto dadosAtualizados) {
        Map<String, Object> response = new HashMap<>();

        return projetoRepository.findById(id)
                .map(projeto -> {
                    if (dadosAtualizados.getDataInicio() != null)
                        projeto.setDataInicio(dadosAtualizados.getDataInicio());
                    if (dadosAtualizados.getDataFim() != null)
                        projeto.setDataFim(dadosAtualizados.getDataFim());
                    if (dadosAtualizados.getStatus() != null)
                        projeto.setStatus(dadosAtualizados.getStatus());
                    if (dadosAtualizados.getAsBuiltStatus() != null)
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

    @GetMapping
    public ResponseEntity<List<Projeto>> listarTodos() {
        return ResponseEntity.ok(projetoRepository.findAll());
    }

    /**
     * ✏️ PUT: Atualizar a quantidade utilizada de um material específico na
     * auditoria 💥
     */
    @PutMapping("/{projetoId}/materiais/{materialId}")
    @Transactional
    public ResponseEntity<Map<String, Object>> atualizarQuantidadeUtilizada(
            @PathVariable Long projetoId,
            @PathVariable Long materialId,
            @RequestParam Integer quantidadeUtilizada) {

        Map<String, Object> response = new HashMap<>();
        return projetoRepository.findById(projetoId).map(projeto -> {
            projeto.getMateriais().stream()
                    .filter(m -> m.getId().equals(materialId))
                    .findFirst()
                    .ifPresent(m -> m.setQuantidadeUtilizada(quantidadeUtilizada));

            projetoRepository.save(projeto);
            response.put("mensagem", "Inventário de auditoria sincronizado!");
            return ResponseEntity.ok(response);
        }).orElse(ResponseEntity.notFound().build());
    }

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
            m.put("id", mat.getId()); // ID do vínculo relacional 💥
            m.put("nome", mat.getMaterial() != null ? mat.getMaterial().getNome() : "Material não identificado");
            m.put("previsto", mat.getQuantidadePrevista());
            m.put("utilizado", mat.getQuantidadeUtilizada());
            return m;
        }).collect(Collectors.toList());

        response.put("materiais", materiaisMapeados); // Chave corrigida para bater com o React! 💥[cite: 1]
        return ResponseEntity.ok(response);
    }

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