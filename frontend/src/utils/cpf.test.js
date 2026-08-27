import test from "node:test";
import assert from "node:assert/strict";
import { cpfCompleto, formatarCpf, normalizarCpf } from "./cpf.js";

test("formata o CPF progressivamente durante a digitação", () => {
  assert.equal(formatarCpf("700"), "700");
  assert.equal(formatarCpf("70008308403"), "700.083.084-03");
  assert.equal(formatarCpf("700.083.084-03"), "700.083.084-03");
});

test("remove a máscara antes do envio e limita a onze dígitos", () => {
  assert.equal(normalizarCpf("700.083.084-03"), "70008308403");
  assert.equal(normalizarCpf("70008308403999"), "70008308403");
  assert.equal(cpfCompleto("700.083.084-03"), true);
  assert.equal(cpfCompleto("700.083"), false);
});
