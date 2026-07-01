package com.poprc.demo.controller;

import com.poprc.demo.model.Contrato;
import com.poprc.demo.repository.ContratoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contratos")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ContratoController {

    private final ContratoRepository contratoRepository;

    /**
     * POST: Salvar novo contrato recebendo JSON do React
     * (a versão anterior usava @RequestParam multipart — incompatível com o front
     * atual)
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> salvarContrato(@RequestBody Contrato novoContrato) {
        Map<String, Object> response = new HashMap<>();

        if (novoContrato.getCliente() == null || novoContrato.getCliente().isBlank()) {
            response.put("erro", "O campo 'cliente' é obrigatório.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        if (novoContrato.getContrato() == null || novoContrato.getContrato().isBlank()) {
            response.put("erro", "O campo 'contrato' (número) é obrigatório.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        if (novoContrato.getStatus() == null || novoContrato.getStatus().isBlank()) {
            novoContrato.setStatus("ATIVO");
        }

        Contrato salvo = contratoRepository.save(novoContrato);
        response.put("contrato", salvo);
        response.put("mensagem", "Contrato salvo com sucesso");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /{id}: Buscar contrato específico
     */
    @GetMapping("/{id}")
    public ResponseEntity<Contrato> buscarPorId(@PathVariable Long id) {
        return contratoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * PUT /{id}: Atualizar contrato existente
     */
    @PutMapping("/{id}")
    public ResponseEntity<Contrato> atualizarContrato(
            @PathVariable Long id,
            @RequestBody Contrato dadosAtualizados) {

        return contratoRepository.findById(id)
                .map(contrato -> {
                    contrato.setCliente(dadosAtualizados.getCliente());
                    contrato.setContrato(dadosAtualizados.getContrato());
                    contrato.setVigenciaInicio(dadosAtualizados.getVigenciaInicio());
                    contrato.setVigenciaFim(dadosAtualizados.getVigenciaFim());
                    contrato.setValorGlobal(dadosAtualizados.getValorGlobal());
                    contrato.setEscopo(dadosAtualizados.getEscopo());
                    contrato.setStatus(dadosAtualizados.getStatus());
                    return ResponseEntity.ok(contratoRepository.save(contrato));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET: Listar todos os contratos
     */
    @GetMapping
    public ResponseEntity<List<Contrato>> listarTodos() {
        return ResponseEntity.ok(contratoRepository.findAll());
    }

    /**
     * GET /ativos: Contratos dentro da vigência atual
     */
    @GetMapping("/ativos")
    public ResponseEntity<List<Contrato>> listarAtivos() {
        return ResponseEntity.ok(contratoRepository.findContratoAtivos());
    }

    /**
     * GET /cliente?cliente=...: Buscar por nome do cliente
     */
    @GetMapping("/cliente")
    public ResponseEntity<List<Contrato>> buscarPorCliente(@RequestParam String cliente) {
        return ResponseEntity.ok(contratoRepository.findByCliente(cliente));
    }
}