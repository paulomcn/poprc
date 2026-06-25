package com.poprc.demo.controller;

import com.poprc.demo.model.Projeto;
import com.poprc.demo.model.Contrato;
import com.poprc.demo.model.MaterialProjeto;
import com.poprc.demo.repository.ProjetoRepository;
import com.poprc.demo.repository.ContratoRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
public class ProjetoController {

    @Autowired
    private ProjetoRepository projetoRepository;

    @Autowired
    private ContratoRepository contratoRepository;

    /**
     * Salvar novo projeto vinculado a um contrato
     */
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

            Projeto projetoSalvo = projetoRepository.save(projeto);
            response.put("projeto", projetoSalvo);
            response.put("mensagem", "Projeto salvo com sucesso");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, Object> erro = new HashMap<>();
            erro.put("erro", "Erro ao salvar projeto: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
        }
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
     *  Buscar auditoria de materiais e status de As-Built do Projeto DIRETO DO BANCO
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
        
        // 💥 ADAPTAÇÃO PRO SEU MODEL REAL: Transforma a lista do banco no formato do Front
        List<Map<String, Object>> materiaisMapeados = projeto.getMateriais().stream().map(mat -> {
            Map<String, Object> m = new HashMap<>();
            // Busca o nome de dentro do objeto Material mapeado (ajuste se o campo for 'descricao')
            m.put("nome", mat.getMaterial() != null ? mat.getMaterial().getNome() : "Material não identificado");
            m.put("previsto", mat.getQuantidadePrevista());
            m.put("utilizado", mat.getQuantidadeUtilizada());
            return m;
        }).collect(Collectors.toList());

        response.put("materiais", materiaisMapeados);
        
        return ResponseEntity.ok(response);
    }

    /**
     *  Homologar Documento alterando o registro definitivo no Postgres
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
            "mensagem", "As-Built homologado com sucesso no banco de dados!"
        ));
    }
}