ALTER TABLE materiais
    DROP CONSTRAINT IF EXISTS materiais_tipo_controle_check;

ALTER TABLE materiais
    ADD CONSTRAINT materiais_tipo_controle_check
    CHECK (tipo_controle IN (
        'UNIDADE',
        'FRACIONADO',
        'PECA_COM_COMPRIMENTO',
        'METRAGEM',
        'ROLO',
        'BOBINA'
    ));
