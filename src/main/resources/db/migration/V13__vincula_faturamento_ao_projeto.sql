ALTER TABLE faturamentos
    ADD COLUMN IF NOT EXISTS projeto_id BIGINT;

ALTER TABLE faturamentos
    DROP CONSTRAINT IF EXISTS fk_faturamentos_projeto;

ALTER TABLE faturamentos
    ADD CONSTRAINT fk_faturamentos_projeto
        FOREIGN KEY (projeto_id) REFERENCES projetos(id);

CREATE INDEX IF NOT EXISTS idx_faturamentos_projeto_id
    ON faturamentos(projeto_id);

UPDATE faturamentos f
SET projeto_id = unico.projeto_id
FROM (
    SELECT contrato_id, MIN(id) AS projeto_id
    FROM projetos
    WHERE COALESCE(arquivado, FALSE) = FALSE
    GROUP BY contrato_id
    HAVING COUNT(*) = 1
) unico
WHERE f.contrato_id = unico.contrato_id
  AND f.projeto_id IS NULL;
