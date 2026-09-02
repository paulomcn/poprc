import assert from "node:assert/strict";
import test from "node:test";

import { PERMISSOES, perfisComPermissao, temPermissao } from "./permissions.js";

const matrizEsperada = {
  [PERMISSOES.DASHBOARD_VISUALIZAR]: ["ADMIN", "SUPERVISOR_TECNICO"],
  [PERMISSOES.CONTRATOS_VISUALIZAR]: ["ADMIN", "SUPERVISOR_TECNICO"],
  [PERMISSOES.PROJETOS_VISUALIZAR]: ["ADMIN", "SUPERVISOR_TECNICO"],
  [PERMISSOES.FUNCIONARIOS_VISUALIZAR]: ["ADMIN", "SUPERVISOR_TECNICO", "ESTOQUE"],
  [PERMISSOES.FUNCIONARIOS_GERENCIAR]: ["ADMIN"],
  [PERMISSOES.OS_VISUALIZAR]: ["ADMIN", "SUPERVISOR_TECNICO", "TECNICO"],
  [PERMISSOES.OS_GERENCIAR]: ["ADMIN", "SUPERVISOR_TECNICO"],
  [PERMISSOES.OBRAS_VISUALIZAR]: ["ADMIN", "SUPERVISOR_TECNICO", "TECNICO", "AUDITOR"],
  [PERMISSOES.OBRAS_EXECUTAR]: ["ADMIN", "SUPERVISOR_TECNICO", "TECNICO"],
  [PERMISSOES.OBRAS_GERENCIAR]: ["ADMIN", "SUPERVISOR_TECNICO"],
  [PERMISSOES.ESTOQUE_VISUALIZAR]: ["ADMIN", "ESTOQUE"],
  [PERMISSOES.AUDITORIA_VISUALIZAR]: ["ADMIN", "AUDITOR"],
  [PERMISSOES.FINANCEIRO_VISUALIZAR]: ["ADMIN"],
  [PERMISSOES.NOTIFICACOES_VISUALIZAR]: ["ADMIN", "SUPERVISOR_TECNICO"],
  [PERMISSOES.ATIVIDADES_GERENCIAR]: ["ADMIN"],
  [PERMISSOES.PORTAL_TECNICO_VISUALIZAR]: ["ADMIN", "SUPERVISOR_TECNICO", "TECNICO"],
  [PERMISSOES.OR_GERENCIAR]: ["ADMIN", "ESTOQUE"],
  [PERMISSOES.DOCUMENTOS_EDITAR]: ["ADMIN", "SUPERVISOR_TECNICO", "TECNICO"],
};

test("mantem a matriz de permissoes explicita para todos os modulos", () => {
  assert.deepEqual(Object.keys(matrizEsperada).sort(), Object.values(PERMISSOES).sort());
  for (const [permissao, perfis] of Object.entries(matrizEsperada)) {
    assert.deepEqual(perfisComPermissao(permissao), perfis);
  }
});

test("nega perfil desconhecido e permissao inexistente", () => {
  assert.equal(temPermissao("PERFIL_INEXISTENTE", PERMISSOES.OS_VISUALIZAR), false);
  assert.equal(temPermissao("ADMIN", "modulo.inexistente"), false);
  assert.deepEqual(perfisComPermissao("modulo.inexistente"), []);
});
