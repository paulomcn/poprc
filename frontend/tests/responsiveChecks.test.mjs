import test from 'node:test';
import assert from 'node:assert/strict';
import { responsiveFailures } from './responsiveChecks.mjs';

const valid = {
  viewport: { width: 320, height: 640 }, documentWidth: 320,
  main: { width: 305, scrollWidth: 305 }, header: { height: 64 },
  dialogs: [{ x: 12, y: 16, right: 308, bottom: 624 }], brokenImages: [],
};

test('aceita modal contido e tabela com rolagem interna sem estourar a pagina', () => {
  assert.deepEqual(responsiveFailures(valid), []);
});
test('detecta regressao observada no modal de projeto e cabecalho', () => {
  const result = responsiveFailures({ ...valid, header: { height: 41 },
    dialogs: [{ x: 16, y: -59.5, right: 304, bottom: 719.5 }] });
  assert.deepEqual(result, ['Application header shorter than 56px', 'Dialog outside viewport']);
});
test('detecta overflow e imagem quebrada', () => {
  assert.deepEqual(responsiveFailures({ ...valid, documentWidth: 700,
    main: { width: 305, scrollWidth: 500 }, brokenImages: ['Produto'] }),
  ['Horizontal page overflow', 'Horizontal main overflow', 'Broken visible images']);
});
