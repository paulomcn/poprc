import test from 'node:test';
import assert from 'node:assert/strict';
import { vincularComarcasAosProjetos } from './projetos.js';

test('exibe a obra pelo vinculo do projeto, sem depender da ordem das listas', () => {
  const projetos = [{ id: 1 }, { id: 2 }];
  const obras = [{ projeto: { id: '2' }, nomeComarca: 'Obra B' }, { projeto: { id: 1 }, nomeComarca: 'Obra A' }];
  const resultado = vincularComarcasAosProjetos(projetos, obras);
  assert.equal(resultado[0].comarca.nomeComarca, 'Obra A');
  assert.equal(resultado[1].comarca.nomeComarca, 'Obra B');
  assert.equal(projetos[0].comarca, undefined);
});

test('preserva projeto sem obra e vinculo ja fornecido pela API', () => {
  const existente = { nomeComarca: 'Obra existente' };
  const resultado = vincularComarcasAosProjetos([{ id: 1 }, { id: 2, comarca: existente }], []);
  assert.equal(resultado[0].comarca, undefined);
  assert.equal(resultado[1].comarca, existente);
});
