-- Massa pequena, deterministica e idempotente. Este arquivo so e carregado
-- quando o perfil dev adiciona classpath:db/dev as locations do Flyway.

INSERT INTO funcionarios (nome, funcao, cidade)
SELECT 'Tecnico Desenvolvimento', 'Tecnico', 'Joao Pessoa'
WHERE NOT EXISTS (
    SELECT 1 FROM funcionarios WHERE nome = 'Tecnico Desenvolvimento'
);

INSERT INTO funcionarios (nome, funcao, cidade)
SELECT 'Gestor Desenvolvimento', 'Gestor', 'Joao Pessoa'
WHERE NOT EXISTS (
    SELECT 1 FROM funcionarios WHERE nome = 'Gestor Desenvolvimento'
);

INSERT INTO contratos (
    cliente, contrato, escopo, vigencia_inicio, vigencia_fim,
    gestor_contrato_id, fiscal_tecnico_id, status
)
SELECT
    'Cliente Desenvolvimento',
    'DEV-CONTRATO-001',
    'Contrato reproduzivel para testes locais',
    CURRENT_DATE,
    CURRENT_DATE + 365,
    gestor.id,
    tecnico.id,
    'ATIVO'
FROM funcionarios gestor
CROSS JOIN funcionarios tecnico
WHERE gestor.nome = 'Gestor Desenvolvimento'
  AND tecnico.nome = 'Tecnico Desenvolvimento'
  AND NOT EXISTS (
      SELECT 1 FROM contratos WHERE contrato = 'DEV-CONTRATO-001'
  );
INSERT INTO projetos (
    data_inicio, data_fim, status, contrato_id, responsavel_id, as_built_status
)
SELECT
    CURRENT_DATE,
    CURRENT_DATE + 90,
    'EM_ANDAMENTO',
    contrato.id,
    tecnico.id,
    'PENDENTE'
FROM contratos contrato
CROSS JOIN funcionarios tecnico
WHERE contrato.contrato = 'DEV-CONTRATO-001'
  AND tecnico.nome = 'Tecnico Desenvolvimento'
  AND NOT EXISTS (
      SELECT 1 FROM projetos WHERE contrato_id = contrato.id
  );

INSERT INTO comarcas (
    nome_comarca, endereco, contato_local, quantidade_pontos, situacao,
    percentual_concluido, projeto_id, etapa_atual, as_built_status,
    faltou_material, virada_rede_concluida
)
SELECT
    'Obra Desenvolvimento',
    'Endereco de homologacao local',
    'Contato Desenvolvimento',
    12,
    'EM_ANDAMENTO',
    0,
    projeto.id,
    1,
    'PENDENTE',
    false,
    false
FROM projetos projeto
JOIN contratos contrato ON contrato.id = projeto.contrato_id
WHERE contrato.contrato = 'DEV-CONTRATO-001'
  AND NOT EXISTS (
      SELECT 1 FROM comarcas WHERE nome_comarca = 'Obra Desenvolvimento'
  );

INSERT INTO locais_estoque (nome, endereco, ativo)
VALUES ('Deposito Desenvolvimento', 'Endereco de homologacao local', true)
ON CONFLICT (nome) DO NOTHING;

INSERT INTO materiais (
    nome, part_number, fabricante, fornecedor, categoria, descricao,
    quantidade_disponivel, quantidade_reservada, tipo_controle,
    unidade_medida, metragem_disponivel, metragem_reservada,
    estoque_minimo, localizacao
)
VALUES
    ('Cabo de Rede DEV', 'DEV-CABO-001', 'Fabricante DEV', 'Fornecedor DEV',
     'MATERIAL_CONSUMO', 'Material de consumo para homologacao local',
     100, 0, 'UNIDADE', 'UNIDADE', 0, 0, 10, 'Deposito Desenvolvimento'),
    ('Alicate DEV', 'DEV-ALICATE-001', 'Fabricante DEV', 'Fornecedor DEV',
     'FERRAMENTA', 'Ferramenta com retorno obrigatorio',
     5, 0, 'UNIDADE', 'UNIDADE', 0, 0, 1, 'Deposito Desenvolvimento'),
    ('Bobina de Fibra DEV', 'DEV-BOBINA-001', 'Fabricante DEV', 'Fornecedor DEV',
     'MATERIAL_CONSUMO', 'Bobina rastreavel para testes de metragem',
     0, 0, 'BOBINA', 'METRO', 305, 0, 30, 'Deposito Desenvolvimento')
ON CONFLICT (part_number) DO NOTHING;

INSERT INTO saldos_materiais_locais (
    material_id, local_estoque_id, quantidade_disponivel,
    quantidade_reservada, metragem_disponivel, metragem_reservada,
    estoque_minimo
)
SELECT
    material.id,
    local.id,
    CASE material.part_number
        WHEN 'DEV-CABO-001' THEN 100
        WHEN 'DEV-ALICATE-001' THEN 5
        ELSE 0
    END,
    0,
    CASE material.part_number WHEN 'DEV-BOBINA-001' THEN 305 ELSE 0 END,
    0,
    CASE material.part_number
        WHEN 'DEV-CABO-001' THEN 10
        WHEN 'DEV-ALICATE-001' THEN 1
        ELSE 30
    END
FROM materiais material
CROSS JOIN locais_estoque local
WHERE material.part_number IN ('DEV-CABO-001', 'DEV-ALICATE-001', 'DEV-BOBINA-001')
  AND local.nome = 'Deposito Desenvolvimento'
  AND NOT EXISTS (
      SELECT 1
      FROM saldos_materiais_locais saldo
      WHERE saldo.material_id = material.id
        AND saldo.local_estoque_id = local.id
  );

INSERT INTO unidades_estoque_rastreaveis (
    codigo, data_entrada, metragem_inicial, metragem_atual,
    observacao, status, tipo, material_id, local_estoque_id
)
SELECT
    'DEV-BOBINA-RASTREAVEL-001',
    CURRENT_TIMESTAMP,
    305,
    305,
    'Unidade reproduzivel do perfil dev',
    'LACRADA',
    'BOBINA',
    material.id,
    local.id
FROM materiais material
CROSS JOIN locais_estoque local
WHERE material.part_number = 'DEV-BOBINA-001'
  AND local.nome = 'Deposito Desenvolvimento'
  AND NOT EXISTS (
      SELECT 1 FROM unidades_estoque_rastreaveis
      WHERE codigo = 'DEV-BOBINA-RASTREAVEL-001'
  );
