package com.poprc.demo.controller;

import com.poprc.demo.model.Contrato;
import com.poprc.demo.repository.ContratoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/contratos")
public class ContratoController {

    @Autowired
    private ContratoRepository contratoRepository;

    private final String UPLOAD_DIR = "uploads/contratos/";

    /**
     * Salvar novo contrato com anexos (edital, proposta, aditivos)
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> salvarContrato(
            @RequestParam String cliente,
            @RequestParam String contrato,
            @RequestParam(required = false) MultipartFile edital,
            @RequestParam(required = false) MultipartFile proposta,
            @RequestParam(required = false) MultipartFile[] aditivos) {

        try {
            Map<String, Object> response = new HashMap<>();
            Contrato novoContrato = new Contrato();
            novoContrato.setCliente(cliente);
            novoContrato.setContrato(contrato);

            Map<String, String> caminhos = new HashMap<>();

            // Criar diretório se não existir
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Salvar edital
            if (edital != null && !edital.isEmpty()) {
                String caminhoEdital = salvarArquivo(edital, "edital");
                caminhos.put("edital", caminhoEdital);
            }

            // Salvar proposta
            if (proposta != null && !proposta.isEmpty()) {
                String caminhoProposta = salvarArquivo(proposta, "proposta");
                caminhos.put("proposta", caminhoProposta);
            }

            // Salvar aditivos
            if (aditivos != null && aditivos.length > 0) {
                for (int i = 0; i < aditivos.length; i++) {
                    if (!aditivos[i].isEmpty()) {
                        String caminhoAditivo = salvarArquivo(aditivos[i], "aditivo_" + i);
                        caminhos.put("aditivo_" + i, caminhoAditivo);
                    }
                }
            }

            Contrato contratoSalvo = contratoRepository.save(novoContrato);
            response.put("contrato", contratoSalvo);
            response.put("arquivos", caminhos);
            response.put("mensagem", "Contrato salvo com sucesso");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IOException e) {
            Map<String, Object> erro = new HashMap<>();
            erro.put("erro", "Erro ao salvar contrato: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
        }
    }

    /**
     * Listar todos os contratos
     */
    @GetMapping
    public ResponseEntity<List<Contrato>> listarTodos() {
        List<Contrato> contratos = contratoRepository.findAll();
        return ResponseEntity.ok(contratos);
    }

    /**
     * Listar contratos ativos (dentro da vigência)
     */
    @GetMapping("/ativos")
    public ResponseEntity<List<Contrato>> listarAtivos() {
        List<Contrato> contratosAtivos = contratoRepository.findContratoAtivos();
        return ResponseEntity.ok(contratosAtivos);
    }

    /**
     * Buscar contratos por cliente
     */
    @GetMapping("/cliente")
    public ResponseEntity<List<Contrato>> buscarPorCliente(@RequestParam String cliente) {
        List<Contrato> contratos = contratoRepository.findByCliente(cliente);
        return ResponseEntity.ok(contratos);
    }

    /**
     * Método auxiliar para salvar arquivo
     */
    private String salvarArquivo(MultipartFile arquivo, String tipo) throws IOException {
        String nomeOriginal = arquivo.getOriginalFilename();
        String extensao = nomeOriginal != null ? nomeOriginal.substring(nomeOriginal.lastIndexOf(".")) : "";
        String nomeArquivo = tipo + "_" + UUID.randomUUID() + extensao;
        Path caminho = Paths.get(UPLOAD_DIR + nomeArquivo);

        Files.write(caminho, arquivo.getBytes());
        return caminho.toString();
    }
}
