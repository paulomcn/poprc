# Matriz de Permissoes e Escopo Operacional

Data: 27/07/2026

Status: BASE DA FASE DE AUTORIZACAO

## Perfis atuais

- ADMIN: administracao completa do sistema.
- SUPERVISOR_TECNICO: gestao do fluxo operacional e das equipes de campo.
- TECNICO: execucao de campo limitada aos projetos de sua equipe.
- ESTOQUE: entradas, retiradas, devolucoes e rastreabilidade de materiais.
- AUDITOR: conciliacao, homologacao e consulta de evidencias operacionais.

## Matriz por modulo

Legenda: C = consultar, I = incluir, A = alterar, X = arquivar ou excluir
logicamente, H = homologar ou aprovar.

| Modulo | ADMIN | SUPERVISOR_TECNICO | TECNICO | ESTOQUE | AUDITOR |
| --- | --- | --- | --- | --- | --- |
| Dashboard executivo | C | C | - | - | - |
| Contratos | C/I/A/X | C/I/A/X | - | - | - |
| Projetos e equipes | C/I/A/X | C/I/A/X | - | - | - |
| Funcionarios e acessos | C/I/A/X | C | - | C | - |
| Ordens de servico | C/I/A/X | C/I/A/X | C/A* | - | - |
| Gestao de obras | C/I/A/X | C/I/A/X | C/A* | C | C |
| Documentos da OS | C/I/A/X | C/I/A/X | C/I/A* | - | C |
| Estoque | C/I/A | - | - | C/I/A | - |
| Ordens de retirada | C/I/A | C | C* | C/I/A | C |
| Auditoria e As-Built | C/H | - | - | - | C/H |
| Faturamento e financeiro | C/I/A/X | - | - | - | - |
| Atividades padrao | C/I/A/X | C | C | - | - |
| Configuracao de alertas | C/I/A | C/I/A | - | - | - |

`*` Acesso limitado aos projetos em que o funcionario integra a equipe.

## Regras de escopo

1. ADMIN possui escopo global.
2. SUPERVISOR_TECNICO possui escopo operacional global enquanto os perfis
   personalizados ainda nao estiverem implementados.
3. TECNICO pode consultar e executar somente projetos, OS, obras, evidencias e
   documentos vinculados a uma equipe da qual faca parte.
4. ESTOQUE pode consultar os dados minimos de OS, obra e OR necessarios para
   identificar a movimentacao, sem acesso a documentos financeiros ou
   evidencias de campo.
5. AUDITOR pode consultar dados necessarios para conciliacao e homologacao, mas
   nao pode modificar a execucao de campo ou o estoque.
6. O frontend pode ocultar comandos indisponiveis, mas a regra definitiva deve
   ser sempre aplicada no backend.

## Operacoes sensiveis

As operacoes abaixo devem exigir sessao valida e confirmacao recente da senha:

- cadastrar ou alterar usuario, perfil ou senha;
- arquivar e restaurar entidades operacionais;
- retirar e devolver materiais;
- invalidar documento assinado;
- homologar As-Built;
- alterar dados financeiros ou marcar faturamento como pago;
- alterar a matriz de permissoes quando ela se tornar configuravel.

## Entregas desta fase

- [x] Formalizar a matriz inicial.
- [x] Considerar todos os membros da equipe no escopo do tecnico.
- [x] Restringir arquivos e documentos pelo vinculo com projeto, OS ou obra.
- [x] Restringir consultas de OR do tecnico ao seu escopo operacional.
- [x] Cobrir as regras de escopo desta entrega com testes permitidos e negados.
- [x] Ampliar os testes de integracao para toda a matriz de modulos.
- [ ] Alinhar todos os comandos visiveis no frontend com a matriz.
- [ ] Projetar perfis personalizados e permissoes administraveis.

## Criterio para perfis personalizados

A administracao dinamica de perfis sera iniciada somente depois que as regras
fixas acima estiverem homologadas. Perfis de sistema nao poderao ser removidos,
e o ultimo administrador ativo nunca podera perder o acesso administrativo.
