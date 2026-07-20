# Resultado da Homologacao Operacional - 20/07/2026

**Status:** CONCLUIDA. Este arquivo e a evidencia da execucao, nao um roteiro pendente.

## Escopo

Homologacao executada em banco PostgreSQL temporario, criado exclusivamente para o teste, com a massa de desenvolvimento versionada no projeto.

Fluxo validado:

`Contrato -> Projeto -> OS -> OR -> Retirada -> Tecnico -> Devolucao -> Auditoria -> As-Built -> Documento final -> Encerramento -> Faturamento`

## Resultado

- Contrato, projeto, comarca, equipe, deposito e estoque carregados pela massa de desenvolvimento.
- OS `DEV-CONTRATO-001 - OS 01` criada com materiais previstos.
- OR `DEV-CONTRATO-001 - OS 01 - OR 01` criada automaticamente.
- Retirada sem a segunda assinatura bloqueada com HTTP 400.
- Retirada valida concluida com dupla assinatura.
- Checklist tecnico, foto de evidencia, vistoria, assinatura e prova da virada de rede persistidos.
- Devolucao sem retorno da ferramenta bloqueada com HTTP 400.
- Devolucao valida concluida, incluindo ferramenta, consumiveis e saldo de bobina.
- Estoque final conciliado: 92 unidades do consumivel, 5 ferramentas e 285 metros da bobina.
- Auditoria registrada com consumo liquido de 8 unidades, 0 ferramentas e 20 metros da bobina.
- As-Built homologado com divergencia devido a sobra devolvida ao estoque.
- Documento final registrado com tres assinaturas, tres eventos de log e PDF de 40.283 bytes.
- Integridade do documento confirmada: hash calculado igual ao hash registrado.
- Obra e projeto concluidos, OR devolvida e OS faturada.
- Exclusao de evidencia depois da conclusao bloqueada com HTTP 400.

## Defeito Encontrado E Corrigido

A validacao de assinatura aceitava qualquer conteudo que comecasse com o cabecalho de PNG. O teste de ponta a ponta revelou que esse arquivo falhava apenas durante a geracao do PDF.

A API agora decodifica a imagem com `ImageIO` antes de persistir a assinatura. Um teste automatizado cobre especificamente um cabecalho PNG com conteudo corrompido. O documento criado durante a tentativa invalida foi invalidado, e a nova versao valida manteve o vinculo de historico.

## Criterio De Aprovacao

O fluxo principal foi aprovado funcionalmente. A autenticacao das rotas `/api/**` permanece fora deste ciclo, conforme decisao anterior de deixa-la para a etapa final de seguranca.

Em 20/07/2026, a mesma cobertura foi executada novamente pelo perfil `test` no
banco descartavel `poprc_test`: 75 testes passaram, as 15 migracoes foram
aplicadas desde um schema vazio e a base foi removida automaticamente ao final.

## Homologacao de interface

As telas Dashboard Executivo, Gestao de Obras, Ordens de Servico, Area do
Tecnico e Auditoria de Retirada/Devolucao foram verificadas em desktop (1280 x
720) e celular (390 x 844), com dados reais da API local.

Resultados:

- Dashboard carregou indicadores, filtros, alertas e acessos rapidos sem erros.
- Sidebar mobile abriu e fechou completamente, sem manter overlay ativo.
- Area do Tecnico exibiu equipe, alertas, prazos e OS atribuidas sem overflow.
- Ordens de Servico carregou as colunas depois do debounce e manteve os cards
  acessiveis no celular.
- Gestao de Obras teve o grupo de filtros ajustado para nao cortar o contador
  nem criar rolagem horizontal global no celular.
- Auditoria teve o seletor de OS ajustado para respeitar a largura disponivel;
  tabelas extensas mantem somente a rolagem horizontal local esperada.
- As duas correcoes responsivas foram verificadas novamente em desktop sem
  regressao de largura ou layout.
