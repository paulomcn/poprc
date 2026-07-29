UPDATE projetos p
SET as_built_status = c.as_built_status
FROM comarcas c
WHERE c.projeto_id = p.id
  AND c.as_built_status IS NOT NULL
  AND c.as_built_status IS DISTINCT FROM p.as_built_status;
