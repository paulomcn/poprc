CREATE TABLE reconciliacoes_retiradas_planilha (
    id BIGSERIAL PRIMARY KEY,
    retirada_importada_id BIGINT NOT NULL REFERENCES importacoes_retiradas_planilha(id),
    material_id BIGINT NOT NULL REFERENCES materiais(id),
    aba_origem VARCHAR(255) NOT NULL,
    nome_arquivo VARCHAR(255) NOT NULL,
    hash_origem VARCHAR(64) NOT NULL,
    quantidade_anterior NUMERIC(14, 3) NOT NULL,
    quantidade_nova NUMERIC(14, 3) NOT NULL,
    saldo_inicial_anterior NUMERIC(14, 3) NOT NULL,
    saldo_inicial_novo NUMERIC(14, 3) NOT NULL,
    saldo_final_anterior NUMERIC(14, 3) NOT NULL,
    saldo_final_novo NUMERIC(14, 3) NOT NULL,
    falta_anterior NUMERIC(14, 3) NOT NULL,
    falta_nova NUMERIC(14, 3) NOT NULL,
    data_retirada_anterior DATE,
    data_retirada_nova DATE,
    reconciliado_por VARCHAR(255) NOT NULL,
    reconciliado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_reconciliacao_retirada_hash UNIQUE (retirada_importada_id, hash_origem)
);

CREATE INDEX idx_reconciliacao_retirada_data
    ON reconciliacoes_retiradas_planilha (reconciliado_em DESC);

CREATE OR REPLACE FUNCTION impedir_alteracao_reconciliacao_retirada()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Registros de reconciliação de retiradas são imutáveis';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_reconciliacao_retirada_append_only
BEFORE UPDATE OR DELETE ON reconciliacoes_retiradas_planilha
FOR EACH ROW EXECUTE FUNCTION impedir_alteracao_reconciliacao_retirada();
