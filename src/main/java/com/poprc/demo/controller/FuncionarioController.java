package com.poprc.demo.controller;

import com.poprc.demo.model.Funcionario;
import com.poprc.demo.repository.FuncionarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/funcionarios")
public class FuncionarioController {

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    /**
     * Inserir novo funcionário (acesso restrito ao time de RH)
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
     * Listar todos os funcionários
     */
    @GetMapping
    public ResponseEntity<List<Funcionario>> listarTodos() {
        List<Funcionario> funcionarios = funcionarioRepository.findAll();
        return ResponseEntity.ok(funcionarios);
    }

    /**
     * Buscar funcionários por nome
     */
    @GetMapping("/nome")
    public ResponseEntity<List<Funcionario>> buscarPorNome(@RequestParam String nome) {
        List<Funcionario> funcionarios = funcionarioRepository.findByNome(nome);
        return ResponseEntity.ok(funcionarios);
    }

    /**
     * Buscar funcionários por função
     */
    @GetMapping("/funcao")
    public ResponseEntity<List<Funcionario>> buscarPorFuncao(@RequestParam String funcao) {
        List<Funcionario> funcionarios = funcionarioRepository.findByFuncao(funcao);
        return ResponseEntity.ok(funcionarios);
    }

    /**
     * Buscar funcionários por cidade
     */
    @GetMapping("/cidade")
    public ResponseEntity<List<Funcionario>> buscarPorCidade(@RequestParam String cidade) {
        List<Funcionario> funcionarios = funcionarioRepository.findByCidade(cidade);
        return ResponseEntity.ok(funcionarios);
    }
}
