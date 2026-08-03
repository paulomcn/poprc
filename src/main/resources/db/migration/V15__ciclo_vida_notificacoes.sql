ALTER TABLE notificacoes_operacionais
    ADD COLUMN chave_base VARCHAR(180),
    ADD COLUMN resolvida_em TIMESTAMP,
    ADD COLUMN motivo_resolucao VARCHAR(500);

UPDATE notificacoes_operacionais
SET chave_base = chave
WHERE chave_base IS NULL;

ALTER TABLE notificacoes_operacionais
    ALTER COLUMN chave_base SET NOT NULL;

CREATE UNIQUE INDEX uk_notificacao_chave_base_ativa
    ON notificacoes_operacionais (chave_base)
    WHERE resolvida_em IS NULL;
