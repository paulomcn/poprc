ALTER TABLE documentos_internos
    ADD COLUMN IF NOT EXISTS invalidado_em TIMESTAMP,
    ADD COLUMN IF NOT EXISTS invalidado_por VARCHAR(255),
    ADD COLUMN IF NOT EXISTS motivo_invalidacao TEXT,
    ADD COLUMN IF NOT EXISTS documento_origem_id BIGINT;

ALTER TABLE documentos_internos
    ADD CONSTRAINT fk_documento_interno_origem
    FOREIGN KEY (documento_origem_id) REFERENCES documentos_internos(id);

CREATE INDEX IF NOT EXISTS idx_documentos_internos_origem
    ON documentos_internos(documento_origem_id);
