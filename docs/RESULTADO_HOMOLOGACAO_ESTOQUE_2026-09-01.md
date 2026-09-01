# Resultado da Homologacao do Estoque - 01/09/2026

## Objetivo

Fechar a linha de base do estoque local depois da importacao da planilha central,
da atualizacao de custos e da reconciliacao do historico das ordens de retirada.

## Marco homologado

- Banco: `poprc_local`.
- Materiais ativos: 62.
- Valor total do estoque: **R$ 40.674,36**.
- Deposito ativo: `Estoque Shineray`.
- Valor no deposito: **R$ 40.674,36**.
- Divergencias entre estoque geral e saldos por deposito: 0.
- Divergencias entre reservas gerais e reservas por deposito: 0.
- Materiais reservados no momento da homologacao: 0.
- Movimentacoes de ajuste registradas: 92.

O valor foi confrontado com a celula A2 da aba `ESTOQUE ATUAL` do arquivo
`CONTROLE_ESTOQUE (2).xlsx`. A planilha informa `40674,362`; o sistema utiliza
duas casas decimais e exibe **R$ 40.674,36**.

## Conferencia amostral

| Material | Planilha | Banco | Custo unitario | Resultado |
| --- | ---: | ---: | ---: | --- |
| TAMPA | 151 | 151 | R$ 24,76 | Confere |
| BASE | 168 | 168 | R$ 22,59 | Confere |
| CURVA HORIZONTAL RETA | 85 | 85 | R$ 32,16 | Confere |
| CAIXA DE TOMADA DE 2 F | 112 | 112 | R$ 17,02 | Confere |
| BUCHA/GESSO | 62 | 62 | R$ 0,15 | Confere |
| SUPORTE DE ELETROCALHA 150X50 | 60 | 60 | R$ 5,11 | Confere |
| FITA ROTULADORA DE 12MM | 11 | 11 | R$ 193,14 | Confere |
| ABRACADEIRA METALICA TIPO D 90MM | 19 | 19 | R$ 3,74 | Confere |
| ABRACADEIRA METALICA TIPO D 60MM | 3 | 3 | R$ 2,10 | Confere |

A `CAIXA DE CABO CAT6A` tambem foi validada. A planilha informa 0,41 caixa de
305 m e custo de R$ 2.087,20 por caixa. O sistema armazena 125,05 m a
R$ 6,8433 por metro, resultando em R$ 855,75. A conversao confere.

## Ordens de retirada

A planilha homologada possui 17 abas no total e 12 abas validas de retirada.
As demais sao `CADASTRO_PRODUTOS`, `ESTOQUE ATUAL`, `SOBRAS - RETORNOS`,
`SIMULACAO` e `Configuracoes`.

As 12 abas foram vinculadas a 12 ORs, 8 obras/comarcas, suas OSs e ao contrato
`0001`. Nao foram encontrados vinculos incompletos. A reconciliacao automatica
aplicou 144 correcoes em 01/09/2026; uma nova comparacao retornou zero
divergencias. Existe ainda uma correcao manual auditada de 31/08/2026, por isso
o historico completo possui 145 eventos de reconciliacao.

O numero anterior de 16 abas nao corresponde ao arquivo atualmente homologado.
Caso existam quatro retiradas adicionais, elas precisam ser fornecidas em outra
versao da planilha e importadas separadamente.

## Faltas registradas

Os seguintes valores negativos da retirada de Cabedelo foram preservados como
faltas historicas. O saldo disponivel dos materiais permanece em zero:

| Material | Quantidade em falta |
| --- | ---: |
| CONDUITE 3/4 | 86 |
| PORCA GAIOLA | 15 |
| VELCRO | 1 |

Esses registros nao sao divergencias do banco. Eles representam demanda nao
atendida e devem continuar visiveis para reposicao e auditoria.

## Backup

Foi criado o pacote:

`backups/poprc_local-completo-20260901-104214.zip`

O pacote contem o dump PostgreSQL, 112 arquivos de upload e o manifesto. Foram
validados 113 arquivos do pacote (dump mais uploads), seus tamanhos e hashes
SHA-256, sem erros.

## Verificacao repetivel

O comando abaixo valida o valor homologado, a igualdade dos saldos gerais e
locais, os vinculos das retiradas e a quantidade de abas de OR:

```powershell
.\scripts\auditar-estoque.ps1 `
  -Database poprc_local `
  -ExpectedInventoryValue 40674.36 `
  -ExpectedImportedOrTabs 12
```

## Testes executados

- Auditoria estrutural do estoque: aprovada.
- Testes utilitarios do frontend: 16 aprovados, sem falhas.
- Build de producao do frontend: aprovado.
- Suite completa do backend: 237 testes aprovados, sem falhas ou erros.
- Banco temporario `poprc_test`: criado pelo executor seguro e removido ao final.

A suite inclui testes de concorrencia do estoque, importacao e rollback da
planilha, fluxo operacional, seguranca, documentos e imutabilidade dos logs.

## Situacao e proxima etapa

As etapas de checkpoint, backup, homologacao do estoque e revisao das retiradas
do arquivo atual estao concluidas. A proxima etapa e executar um piloto
operacional com uma nova operacao controlada:

`Projeto -> OS -> equipe -> OR -> retirada -> tecnico -> devolucao -> auditoria -> encerramento`

As faltas de Cabedelo permanecem abertas para reposicao. A procura pelas quatro
abas mencionadas anteriormente so deve ser reaberta se outra planilha fonte for
apresentada.
