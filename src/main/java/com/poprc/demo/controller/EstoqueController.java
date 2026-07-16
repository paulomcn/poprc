package com.poprc.demo.controller;

import com.poprc.demo.model.Material;
import com.poprc.demo.model.MovimentacaoEstoque;
import com.poprc.demo.repository.MaterialRepository;
import com.poprc.demo.repository.MovimentacaoEstoqueRepository; //  IMPORT DO REPOSITORY NOVO
import com.poprc.demo.service.EstoqueService;
import com.poprc.demo.service.UnidadeEstoqueRastreavelService;
import com.poprc.demo.model.UnidadeEstoqueRastreavel;
import com.poprc.demo.model.LocalEstoque;
import com.poprc.demo.model.SaldoMaterialLocal;
import com.poprc.demo.service.SaldoLocalService;
import lombok.RequiredArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/estoque")
@RequiredArgsConstructor
public class EstoqueController {

    private final EstoqueService estoqueService;
    private final MaterialRepository materialRepository;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository; // INJEÇÃO DIRETA
    private final UnidadeEstoqueRastreavelService unidadeRastreavelService;
    private final SaldoLocalService saldoLocalService;

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> tratarRequisicaoInvalida(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("erro", exception.getMessage()));
    }

    @GetMapping("/materiais")
    public ResponseEntity<List<Material>> listarMateriais() {
        List<Material> materiais = materialRepository.findAll();
        return ResponseEntity.ok(materiais);
    }

    @PostMapping("/materiais")
    public ResponseEntity<Material> cadastrarNovoMaterial(@RequestBody Material material) {
        Material salvo = estoqueService.cadastrarMaterial(material);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    @PutMapping("/materiais/{id}")
    public ResponseEntity<Material> atualizarMaterial(@PathVariable Long id, @RequestBody Material materialAtualizado) {
        return ResponseEntity.ok(estoqueService.atualizarMaterial(id, materialAtualizado));
    }

    // ROTA DO HISTÓRICO CORRIGIDA: Buscando direto do banco pelo repository
    @GetMapping("/historico")
    public ResponseEntity<List<MovimentacaoEstoque>> listarHistorico() {
        List<MovimentacaoEstoque> historico = movimentacaoEstoqueRepository.findAllByOrderByDataMovimentacaoDesc();
        return ResponseEntity.ok(historico);
    }

    @PostMapping("/entrada")
    public ResponseEntity<MovimentacaoEstoque> registrarEntrada(@RequestBody EntradaRequest request) {
        MovimentacaoEstoque movimentacao = estoqueService.registrarEntrada(
                request.getMaterialId(),
                request.getQuantidade(),
                request.getMetragem(),
                request.getFuncionarioId(),
                request.getLocalEstoqueId());
        return ResponseEntity.status(HttpStatus.CREATED).body(movimentacao);
    }

    @PostMapping("/saida")
    public ResponseEntity<MovimentacaoEstoque> registrarSaida(@RequestBody SaidaRequest request) {
        MovimentacaoEstoque movimentacao = estoqueService.registrarSaida(
                request.getMaterialId(),
                request.getQuantidade(),
                request.getFuncionarioId(),
                request.getComarcaId());
        return ResponseEntity.status(HttpStatus.CREATED).body(movimentacao);
    }

    @PostMapping("/ajustes")
    public ResponseEntity<MovimentacaoEstoque> registrarAjuste(@RequestBody AjusteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(estoqueService.registrarAjuste(
                request.getMaterialId(), request.getLocalEstoqueId(), request.getValor(), request.getTipo(), request.getMotivo(),
                request.getLancadoPor(), request.getAutorizadoPor()));
    }

    @PostMapping("/transferencias")
    public ResponseEntity<MovimentacaoEstoque> transferir(@RequestBody TransferenciaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(estoqueService.transferirLocalizacao(
                request.getMaterialId(), request.getOrigemId(), request.getDestinoId(), request.getValor(), request.getMotivo(),
                request.getLancadoPor(), request.getAutorizadoPor()));
    }

    @GetMapping("/locais")
    public ResponseEntity<List<LocalEstoque>> listarLocais() {
        return ResponseEntity.ok(saldoLocalService.listarLocais());
    }

    @PostMapping("/locais")
    public ResponseEntity<LocalEstoque> cadastrarLocal(@RequestBody LocalEstoqueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(saldoLocalService.cadastrarLocal(request.getNome(), request.getEndereco()));
    }

    @GetMapping("/saldos-locais")
    public ResponseEntity<List<SaldoMaterialLocal>> listarSaldosLocais(
            @RequestParam(required = false) Long materialId) {
        return ResponseEntity.ok(saldoLocalService.listarSaldos(materialId));
    }

    @PatchMapping("/saldos-locais/{id}/estoque-minimo")
    public ResponseEntity<SaldoMaterialLocal> atualizarEstoqueMinimoLocal(
            @PathVariable Long id, @RequestBody EstoqueMinimoLocalRequest request) {
        return ResponseEntity.ok(saldoLocalService.atualizarEstoqueMinimo(id, request.estoqueMinimo()));
    }

    public record EstoqueMinimoLocalRequest(BigDecimal estoqueMinimo) {
    }

    @GetMapping("/unidades-rastreaveis")
    public ResponseEntity<List<UnidadeEstoqueRastreavel>> listarUnidadesRastreaveis(
            @RequestParam(required = false) Long materialId) {
        return ResponseEntity.ok(unidadeRastreavelService.listar(materialId));
    }

    @PostMapping("/unidades-rastreaveis")
    public ResponseEntity<UnidadeEstoqueRastreavel> cadastrarUnidadeRastreavel(
            @RequestBody UnidadeRastreavelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(unidadeRastreavelService.cadastrar(
                request.getMaterialId(), request.getCodigo(), request.getMetragemInicial(), request.getObservacao(),
                request.getLocalEstoqueId()));
    }

    @PatchMapping("/unidades-rastreaveis/{id}/transferir")
    public ResponseEntity<UnidadeEstoqueRastreavel> transferirUnidadeRastreavel(
            @PathVariable Long id, @RequestBody TransferenciaUnidadeRequest request) {
        return ResponseEntity.ok(unidadeRastreavelService.transferir(
                id, request.getDestinoId(), request.getMotivo(), request.getLancadoPor(), request.getAutorizadoPor()));
    }

    @Data
    public static class EntradaRequest {
        private Long materialId;
        private Integer quantidade;
        private BigDecimal metragem;
        private Long funcionarioId;
        private Long localEstoqueId;
    }

    @Data
    public static class SaidaRequest {
        private Long materialId;
        private Integer quantidade;
        private Long funcionarioId;
        private Long comarcaId;
    }

    @Data
    public static class UnidadeRastreavelRequest {
        private Long materialId;
        private String codigo;
        private BigDecimal metragemInicial;
        private String observacao;
        private Long localEstoqueId;
    }

    @Data
    public static class AjusteRequest {
        private Long materialId;
        private Long localEstoqueId;
        private BigDecimal valor;
        private com.poprc.demo.model.TipoMovimentacao tipo;
        private String motivo;
        private String lancadoPor;
        private String autorizadoPor;
    }

    @Data
    public static class TransferenciaRequest {
        private Long materialId;
        private Long origemId;
        private Long destinoId;
        private BigDecimal valor;
        private String motivo;
        private String lancadoPor;
        private String autorizadoPor;
    }

    @Data
    public static class LocalEstoqueRequest {
        private String nome;
        private String endereco;
    }

    @Data
    public static class TransferenciaUnidadeRequest {
        private Long destinoId;
        private String motivo;
        private String lancadoPor;
        private String autorizadoPor;
    }
}
