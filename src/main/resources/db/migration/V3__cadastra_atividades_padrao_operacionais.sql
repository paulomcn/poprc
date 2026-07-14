INSERT INTO atividades_padrao (nome, categoria, ativo, ordem_exibicao, criado_em)
SELECT 'Conferir o escopo da OS com o responsavel local', 'Vistoria', TRUE, 10, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM atividades_padrao WHERE LOWER(nome) = LOWER('Conferir o escopo da OS com o responsavel local'));

INSERT INTO atividades_padrao (nome, categoria, ativo, ordem_exibicao, criado_em)
SELECT 'Registrar as condicoes iniciais e evidencias fotograficas', 'Vistoria', TRUE, 20, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM atividades_padrao WHERE LOWER(nome) = LOWER('Registrar as condicoes iniciais e evidencias fotograficas'));

INSERT INTO atividades_padrao (nome, categoria, ativo, ordem_exibicao, criado_em)
SELECT 'Sinalizar e proteger a area de trabalho', 'Infraestrutura', TRUE, 30, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM atividades_padrao WHERE LOWER(nome) = LOWER('Sinalizar e proteger a area de trabalho'));

INSERT INTO atividades_padrao (nome, categoria, ativo, ordem_exibicao, criado_em)
SELECT 'Instalar eletrodutos, canaletas ou infraestrutura prevista', 'Infraestrutura', TRUE, 40, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM atividades_padrao WHERE LOWER(nome) = LOWER('Instalar eletrodutos, canaletas ou infraestrutura prevista'));

INSERT INTO atividades_padrao (nome, categoria, ativo, ordem_exibicao, criado_em)
SELECT 'Executar a passagem e organizacao dos cabos', 'Infraestrutura', TRUE, 50, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM atividades_padrao WHERE LOWER(nome) = LOWER('Executar a passagem e organizacao dos cabos'));

INSERT INTO atividades_padrao (nome, categoria, ativo, ordem_exibicao, criado_em)
SELECT 'Identificar cabos, pontos e equipamentos', 'Infraestrutura', TRUE, 60, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM atividades_padrao WHERE LOWER(nome) = LOWER('Identificar cabos, pontos e equipamentos'));

INSERT INTO atividades_padrao (nome, categoria, ativo, ordem_exibicao, criado_em)
SELECT 'Instalar e conectar os equipamentos previstos', 'Infraestrutura', TRUE, 70, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM atividades_padrao WHERE LOWER(nome) = LOWER('Instalar e conectar os equipamentos previstos'));

INSERT INTO atividades_padrao (nome, categoria, ativo, ordem_exibicao, criado_em)
SELECT 'Testar a alimentacao eletrica e o funcionamento dos equipamentos', 'Validacao', TRUE, 80, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM atividades_padrao WHERE LOWER(nome) = LOWER('Testar a alimentacao eletrica e o funcionamento dos equipamentos'));

INSERT INTO atividades_padrao (nome, categoria, ativo, ordem_exibicao, criado_em)
SELECT 'Testar conectividade, comunicacao e acesso a rede', 'Validacao', TRUE, 90, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM atividades_padrao WHERE LOWER(nome) = LOWER('Testar conectividade, comunicacao e acesso a rede'));

INSERT INTO atividades_padrao (nome, categoria, ativo, ordem_exibicao, criado_em)
SELECT 'Registrar evidencias fotograficas apos a execucao', 'Validacao', TRUE, 100, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM atividades_padrao WHERE LOWER(nome) = LOWER('Registrar evidencias fotograficas apos a execucao'));

INSERT INTO atividades_padrao (nome, categoria, ativo, ordem_exibicao, criado_em)
SELECT 'Limpar e organizar o ambiente de trabalho', 'Encerramento', TRUE, 110, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM atividades_padrao WHERE LOWER(nome) = LOWER('Limpar e organizar o ambiente de trabalho'));

INSERT INTO atividades_padrao (nome, categoria, ativo, ordem_exibicao, criado_em)
SELECT 'Apresentar o servico executado ao responsavel local', 'Encerramento', TRUE, 120, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM atividades_padrao WHERE LOWER(nome) = LOWER('Apresentar o servico executado ao responsavel local'));

INSERT INTO atividades_padrao (nome, categoria, ativo, ordem_exibicao, criado_em)
SELECT 'Registrar pendencias, desvios ou materiais faltantes', 'Encerramento', TRUE, 130, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM atividades_padrao WHERE LOWER(nome) = LOWER('Registrar pendencias, desvios ou materiais faltantes'));
