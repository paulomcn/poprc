ALTER TABLE importacoes_estoque_planilha
    ALTER COLUMN local_estoque_id DROP NOT NULL;

ALTER TABLE importacoes_estoque_planilha
    ADD COLUMN tipo_importacao VARCHAR(40) NOT NULL DEFAULT 'INVENTARIO_COMPLETO';

ALTER TABLE importacoes_estoque_planilha_itens
    ADD COLUMN custo_anterior NUMERIC(19, 4);

UPDATE importacoes_estoque_planilha_itens
SET custo_anterior = custo_unitario
WHERE custo_anterior IS NULL;

ALTER TABLE importacoes_estoque_planilha_itens
    ALTER COLUMN custo_anterior SET NOT NULL;

ALTER TABLE importacoes_estoque_planilha_itens
    ADD COLUMN linha_origem INTEGER;
