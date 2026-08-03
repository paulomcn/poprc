ALTER TABLE movimentacoes_estoque
    ADD COLUMN IF NOT EXISTS custo_unitario numeric(15, 4) NOT NULL DEFAULT 0;

ALTER TABLE movimentacoes_estoque
    ADD COLUMN IF NOT EXISTS valor_total_movimentacao numeric(19, 4) NOT NULL DEFAULT 0;

ALTER TABLE movimentacoes_estoque
    ADD COLUMN IF NOT EXISTS custo_estimado boolean NOT NULL DEFAULT false;

UPDATE movimentacoes_estoque movimentacao
SET custo_unitario = COALESCE(material.custo_medio, 0),
    valor_total_movimentacao = ROUND(
        COALESCE(material.custo_medio, 0) *
        CASE
            WHEN material.tipo_controle IN ('METRAGEM', 'BOBINA', 'ROLO')
                THEN COALESCE(movimentacao.metragem, 0)
            ELSE COALESCE(movimentacao.quantidade, 0)
        END,
        4
    ),
    custo_estimado = true
FROM materiais material
WHERE movimentacao.material_id = material.id
  AND movimentacao.tipo IN ('RETIRADA_OR', 'DEVOLUCAO_OR');

ALTER TABLE movimentacoes_estoque
    ADD CONSTRAINT chk_movimentacao_custo_unitario_nao_negativo
    CHECK (custo_unitario >= 0);

ALTER TABLE movimentacoes_estoque
    ADD CONSTRAINT chk_movimentacao_valor_total_nao_negativo
    CHECK (valor_total_movimentacao >= 0);
