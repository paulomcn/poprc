UPDATE comarcas
SET percentual_concluido = 90
WHERE virada_rede_concluida = TRUE
  AND situacao IS DISTINCT FROM 'CONCLUIDA'
  AND percentual_concluido > 90;
