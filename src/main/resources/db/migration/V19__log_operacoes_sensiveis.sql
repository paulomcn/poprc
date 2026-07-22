CREATE TABLE IF NOT EXISTS logs_operacoes_sensiveis (
    id BIGSERIAL PRIMARY KEY,
    funcionario_id BIGINT,
    usuario VARCHAR(160) NOT NULL,
    perfil VARCHAR(40) NOT NULL,
    metodo_http VARCHAR(10) NOT NULL,
    caminho VARCHAR(500) NOT NULL,
    status_http INTEGER NOT NULL,
    endereco_ip VARCHAR(80),
    registrado_em TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_logs_operacoes_sensiveis_funcionario_data
    ON logs_operacoes_sensiveis (funcionario_id, registrado_em);

CREATE OR REPLACE FUNCTION bloquear_alteracao_logs_operacoes_sensiveis()
RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'O log de operações sensíveis é append-only.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_logs_operacoes_sensiveis_append_only
BEFORE UPDATE OR DELETE ON logs_operacoes_sensiveis
FOR EACH ROW EXECUTE FUNCTION bloquear_alteracao_logs_operacoes_sensiveis();
