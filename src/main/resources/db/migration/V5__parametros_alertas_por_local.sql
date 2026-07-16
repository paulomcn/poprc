ALTER TABLE saldos_materiais_locais
    ADD COLUMN IF NOT EXISTS estoque_minimo NUMERIC(14, 3);

ALTER TABLE configuracoes_notificacao
    ADD COLUMN IF NOT EXISTS antecedencia_os_horas INTEGER NOT NULL DEFAULT 24;

ALTER TABLE configuracoes_notificacao
    ADD COLUMN IF NOT EXISTS antecedencia_contrato_dias INTEGER NOT NULL DEFAULT 30;
