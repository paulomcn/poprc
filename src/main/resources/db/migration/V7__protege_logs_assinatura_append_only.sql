CREATE OR REPLACE FUNCTION bloquear_alteracao_log_assinatura()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION
        'documentos_assinaturas_log e append-only: UPDATE e DELETE nao sao permitidos';
END;
$$;

DROP TRIGGER IF EXISTS trg_documentos_assinaturas_log_append_only
    ON documentos_assinaturas_log;

CREATE TRIGGER trg_documentos_assinaturas_log_append_only
BEFORE UPDATE OR DELETE ON documentos_assinaturas_log
FOR EACH ROW
EXECUTE FUNCTION bloquear_alteracao_log_assinatura();
