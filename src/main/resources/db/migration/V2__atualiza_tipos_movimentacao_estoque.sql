ALTER TABLE public.movimentacoes_estoque
    DROP CONSTRAINT IF EXISTS movimentacoes_estoque_tipo_check;

ALTER TABLE public.movimentacoes_estoque
    ADD CONSTRAINT movimentacoes_estoque_tipo_check
    CHECK (tipo IN (
        'ENTRADA',
        'SAIDA',
        'RESERVA',
        'ESTORNO_RESERVA',
        'TRANSFERENCIA',
        'BAIXA',
        'ESTORNO_BAIXA',
        'RETIRADA_OR',
        'DEVOLUCAO_OR',
        'AJUSTE_POSITIVO',
        'AJUSTE_NEGATIVO'
    ));
