ALTER TABLE materiais
    ADD COLUMN IF NOT EXISTS ativo BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS removido_em TIMESTAMP,
    ADD COLUMN IF NOT EXISTS removido_por VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_materiais_ativo_nome
    ON materiais (ativo, nome);
