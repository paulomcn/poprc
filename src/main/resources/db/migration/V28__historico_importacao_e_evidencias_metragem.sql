ALTER TABLE importacoes_estoque_planilha
    ADD COLUMN abas_retirada_processadas INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN retiradas_importadas INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN faltas_identificadas INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN data_complementacao TIMESTAMP,
    ADD COLUMN complementado_por VARCHAR(255);

CREATE TABLE importacoes_estoque_planilha_itens (
    id BIGSERIAL PRIMARY KEY,
    importacao_id BIGINT NOT NULL REFERENCES importacoes_estoque_planilha(id) ON DELETE CASCADE,
    material_id BIGINT NOT NULL REFERENCES materiais(id),
    nome_planilha VARCHAR(255) NOT NULL,
    saldo_anterior INTEGER NOT NULL,
    saldo_importado INTEGER NOT NULL,
    custo_unitario NUMERIC(19, 4) NOT NULL DEFAULT 0,
    acao VARCHAR(30) NOT NULL
);

CREATE INDEX idx_importacao_estoque_item_importacao
    ON importacoes_estoque_planilha_itens (importacao_id);

CREATE TABLE importacoes_retiradas_planilha (
    id BIGSERIAL PRIMARY KEY,
    importacao_id BIGINT NOT NULL REFERENCES importacoes_estoque_planilha(id) ON DELETE CASCADE,
    comarca_id BIGINT NOT NULL REFERENCES comarcas(id),
    material_id BIGINT NOT NULL REFERENCES materiais(id),
    aba_origem VARCHAR(255) NOT NULL,
    saldo_inicial NUMERIC(14, 3) NOT NULL DEFAULT 0,
    quantidade_retirada NUMERIC(14, 3) NOT NULL DEFAULT 0,
    saldo_final NUMERIC(14, 3) NOT NULL DEFAULT 0,
    quantidade_faltante NUMERIC(14, 3) NOT NULL DEFAULT 0,
    custo_unitario NUMERIC(19, 4) NOT NULL DEFAULT 0,
    data_retirada DATE
);

CREATE INDEX idx_importacao_retirada_importacao
    ON importacoes_retiradas_planilha (importacao_id);

CREATE INDEX idx_importacao_retirada_comarca
    ON importacoes_retiradas_planilha (comarca_id);

ALTER TABLE ordem_retirada_alocacoes
    ADD COLUMN evidencia_retirada_path VARCHAR(500),
    ADD COLUMN evidencia_retirada_nome VARCHAR(255),
    ADD COLUMN evidencia_retirada_data TIMESTAMP,
    ADD COLUMN metragem_restante_apos_retirada NUMERIC(14, 3),
    ADD COLUMN evidencia_devolucao_path VARCHAR(500),
    ADD COLUMN evidencia_devolucao_nome VARCHAR(255),
    ADD COLUMN evidencia_devolucao_data TIMESTAMP,
    ADD COLUMN metragem_restante_apos_devolucao NUMERIC(14, 3);
