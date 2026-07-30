CREATE TABLE importacoes_estoque_planilha (
    id BIGSERIAL PRIMARY KEY,
    nome_arquivo VARCHAR(255) NOT NULL,
    hash_sha256 VARCHAR(64) NOT NULL,
    data_importacao TIMESTAMP NOT NULL,
    importado_por VARCHAR(255) NOT NULL,
    local_estoque_id BIGINT NOT NULL REFERENCES locais_estoque(id),
    itens_processados INTEGER NOT NULL DEFAULT 0,
    materiais_criados INTEGER NOT NULL DEFAULT 0,
    materiais_atualizados INTEGER NOT NULL DEFAULT 0,
    ajustes_positivos INTEGER NOT NULL DEFAULT 0,
    ajustes_negativos INTEGER NOT NULL DEFAULT 0,
    valor_total_importado NUMERIC(19, 2) NOT NULL DEFAULT 0,
    CONSTRAINT uk_importacao_estoque_hash UNIQUE (hash_sha256)
);

CREATE INDEX idx_importacao_estoque_data
    ON importacoes_estoque_planilha (data_importacao DESC);
