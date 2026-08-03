# Resultado da Homologacao das Fases D e E

Data: 24/07/2026

Branch: `fase-e/autenticacao`

Checkpoint inicial: `93e36a6 feat: exige reautenticacao em operacoes sensiveis`

## Escopo

- Fase D: PDFs operacionais, snapshots imutaveis, backup completo e restauracao.
- Fase E: login, sessao, CSRF, perfis, atribuicoes, reautenticacao e acesso em rede.

## Resultado automatizado

- `120` testes aprovados, sem falhas, erros ou testes ignorados.
- `22` migracoes Flyway aplicadas desde um schema PostgreSQL vazio.
- Build de producao do frontend aprovado.
- Testes de integridade recusam adulteracao de PDFs e alteracao dos logs append-only.
- Testes de seguranca cobrem `401`, `403`, reautenticacao `428`, CSRF, logout,
  troca de senha, perfis, uploads e filas operacionais.

## Fase D - Documentos e recuperacao

O pacote `backups/poprc-completo-20260723-141740.zip` foi restaurado novamente
em ambiente isolado:

- banco: `poprc_restore_test`;
- uploads: `backups/restores/poprc_restore_test/rc_uploads`;
- backend temporario: porta `8091`.

O script validou o manifesto e os hashes SHA-256 antes da restauracao.

Contagens restauradas:

| Tabela | Registros |
|---|---:|
| contratos | 8 |
| projetos | 10 |
| ordens_servico | 9 |
| ordens_retirada | 5 |
| documentos_internos | 6 |
| evidencias_foto | 5 |
| materiais | 7 |

Provas de abertura:

- liveness: `UP`;
- readiness: `UP`;
- upload restaurado: HTTP `200`, 12.197 bytes;
- PDF atual da OR: HTTP `200`, 21.037 bytes;
- snapshot arquivado da OR: HTTP `200`, 21.037 bytes.

Foram renderizadas e inspecionadas visualmente as 12 paginas dos quatro
cenarios digitais:

- OS vazia;
- OS preenchida;
- OR antes da retirada;
- OR concluida.

Nao foram encontrados cortes, sobreposicoes ou campos ilegíveis.

### Pendente externo da Fase D

A impressao fisica A4 permanece aguardando o equipamento e o aceite do time.
O aceite digital esta aprovado; o aceite fisico ainda nao deve ser marcado
como concluido.

## Fase E - Seguranca

Homologacao em banco isolado `poprc_auth_dev`, backend `8092` e frontend `5174`.

Validacoes aprovadas:

- perfis `ADMIN`, `SUPERVISOR_TECNICO`, `TECNICO`, `ESTOQUE` e `AUDITOR`
  disponiveis na massa de teste;
- redirecionamento para a pagina inicial correta de cada perfil;
- rotas financeiras bloqueadas para Tecnico;
- sidebar e atalhos coerentes com as permissoes;
- logout e troca de usuario de teste;
- fila operacional limitada automaticamente pela area e pelo usuario;
- sessao do Supervisor preservada apos reinicio real do backend;
- preflight CORS da origem `http://192.168.0.17:5173` aprovado com credenciais;
- comprovantes financeiros e evidencias protegidos por categoria e perfil;
- reautenticacao, repeticao com novo CSRF e log da operacao sensivel cobertos
  pela suite atual e pela homologacao visual que precedeu o checkpoint E.2.

## Bugs encontrados e corrigidos

1. A massa de desenvolvimento nao possuia usuarios Estoque e Auditor.
2. O perfil Estoque recebia `403` na fila exibida em sua propria pagina.
3. A fila do Tecnico ignorava membros da equipe que nao eram o responsavel principal.
4. O Dashboard do Supervisor mostrava atalhos para paginas proibidas.
5. O menu compacto do Portal Tecnico nao possuia nome acessivel.
6. Qualquer usuario autenticado podia solicitar qualquer categoria de upload.
7. O guia e o `.env.example` ainda indicavam seguranca desativada por padrao.

Todos os defeitos corrigidos receberam validacao automatizada ou visual.

## Pendencias antes do piloto

### Alta

- Executar a Fase E em um segundo computador fisico da mesma rede.
- Homologar o login Zoho com contas reais quando as credenciais oficiais forem
  disponibilizadas.
- Evoluir a protecao de arquivos de categoria/perfil para autorizacao por
  entidade: um Tecnico deve abrir somente evidencias e documentos das OS de
  sua equipe, mesmo conhecendo uma URL de outro arquivo.

### Media

- Registrar screenshots de cada perfil em desktop e celular para o dossie final.
- Resolver os avisos futuros de compatibilidade do Mockito/JDK e o warning de
  `open-in-view`; eles nao causaram falha nesta rodada.

### Externa

- Imprimir e assinar os quatro cenarios documentais em A4.

## Conclusao

- Fase D digital: aprovada.
- Fase D fisica: pendente externa.
- Fase E automatizada e no computador local: aprovada com correcoes.
- Fase E completa: pendente de segundo computador, Zoho real e autorizacao de
  arquivo por entidade antes de liberar um piloto com usuarios reais.
