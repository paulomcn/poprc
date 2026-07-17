ALTER TABLE contratos
    ADD COLUMN IF NOT EXISTS arquivado BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS arquivado_em TIMESTAMP,
    ADD COLUMN IF NOT EXISTS arquivado_por VARCHAR(255),
    ADD COLUMN IF NOT EXISTS motivo_arquivamento VARCHAR(1000);

ALTER TABLE projetos
    ADD COLUMN IF NOT EXISTS arquivado BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS arquivado_em TIMESTAMP,
    ADD COLUMN IF NOT EXISTS arquivado_por VARCHAR(255),
    ADD COLUMN IF NOT EXISTS motivo_arquivamento VARCHAR(1000);

ALTER TABLE comarcas
    ADD COLUMN IF NOT EXISTS arquivado BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS arquivado_em TIMESTAMP,
    ADD COLUMN IF NOT EXISTS arquivado_por VARCHAR(255),
    ADD COLUMN IF NOT EXISTS motivo_arquivamento VARCHAR(1000);

ALTER TABLE ordens_servico
    ADD COLUMN IF NOT EXISTS arquivado BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS arquivado_em TIMESTAMP,
    ADD COLUMN IF NOT EXISTS arquivado_por VARCHAR(255),
    ADD COLUMN IF NOT EXISTS motivo_arquivamento VARCHAR(1000);

CREATE INDEX IF NOT EXISTS idx_contratos_arquivado ON contratos(arquivado);
CREATE INDEX IF NOT EXISTS idx_projetos_arquivado ON projetos(arquivado);
CREATE INDEX IF NOT EXISTS idx_comarcas_arquivado ON comarcas(arquivado);
CREATE INDEX IF NOT EXISTS idx_ordens_servico_arquivado ON ordens_servico(arquivado);
