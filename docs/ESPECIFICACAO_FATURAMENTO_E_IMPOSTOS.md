# Especificacao - Faturamento e Impostos

Data: 22/07/2026

**Status:** IMPLEMENTADA E HOMOLOGADA em 22/07/2026. Evidencias em
`RESULTADO_FASE_B1_FATURAMENTO_2026-07-22.md`.

## 1. Decisao de cronograma

O modulo sera implementado depois da Fase A (baseline/checkpoint) e da Fase B (regressao operacional), como **Fase B.1**. Ele deve entrar antes da revisao visual ampla, pois altera banco, API, regras de calculo e a propria pagina que depois sera padronizada visualmente.

## 2. Referencia analisada

A planilha `RELATORIO DE COBRANCA TJ PB.xlsx` possui duas abas:

- `FATURAMENTO`: item, status operacional, numero da NF, emissao, valor, comarca e situacao do recebimento.
- `IMPOSTO`: item, NF, emissao, competencia, valor, imposto retido, imposto a pagar e total.

Exemplo conciliado da referencia:

| NF | Emissao | Competencia | Valor | Retido 4,8% | A pagar 14,93% | Total 19,73% |
|---|---|---|---:|---:|---:|---:|
| 14 | 16/07/2026 | 20/08/2026 | R$ 23.450,00 | R$ 1.125,60 | R$ 3.501,09 | R$ 4.626,69 |
| 15 | 16/07/2026 | 20/08/2026 | R$ 30.524,00 | R$ 1.465,15 | R$ 4.557,23 | R$ 6.022,38 |

## 3. Estado atual reutilizavel

O sistema ja possui:

- faturamento vinculado a contrato e projeto;
- valor da medicao;
- numero da nota fiscal;
- vencimento da cobranca;
- situacoes `A_FATURAR`, `FATURADO`, `PAGO` e `EM_ATRASO`;
- emissao de NF e baixa de pagamento;
- totais basicos e filtros por contrato/situacao.

## 4. Alteracoes de dados

### Contrato

Adicionar classificacao obrigatoria do contratante:

- `SETOR_PUBLICO`;
- `SETOR_PRIVADO`.

Para o setor publico, o destino exibido sera a comarca vinculada ao projeto/OS. Para o setor privado, sera o nome do cliente do contrato. Registros antigos exigem migracao com valor inicial controlado e revisao posterior.

### Faturamento

Adicionar ou ajustar:

- `dataEmissao` obrigatoria quando houver NF;
- `dataPagamento`, preenchida ao marcar como pago;
- `servicosExecutados` opcional;
- numero da NF unico dentro do contrato;
- `dataVencimento` mantida como vencimento da cobranca, separada da competencia fiscal.

As aliquotas devem ser salvas como snapshot do lancamento fiscal ou obtidas de configuracao versionada. Isso evita que uma futura mudanca de aliquota altere impostos historicos.

## 5. Regras fiscais

- Competencia: dia 20 do mes seguinte a `dataEmissao`, inclusive na virada de dezembro para janeiro.
- Imposto retido: `valor * 0,048`.
- Imposto a pagar: `valor * 0,1493`.
- Total de imposto: `retido + aPagar`, equivalente a `valor * 0,1973`.
- Arredondamento: duas casas decimais, regra monetaria `HALF_UP`.
- A aba fiscal deve ser derivada da NF cadastrada; nao deve existir redigitacao de NF ou valor.

## 6. Interface proposta

### Aba Faturamento

- segmentos `Publico` e `Privado`;
- colunas: contrato, NF, emissao, valor, comarca/cliente, vencimento e situacao;
- acao de registrar pagamento com confirmacao e data;
- servicos executados dentro de `Mais detalhes`;
- totais: faturado, a receber, pago e em atraso;
- filtros por periodo, contrato, NF, tipo de cliente, comarca/cliente e situacao.

### Aba Impostos

- colunas: NF, emissao, competencia, valor, 4,8%, 14,93%, total e alerta;
- totais das quatro colunas financeiras;
- filtros por competencia, vencimento, contrato, NF e tipo de cliente;
- alertas para vencido, vence hoje e proximos do vencimento;
- graficos: faturamento mensal, pago x a receber e impostos por competencia.

## 7. Homologacao obrigatoria

1. Reproduzir no sistema as NFs 14 e 15 da planilha e conciliar os valores centavo a centavo.
2. Testar emissao em dezembro e competencia em 20 de janeiro do ano seguinte.
3. Confirmar que marcar como pago persiste `PAGO` e `dataPagamento` apos recarregar a pagina.
4. Confirmar que uma NF nao gera duas linhas fiscais.
5. Testar setor publico com comarca e setor privado com cliente.
6. Validar filtros combinados, totais e alertas com listas vazias e grandes.
7. Executar testes automatizados, build e smoke test no navegador antes do commit.
