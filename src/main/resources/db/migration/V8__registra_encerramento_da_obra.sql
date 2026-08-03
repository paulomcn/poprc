ALTER TABLE comarcas
    ADD COLUMN IF NOT EXISTS data_conclusao timestamp without time zone,
    ADD COLUMN IF NOT EXISTS concluida_por character varying(255);
