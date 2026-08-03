ALTER TABLE funcionarios ADD COLUMN IF NOT EXISTS email VARCHAR(255);
ALTER TABLE funcionarios ADD COLUMN IF NOT EXISTS perfil_acesso VARCHAR(40);
ALTER TABLE funcionarios ADD COLUMN IF NOT EXISTS ativo BOOLEAN;

UPDATE funcionarios
SET perfil_acesso = CASE
    WHEN id = (SELECT MIN(id) FROM funcionarios) THEN 'ADMIN'
    WHEN LOWER(COALESCE(funcao, '')) LIKE '%estoque%' THEN 'ESTOQUE'
    WHEN LOWER(COALESCE(funcao, '')) LIKE '%auditor%' THEN 'AUDITOR'
    WHEN LOWER(COALESCE(funcao, '')) LIKE '%supervisor%'
      OR LOWER(COALESCE(funcao, '')) LIKE '%lider%'
      OR LOWER(COALESCE(funcao, '')) LIKE '%gerente%' THEN 'SUPERVISOR_TECNICO'
    ELSE 'TECNICO'
END
WHERE perfil_acesso IS NULL;

UPDATE funcionarios SET ativo = TRUE WHERE ativo IS NULL;

ALTER TABLE funcionarios ALTER COLUMN perfil_acesso SET DEFAULT 'TECNICO';
ALTER TABLE funcionarios ALTER COLUMN perfil_acesso SET NOT NULL;
ALTER TABLE funcionarios ALTER COLUMN ativo SET DEFAULT TRUE;
ALTER TABLE funcionarios ALTER COLUMN ativo SET NOT NULL;

ALTER TABLE funcionarios DROP CONSTRAINT IF EXISTS funcionarios_perfil_acesso_check;
ALTER TABLE funcionarios ADD CONSTRAINT funcionarios_perfil_acesso_check CHECK (
    perfil_acesso IN ('ADMIN', 'SUPERVISOR_TECNICO', 'TECNICO', 'ESTOQUE', 'AUDITOR')
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_funcionarios_email_normalizado
    ON funcionarios (LOWER(email))
    WHERE email IS NOT NULL;
