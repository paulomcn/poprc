ALTER TABLE contratos
    ADD COLUMN IF NOT EXISTS tipo_contratante VARCHAR(30);

UPDATE contratos
SET tipo_contratante = 'SETOR_PUBLICO'
WHERE tipo_contratante IS NULL;

ALTER TABLE contratos
    ALTER COLUMN tipo_contratante SET DEFAULT 'SETOR_PUBLICO',
    ALTER COLUMN tipo_contratante SET NOT NULL;

ALTER TABLE contratos
    DROP CONSTRAINT IF EXISTS contratos_tipo_contratante_check;

ALTER TABLE contratos
    ADD CONSTRAINT contratos_tipo_contratante_check
        CHECK (tipo_contratante IN ('SETOR_PUBLICO', 'SETOR_PRIVADO'));

ALTER TABLE faturamentos
    ALTER COLUMN servicos_executados DROP NOT NULL,
    ADD COLUMN IF NOT EXISTS data_emissao DATE,
    ADD COLUMN IF NOT EXISTS data_pagamento DATE,
    ADD COLUMN IF NOT EXISTS competencia_fiscal DATE,
    ADD COLUMN IF NOT EXISTS aliquota_imposto_retido NUMERIC(7, 6),
    ADD COLUMN IF NOT EXISTS aliquota_imposto_pagar NUMERIC(7, 6),
    ADD COLUMN IF NOT EXISTS imposto_retido NUMERIC(19, 2),
    ADD COLUMN IF NOT EXISTS imposto_pagar NUMERIC(19, 2),
    ADD COLUMN IF NOT EXISTS imposto_total NUMERIC(19, 2);

UPDATE faturamentos
SET data_emissao = COALESCE(data_emissao, data_vencimento, CURRENT_DATE),
    competencia_fiscal = COALESCE(
        competencia_fiscal,
        (date_trunc('month', COALESCE(data_emissao, data_vencimento, CURRENT_DATE))
            + interval '1 month 19 days')::date),
    aliquota_imposto_retido = COALESCE(aliquota_imposto_retido, 0.048000),
    aliquota_imposto_pagar = COALESCE(aliquota_imposto_pagar, 0.149300),
    imposto_retido = COALESCE(imposto_retido, ROUND(valor_medicao * 0.048000, 2)),
    imposto_pagar = COALESCE(imposto_pagar, ROUND(valor_medicao * 0.149300, 2)),
    imposto_total = COALESCE(
        imposto_total,
        ROUND(valor_medicao * 0.048000, 2) + ROUND(valor_medicao * 0.149300, 2))
WHERE numero_nota_fiscal IS NOT NULL;

WITH nfs_repetidas AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY contrato_id, UPPER(TRIM(numero_nota_fiscal))
               ORDER BY id
           ) AS ordem
    FROM faturamentos
    WHERE numero_nota_fiscal IS NOT NULL
)
UPDATE faturamentos f
SET numero_nota_fiscal = f.numero_nota_fiscal || '-LEGADO-' || f.id
FROM nfs_repetidas repetida
WHERE f.id = repetida.id
  AND repetida.ordem > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uk_faturamentos_contrato_nf
    ON faturamentos (contrato_id, UPPER(TRIM(numero_nota_fiscal)))
    WHERE numero_nota_fiscal IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_faturamentos_data_emissao
    ON faturamentos(data_emissao);

CREATE INDEX IF NOT EXISTS idx_faturamentos_competencia_fiscal
    ON faturamentos(competencia_fiscal);
