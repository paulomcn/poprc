ALTER TABLE ordens_servico DROP CONSTRAINT IF EXISTS ordens_servico_status_check;
ALTER TABLE ordens_servico ADD CONSTRAINT ordens_servico_status_check CHECK (
    status IN (
        'ABERTA',
        'AGUARDANDO_VISTORIA',
        'AGUARDANDO_RETIRADA',
        'EM_EXECUCAO',
        'AGUARDANDO_VALIDACAO',
        'AGUARDANDO_DEVOLUCAO',
        'AGUARDANDO_AUDITORIA',
        'AGUARDANDO_ENCERRAMENTO',
        'CONCLUIDA',
        'FATURADA'
    )
);

CREATE TABLE historico_status_os (
    id BIGSERIAL PRIMARY KEY,
    ordem_servico_id BIGINT NOT NULL REFERENCES ordens_servico(id),
    status_anterior VARCHAR(40),
    status_novo VARCHAR(40) NOT NULL,
    evento VARCHAR(80) NOT NULL,
    responsavel VARCHAR(160) NOT NULL,
    registrado_em TIMESTAMP NOT NULL
);

CREATE INDEX idx_historico_status_os_ordem_data
    ON historico_status_os (ordem_servico_id, registrado_em, id);

INSERT INTO historico_status_os (
    ordem_servico_id, status_anterior, status_novo, evento, responsavel, registrado_em
)
SELECT id, NULL, COALESCE(status, 'ABERTA'), 'MIGRACAO_ESTADO_INICIAL', 'Sistema', CURRENT_TIMESTAMP
FROM ordens_servico;

CREATE OR REPLACE FUNCTION bloquear_alteracao_historico_status_os()
RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'O histórico de status da OS é append-only e não pode ser alterado.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_historico_status_os_append_only
BEFORE UPDATE OR DELETE ON historico_status_os
FOR EACH ROW EXECUTE FUNCTION bloquear_alteracao_historico_status_os();
