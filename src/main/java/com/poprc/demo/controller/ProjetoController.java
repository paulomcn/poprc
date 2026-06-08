package com.poprc.demo.controller;

import com.poprc.demo.model.Projeto;
import com.poprc.demo.model.Contrato;
import com.poprc.demo.repository.ProjetoRepository;
import com.poprc.demo.repository.ContratoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
}
