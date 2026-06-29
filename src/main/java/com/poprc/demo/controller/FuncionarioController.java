package com.poprc.demo.controller;

import com.poprc.demo.model.Funcionario;
import com.poprc.demo.repository.FuncionarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/funcionarios")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class FuncionarioController {

    private final FuncionarioRepository funcionarioRepository;

    /**
     * 📥 POST: Inserir novo funcionário
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> inserirFuncionario(@RequestBody Funcionario funcionario) {
        try {
            Map<String, Object> response = new HashMap<>();

            if (funcionario.getNome() == null || funcionario.getNome().trim().isEmpty()) {
                response.put("erro", "Nome do funcionário é obrigatório");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            Funcionario funcionarioSalvo = funcionarioRepository.save(funcionario);
            response.put("funcionario", funcionarioSalvo);
            response.put("mensagem", "Funcionário inserido com sucesso");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, Object> erro = new HashMap<>();
            erro.put("erro", "Erro ao inserir funcionário: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
        }
    }

    /**
     * 🔍 GET /{id}: Buscar funcionário por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Funcionario> buscarPorId(@PathVariable Long id) {
        return funcionarioRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * ✏️ PUT /{id}: Atualizar funcionário existente
     */
    @PutMapping("/{id}")
    @Transactional // 💥 MÁGICA AQUI: Força o Spring a gravar as alterações no banco!
    public ResponseEntity<Map<String, Object>> atualizarFuncionario(@PathVariable Long id,
            @RequestBody Funcionario dadosAtualizados) {
        Map<String, Object> response = new HashMap<>();

        return funcionarioRepository.findById(id)
                .map(funcionario -> {
                    // Atualiza os campos básicos
                    funcionario.setNome(dadosAtualizados.getNome());
                    funcionario.setFuncao(dadosAtualizados.getFuncao());
                    funcionario.setCidade(dadosAtualizados.getCidade());

                    // 💥 MÁGICA 2: Pluga as listas que tavam de fora!
                    if (dadosAtualizados.getCertificacoes() != null) {
                        funcionario.setCertificacoes(dadosAtualizados.getCertificacoes());
                    }
                    if (dadosAtualizados.getDocumentPaths() != null) {
                        funcionario.setDocumentPaths(dadosAtualizados.getDocumentPaths());
                    }

                    Funcionario salvo = funcionarioRepository.save(funcionario);
                    response.put("funcionario", salvo);
                    response.put("mensagem", "Funcionário atualizado com sucesso!");
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 📋 Listar todos os funcionários
     */
    @GetMapping
    public ResponseEntity<List<Funcionario>> listarTodos() {
        return ResponseEntity.ok(funcionarioRepository.findAll());
    }

    @GetMapping("/nome")
    public ResponseEntity<List<Funcionario>> buscarPorNome(@RequestParam String nome) {
        return ResponseEntity.ok(funcionarioRepository.findByNome(nome));
    }

    @GetMapping("/funcao")
    public ResponseEntity<List<Funcionario>> buscarPorFuncao(@RequestParam String funcao) {
        return ResponseEntity.ok(funcionarioRepository.findByFuncao(funcao));
    }

    @GetMapping("/cidade")
    public ResponseEntity<List<Funcionario>> buscarPorCidade(@RequestParam String cidade) {
        return ResponseEntity.ok(funcionarioRepository.findByCidade(cidade));
    }
}