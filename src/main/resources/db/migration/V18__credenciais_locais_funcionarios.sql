ALTER TABLE funcionarios ADD COLUMN IF NOT EXISTS cpf VARCHAR(11);
ALTER TABLE funcionarios ADD COLUMN IF NOT EXISTS telefone VARCHAR(30);
ALTER TABLE funcionarios ADD COLUMN IF NOT EXISTS senha_hash VARCHAR(100);
ALTER TABLE funcionarios ADD COLUMN IF NOT EXISTS troca_senha_obrigatoria BOOLEAN;
ALTER TABLE funcionarios ADD COLUMN IF NOT EXISTS tentativas_login INTEGER;
ALTER TABLE funcionarios ADD COLUMN IF NOT EXISTS bloqueado_ate TIMESTAMP;

UPDATE funcionarios SET troca_senha_obrigatoria = FALSE WHERE troca_senha_obrigatoria IS NULL;
UPDATE funcionarios SET tentativas_login = 0 WHERE tentativas_login IS NULL;

ALTER TABLE funcionarios ALTER COLUMN troca_senha_obrigatoria SET DEFAULT FALSE;
ALTER TABLE funcionarios ALTER COLUMN troca_senha_obrigatoria SET NOT NULL;
ALTER TABLE funcionarios ALTER COLUMN tentativas_login SET DEFAULT 0;
ALTER TABLE funcionarios ALTER COLUMN tentativas_login SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_funcionarios_cpf
    ON funcionarios (cpf)
    WHERE cpf IS NOT NULL;
