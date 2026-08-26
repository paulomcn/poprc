ALTER TABLE importacoes_estoque_planilha_itens
    ALTER COLUMN saldo_anterior SET DATA TYPE NUMERIC(14, 3),
    ALTER COLUMN saldo_importado SET DATA TYPE NUMERIC(14, 3);

ALTER TABLE importacoes_estoque_planilha
    ADD COLUMN saldo_consolidado BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN contrato_id BIGINT REFERENCES contratos(id),
    ADD COLUMN responsavel_id BIGINT REFERENCES funcionarios(id),
    ADD COLUMN projetos_criados INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN ordens_servico_criadas INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN ordens_retirada_criadas INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN retornos_importados INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN simulacao_faltas INTEGER NOT NULL DEFAULT 0;

ALTER TABLE ordens_servico
    ADD COLUMN titulo VARCHAR(255);

ALTER TABLE importacoes_retiradas_planilha
    ADD COLUMN ordem_retirada_id BIGINT REFERENCES ordens_retirada(id);

CREATE TABLE importacoes_retornos_planilha (
    id BIGSERIAL PRIMARY KEY,
    importacao_id BIGINT NOT NULL REFERENCES importacoes_estoque_planilha(id) ON DELETE CASCADE,
    comarca_id BIGINT NOT NULL REFERENCES comarcas(id),
    material_id BIGINT NOT NULL REFERENCES materiais(id),
    aba_origem VARCHAR(255) NOT NULL,
    quantidade_retornada NUMERIC(14, 3) NOT NULL DEFAULT 0
);

CREATE INDEX idx_importacao_retorno_importacao
    ON importacoes_retornos_planilha (importacao_id);
