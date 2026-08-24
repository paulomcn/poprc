ALTER TABLE logs_operacoes_sensiveis
    ADD COLUMN IF NOT EXISTS tipo_evento VARCHAR(80),
    ADD COLUMN IF NOT EXISTS alvo_funcionario_id BIGINT,
    ADD COLUMN IF NOT EXISTS detalhes VARCHAR(1000);

CREATE INDEX IF NOT EXISTS idx_logs_operacoes_sensiveis_tipo_data
    ON logs_operacoes_sensiveis (tipo_evento, registrado_em DESC);

CREATE INDEX IF NOT EXISTS idx_logs_operacoes_sensiveis_alvo_data
    ON logs_operacoes_sensiveis (alvo_funcionario_id, registrado_em DESC);
