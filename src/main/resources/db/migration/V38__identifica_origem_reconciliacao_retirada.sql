ALTER TABLE reconciliacoes_retiradas_planilha
    ADD COLUMN origem VARCHAR(30) NOT NULL DEFAULT 'PLANILHA',
    ADD COLUMN motivo VARCHAR(500) NOT NULL DEFAULT 'Reconciliação confirmada pela planilha de origem';

ALTER TABLE reconciliacoes_retiradas_planilha
    ALTER COLUMN origem DROP DEFAULT,
    ALTER COLUMN motivo DROP DEFAULT;
