# Homologacao responsiva - 01/09/2026

## Resultado e escopo

Concluida a rodada de homologacao automatizada da interface operacional no
navegador Chromium do Codex, com autenticacao real e dados ficticios isolados.
Nao representa homologacao em aparelhos fisicos, Safari ou Firefox.

- 48 verificacoes de dimensoes aprovadas: seis telas, quatro tamanhos e duas preferencias de tema.
- Tamanhos: 320x640, 390x844, 768x1024 e 1440x900.
- Telas: Estoque, Projetos, Ordens de Servico, Gestao de Obras, Portal Tecnico e Execucao da OS.
- Sem overflow horizontal da pagina ou do conteudo principal nas medicoes finais.
- Modais exercitados contidos na tela; tabelas largas mantem rolagem interna.
- Capturas e inspecao visual amostral de formularios, cabecalho, sidebar e temas.
- 21 testes locais de frontend aprovados e build de producao aprovado.
- Tres fluxos reutilizaveis de regressao executados no navegador: modal de projeto, seletor de material da OS e contexto do checklist.
- O aviso preexistente de bundles maiores que 500 kB permanece; nao impede o build.

O Portal Tecnico e a Execucao preservam seu estilo escuro proprio, inclusive com
a preferencia global clara. Esta rodada verificou compatibilidade; nao implementou
uma nova versao clara dessas duas telas.

## Defeitos corrigidos

| Defeito reproduzido | Correcao | Verificacao |
| --- | --- | --- |
| Modal de Projeto maior que a tela em 320x640: topo -59,5 px e rodape 719,5 px | Modal compartilhado com limite de altura, rolagem interna e portal fora do layout | Modal final entre 16 e 624 px; formulario ficticio salvo pela interface |
| Cabecalho com apenas 41 px; classe min-h-18 inexistente no Tailwind configurado | Altura explicita de 64 px e controles sem encolhimento | Medicoes nos quatro tamanhos |
| Links do menu mobile fechado ainda expostos na navegacao | Sidebar invisivel quando fechada no mobile, visivel no desktop | Abrir, navegar e fechar; recolher/expandir em desktop e tablet |
| Seletor de material da OS com cerca de 55 px de largura no celular | Material ocupa uma linha; quantidade e remocao ficam abaixo | Seletor com 223 px em tela de 320 px; preenchimento sem corte dos controles |
| Blocos cinza da OS permaneciam claros e texto ciano pouco legivel no tema escuro | Complemento da camada de tema existente | Capturas nos temas claro e escuro |
| Projetos mostravam Sem nome complementar mesmo com obra vinculada | Cruzamento explicito de projetos e comarcas pelo ID | Dois testes unitarios; busca pelo nome da obra retorna o projeto correto |
| Salvar checklist apagava nome, endereco e papel tecnico enriquecidos na tela | Atualizacao parcial do estado, preservando o contexto da obra | Nome mantido apos salvar; selecao mantida apos recarregar |

Modais de criacao de OS e relatorio tecnico agora usam o componente compartilhado.
Ele identifica o dialogo, mantem o foco do teclado dentro dele, fecha por Escape
e restaura o foco ao sair. Erros do formulario de Projeto aparecem dentro do modal.

## Interacoes exercitadas

1. Criacao de um terceiro projeto ficticio pelo formulario em 320x640, com equipe e data inicial.
2. Busca de projeto pelo nome da obra, sem depender da ordem das listas retornadas pela API.
3. Abertura da nova OS, selecao de material e quantidade, sem emitir outra OS nessa verificacao visual.
4. Abertura e fechamento do relatorio tecnico e da visualizacao de OS na Gestao de Obras.
5. Filtro de obras concluidas exibindo estado vazio e retorno para todas as obras.
6. Busca de material e abertura da simulacao de retirada.
7. Simulacao: 96 unidades disponiveis, solicitacao de 110, saldo projetado -14 e falta de 14; sem baixa ou reserva.
8. Filtro de OS concluidas no Portal Tecnico e acesso a uma OS atribuida.
9. Checklist ficticio salvo e recarregado; nome da obra preservado ao salvar novamente.
10. Sidebar expandida e recolhida, navegacao mobile, Tab e Escape nos modais.

## Isolamento e integridade

