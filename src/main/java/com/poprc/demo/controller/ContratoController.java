package com.poprc.demo.controller;

import com.poprc.demo.model.Contrato;
import com.poprc.demo.repository.ContratoRepository;
import lombok.RequiredArgsConstructor; //   Injeção moderna
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/contratos")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor // Ativa a injeção automática por construtor do Lombok
public class ContratoController {

    private final ContratoRepository contratoRepository; // Troca do @Autowired para private final
    private final String UPLOAD_DIR = "uploads/contratos/";

    /**
     * POST: Salvar novo contrato recebendo os novos campos + arquivos anexos
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> salvarContrato(
            @RequestParam String cliente,
            @RequestParam String contrato,
            @RequestParam(required = false) LocalDate vigenciaInicio, // Adicionado
            @RequestParam(required = false) LocalDate vigenciaFim, // Adicionado
            @RequestParam(required = false) BigDecimal valorGlobal, // Adicionado
            @RequestParam(required = false) String escopo, // Adicionado
            @RequestParam(required = false, defaultValue = "ATIVO") String status, // Adicionado
            @RequestParam(required = false) MultipartFile edital,
            @RequestParam(required = false) MultipartFile proposta,
            @RequestParam(required = false) MultipartFile[] aditivos) {

        try {
            Map<String, Object> response = new HashMap<>();
            Contrato novoContrato = new Contrato();
            novoContrato.setCliente(cliente);
            novoContrato.setContrato(contrato);
            novoContrato.setVigenciaInicio(vigenciaInicio); // Mapeado
            novoContrato.setVigenciaFim(vigenciaFim); // Mapeado
            novoContrato.setValorGlobal(valorGlobal); // Mapeado
            novoContrato.setEscopo(escopo); // Mapeado
            novoContrato.setStatus(status); // Mapeado

            Map<String, String> caminhos = new HashMap<>();

            // Criar diretório se não existir
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Salvar arquivos (Mantendo sua lógica original intacta)
            if (edital != null && !edital.isEmpty()) {
                caminhos.put("edital", salvarArquivo(edital, "edital"));
            }
            if (proposta != null && !proposta.isEmpty()) {
                caminhos.put("proposta", salvarArquivo(proposta, "proposta"));
            }
            if (aditivos != null && aditivos.length > 0) {
                for (int i = 0; i < aditivos.length; i++) {
                    if (!aditivos[i].isEmpty()) {
                        caminhos.put("aditivo_" + i, salvarArquivo(aditivos[i], "aditivo_" + i));
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
     * GET /{id}: Buscar contrato específico por ID (Gatilho para o botão
     * "Detalhes" do React)
     */
    @GetMapping("/{id}")
    public ResponseEntity<Contrato> buscarPorId(@PathVariable Long id) {
        return contratoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * ️ PUT /{id}: Atualizar dados cadastrais e Status via JSON enviado pelo React
     */
    @PutMapping("/{id}")
    public ResponseEntity<Contrato> atualizarContrato(@PathVariable Long id, @RequestBody Contrato dadosAtualizados) {
        return contratoRepository.findById(id)
                .map(contrato -> {
                    contrato.setCliente(dadosAtualizados.getCliente());
                    contrato.setContrato(dadosAtualizados.getContrato());
                    contrato.setVigenciaInicio(dadosAtualizados.getVigenciaInicio());
                    contrato.setVigenciaFim(dadosAtualizados.getVigenciaFim());
                    contrato.setValorGlobal(dadosAtualizados.getValorGlobal());
                    contrato.setEscopo(dadosAtualizados.getEscopo());
                    contrato.setStatus(dadosAtualizados.getStatus()); // Atualiza o status real do banco

                    Contrato salvo = contratoRepository.save(contrato);
                    return ResponseEntity.ok(salvo);
                })
                .orElse(ResponseEntity.notFound().build());
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