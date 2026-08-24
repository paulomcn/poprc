CREATE TABLE importacoes_notas_fiscais (
    id BIGSERIAL PRIMARY KEY,
    nome_arquivo VARCHAR(255) NOT NULL,
    arquivo_path VARCHAR(1000) NOT NULL,
    hash_sha256 VARCHAR(64) NOT NULL UNIQUE,
    tipo_arquivo VARCHAR(20) NOT NULL,
    chave_acesso VARCHAR(60),
    numero VARCHAR(80),
    serie VARCHAR(30),
    emitente_nome VARCHAR(255),
    emitente_cnpj VARCHAR(20),
    data_emissao TIMESTAMP,
    valor_total NUMERIC(19, 4) NOT NULL DEFAULT 0,
    data_importacao TIMESTAMP NOT NULL,
    importado_por VARCHAR(255) NOT NULL,
    funcionario_id BIGINT NOT NULL REFERENCES funcionarios(id),
    local_estoque_id BIGINT NOT NULL REFERENCES locais_estoque(id),
    itens_processados INTEGER NOT NULL DEFAULT 0,
    materiais_criados INTEGER NOT NULL DEFAULT 0,
    materiais_existentes INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE importacoes_notas_fiscais_itens (
    id BIGSERIAL PRIMARY KEY,
    importacao_id BIGINT NOT NULL REFERENCES importacoes_notas_fiscais(id) ON DELETE CASCADE,
    material_id BIGINT NOT NULL REFERENCES materiais(id),
    codigo_produto VARCHAR(255),
    descricao TEXT NOT NULL,
    ncm VARCHAR(20),
    cfop VARCHAR(20),
    unidade_fiscal VARCHAR(20),
    quantidade NUMERIC(19, 4) NOT NULL,
    valor_unitario NUMERIC(19, 4) NOT NULL,
    valor_total NUMERIC(19, 4) NOT NULL,
    acao VARCHAR(30) NOT NULL
);

CREATE INDEX idx_importacao_nf_data ON importacoes_notas_fiscais(data_importacao DESC);
CREATE INDEX idx_importacao_nf_item_material ON importacoes_notas_fiscais_itens(material_id);
