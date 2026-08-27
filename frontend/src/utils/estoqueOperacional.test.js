import test from "node:test";
import assert from "node:assert/strict";
import {
  calcularSimulacaoRetirada,
  consolidarRetiradasPorObra,
} from "./estoqueOperacional.js";

const material = {
  id: 1,
  nome: "Cabo CAT6",
  tipoControle: "UNIDADE",
  quantidadeDisponivel: 10,
  quantidadeReservada: 2,
  custoMedio: 5,
};

test("não duplica uma retirada presente na OR e no histórico de importação", () => {
  const comarca = { id: 10, nomeComarca: "Cuité" };
  const resultado = consolidarRetiradasPorObra({
    comarcas: [comarca],
    materiais: [material],
    ordensRetirada: [{
      id: 20,
      numeroOr: "0001 - OS 01 - OR 01",
      comarca,
      itens: [{ material, nomeMaterial: material.nome, quantidadeRetirada: 4, quantidadeDevolvida: 1 }],
    }],
    retiradasImportadas: [{
      ordemRetiradaId: 20,
      numeroOr: "0001 - OS 01 - OR 01",
      comarcaId: 10,
      materialId: 1,
      material: material.nome,
      aba: "ORDEM DE RETIRADA - CUITÉ",
      quantidadeRetirada: 4,
      quantidadeFaltante: 2,
      custoUnitario: 5,
    }],
  });

  assert.equal(resultado[0].ordens.length, 1);
  assert.equal(resultado[0].totalRetirado, 4);
  assert.equal(resultado[0].totalDevolvido, 1);
  assert.equal(resultado[0].totalFaltante, 2);
  assert.deepEqual(resultado[0].ordens[0].abasOrigem, ["ORDEM DE RETIRADA - CUITÉ"]);
});

test("simula saldo, falta e valor sem alterar o material", () => {
  const resultado = calcularSimulacaoRetirada([material], [{ materialId: 1, quantidade: 12 }]);

  assert.equal(resultado.itens[0].saldoAtual, 8);
  assert.equal(resultado.itens[0].saldoProjetado, -4);
  assert.equal(resultado.quantidadeFaltante, 4);
  assert.equal(resultado.valorSolicitado, 60);
  assert.equal(resultado.possuiFalta, true);
  assert.equal(material.quantidadeDisponivel, 10);
});
