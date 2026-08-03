ALTER TABLE faturamentos
    ADD COLUMN IF NOT EXISTS ordem_servico_id bigint;

CREATE INDEX IF NOT EXISTS idx_faturamentos_ordem_servico
    ON faturamentos (ordem_servico_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_faturamentos_ordem_servico'
    ) THEN
        ALTER TABLE faturamentos
            ADD CONSTRAINT fk_faturamentos_ordem_servico
            FOREIGN KEY (ordem_servico_id)
            REFERENCES ordens_servico (id);
    END IF;
END $$;
