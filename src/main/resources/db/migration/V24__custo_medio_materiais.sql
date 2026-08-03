ALTER TABLE materiais
    ADD COLUMN IF NOT EXISTS custo_medio numeric(15, 4) NOT NULL DEFAULT 0;

ALTER TABLE materiais
    ADD CONSTRAINT chk_materiais_custo_medio_nao_negativo
    CHECK (custo_medio >= 0);
