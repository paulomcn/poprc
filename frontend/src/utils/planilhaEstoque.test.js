import test from "node:test";
import assert from "node:assert/strict";
import ExcelJS from "exceljs";
import {
  celulaPossuiFormulaSemResultado,
  localizarCabecalhoEstoque,
  valorDaCelula,
} from "./planilhaEstoque.js";

test("localiza a primeira tabela quando existem quadros auxiliares na mesma linha", () => {
  const workbook = new ExcelJS.Workbook();
  const sheet = workbook.addWorksheet("ESTOQUE ATUAL");
  sheet.getCell("A4").value = "Produto";
  sheet.getCell("B4").value = "Estoque APÓS RETIRADAS";
  sheet.getCell("C4").value = "Valor unitário";
  sheet.getCell("F4").value = "Produto";
  sheet.getCell("G4").value = "Estoque atual";
  sheet.getCell("H4").value = "Valor unitário";

  assert.deepEqual(localizarCabecalhoEstoque(sheet), {
    linha: 4,
    produto: 1,
    quantidade: 2,
    custo: 3,
  });
});

test("identifica fórmula compartilhada sem resultado armazenado", () => {
  const workbook = new ExcelJS.Workbook();
  const sheet = workbook.addWorksheet("CADASTRO_PRODUTOS");
  const cell = sheet.getCell("C50");
  cell.value = { sharedFormula: "C5" };

  assert.deepEqual(valorDaCelula(cell), cell.value);
  assert.equal(celulaPossuiFormulaSemResultado(cell), true);
});