- Banco exclusivo: `poprc_responsivo_20260901_test`, criado vazio com 38 migracoes Flyway.
- API temporaria na porta 8086 e frontend temporario na 5174, ambos em loopback.
- Login por CPF/senha e CSRF ativos; login de desenvolvimento e Zoho desativados nesse ambiente.
- Usuario, contrato, projetos, materiais e OSs usados sao ficticios.
- Uploads de teste separados em `.runtime/responsivo-uploads`.
- Evidencias locais em `.runtime/responsivo-20260901`, ignoradas pelo Git.
- Nenhuma senha de usuario operacional foi solicitada ou alterada.
- O banco de teste fica preservado para reproducao; nao faz parte de backup operacional nem deve ser confundido com `poprc_local`.

Auditoria somente de leitura no banco operacional ao final:

| Indicador | Resultado |
| --- | ---: |
| Materiais ativos | 62 |
| Valor atual | R$ 40.674,12 |
| Divergencias geral/depositos | 0 |
| Reservas ativas | 0 |
| Abas de OR importadas / ORs importadas | 12 / 12 |
| Vinculos incompletos | 0 |
| Movimentacoes | 98 |
| Reconciliacoes | 144 |

Faltas historicas preservadas: CONDUITE 3/4 (86), PORCA GAIOLA (15), VELCRO (1).
A linha de base historica e R$ 40.674,36; a diferenca de R$ 0,24 vem do piloto
anterior, nao desta homologacao.

## Posicao no cronograma

| Etapa | Situacao |
| --- | --- |
| Checkpoint, backup e linha de base | Realizados anteriormente; novas alteracoes desta rodada ainda locais |
| 1. Homologar estoque | Auditoria estrutural aprovada; operacao continua exigindo conferencia fisica |
| 2. Revisar retiradas existentes | Doze registros importados estruturalmente validos; nao houve nova comparacao integral com outra planilha |
| 3. Piloto operacional | Executado e documentado anteriormente |
| 4. Defeitos e concorrencia | Rodada anterior concluida, 256 testes de backend aprovados; backend nao alterado nesta rodada visual |
| 5. Usabilidade | Homologacao automatizada desta rodada concluida; aceite da equipe em aparelhos reais pendente |
| 6. Permissoes e implantacao | Proxima frente apos checkpoint e aceite; definir matriz de permissoes antes de ampliar perfis configuraveis |
| Zoho e impressao fisica definitiva | Adiados conforme combinado |

## Proximos passos

1. Revisar o diff e criar checkpoints separados para reservas/testes de backend e responsividade/frontend. Publicar somente codigo, testes e relatorios, nunca o banco, uploads, logs ou credenciais.
2. Fazer aceite curto com a equipe em um celular e em outro computador da rede: filtros, modais, menu, checklist e fotos. Registrar aparelho, navegador, passos e resultado de qualquer defeito.
3. Conferir a matriz atual de permissoes com Administrador, Estoque, Supervisor Tecnico, Tecnico e Auditor; testar permissoes permitidas e negadas antes de criar novos perfis configuraveis.
4. Preparar implantacao com backup/restauracao testados, variaveis de ambiente, HTTPS e monitoramento, sem antecipar Zoho ou impressao definitiva.

Nao e necessario repetir movimentacoes no estoque operacional para testar layout.
Esta rodada nao fez importacao XLSX, teste de camera/touch em aparelho fisico,
nem varreu todos os modais de todas as paginas. A cobertura de evidencias e do
ciclo completo permanece nos testes de integracao da rodada anterior.

## Reproducao

Na pasta `frontend`:

```powershell
npm.cmd run test:utils
npm.cmd run build
```

`tests/responsiveChecks.mjs` fornece a inspecao DOM somente de leitura e as
assercoes de dimensoes. `tests/responsiveFlows.mjs` recebe uma aba autenticada
do navegador para repetir os tres fluxos. Esses fluxos nao sao executados pelo
comando de testes unitarios e precisam de dados ficticios preparados; o de checklist
grava o checklist atual e jamais deve apontar para uma OS operacional.

Exemplo com uma aba de homologacao ja aberta no cliente de navegador:

```javascript
await viewport.set({ width: 320, height: 640 });
await flows.verifyProjectModal(tab); // pagina Projetos
await flows.verifyOsMaterialLayout(tab); // pagina OS, com um projeto disponivel
await flows.verifyChecklistContext(tab); // pagina de execucao de uma OS ficticia
```

Cada chamada exige navegar previamente para a pagina indicada e aguardar seus
dados. Os resultados desta execucao estao em `resultados.json` e `fluxos.json`
no diretorio local de evidencias, junto das capturas.
