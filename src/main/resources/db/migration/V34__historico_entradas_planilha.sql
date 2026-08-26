ALTER TABLE importacoes_estoque_planilha
    ADD COLUMN entradas_importadas INTEGER NOT NULL DEFAULT 0;

CREATE TABLE importacoes_entradas_planilha (
    id BIGSERIAL PRIMARY KEY,
    importacao_id BIGINT NOT NULL REFERENCES importacoes_estoque_planilha(id) ON DELETE CASCADE,
    material_id BIGINT NOT NULL REFERENCES materiais(id),
    chave_evento VARCHAR(36) NOT NULL UNIQUE,
    tipo_entrada VARCHAR(30) NOT NULL,
    cabecalho_origem VARCHAR(255) NOT NULL,
    fornecedor VARCHAR(255),
    data_entrada DATE,
    quantidade NUMERIC(14, 3) NOT NULL DEFAULT 0,
    custo_unitario NUMERIC(19, 2) NOT NULL DEFAULT 0,
    linha_origem INTEGER,
    coluna_origem INTEGER
);

CREATE INDEX idx_importacao_entrada_importacao
    ON importacoes_entradas_planilha (importacao_id);
