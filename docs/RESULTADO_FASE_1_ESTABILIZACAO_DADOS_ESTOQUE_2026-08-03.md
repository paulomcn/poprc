# Resultado da Fase 1 - Estabilização de Dados e Estoque

Data: 03/08/2026

## Objetivo

Homologar e endurecer a importação de planilhas de estoque antes de ampliar perfis,
interface e infraestrutura. A fase segue o plano de ação revisado do relatório de
03/08/2026.

## Planilha de Referência

A planilha real `controle_retiradas_estoque.xlsx` foi inspecionada sem alterar o
arquivo original. Foram identificadas as abas `ESTOQUE_ATUAL`, `ESPERANÇA`,
`ALAGOA_NOVA`, `CUITÉ`, `CABEDELO` e `Configurações`.

As abas de retirada mantêm uma sequência de saldos entre obras e podem representar
falta com saldo final negativo. Algumas células calculadas da própria planilha
apresentam erro de fórmula; por isso, o sistema reconstrói a sequência a partir do
inventário-base e das quantidades retiradas.

## Correções Implementadas

- A importação passa a ser bloqueada quando o parser encontra qualquer linha inválida.
- O modal mostra todas as linhas que precisam ser corrigidas, com aba e número da linha.
- O backend também rejeita requisições que informem avisos, protegendo a regra fora da interface.
- Materiais duplicados no inventário e dentro de uma mesma aba de retirada são bloqueados.
- Cada retirada precisa referenciar um material existente no inventário-base.
- O saldo inicial de cada retirada precisa continuar exatamente o saldo final anterior.
- O saldo final precisa ser igual a `saldo inicial - quantidade retirada`.
- Saldos e quantidades de materiais unitários precisam ser números inteiros.
- A complementação de uma importação precisa usar o mesmo depósito da importação original.
- O inventário-base da complementação precisa ser idêntico ao snapshot originalmente importado.
- Uma segunda complementação do mesmo arquivo continua bloqueada.
- Foram definidos limites de 1.000 materiais e 5.000 retiradas por requisição.
- A linha de origem passou a integrar o payload para produzir erros rastreáveis.

## Integridade Transacional

Foi criado um teste de integração que aplica com sucesso o primeiro ajuste e provoca
falha no item seguinte. O teste confirma que a transação desfaz o ajuste anterior, a
movimentação de estoque e o registro da importação.

As evidências de metragem de bobinas e rolos também foram revisadas. Fotos de retirada
e devolução são validadas como JPG/PNG, armazenadas no servidor e removidas caso a
transação seja revertida.

## Verificações Executadas

- Backend: 208 testes, sem falhas, erros ou testes ignorados.
- Banco: PostgreSQL isolado recriado e migrado por 28 versões do Flyway.
- Testes específicos do importador: 10 testes unitários e 1 teste de integração.
- Frontend: build de produção concluído com sucesso.
- Dependências frontend: nenhuma vulnerabilidade alta ou crítica; permanecem duas
  vulnerabilidades moderadas do React Router cuja correção exige migração incompatível para v7.
- Git: verificação de whitespace sem erros.

## Critérios de Aceite

| Critério | Resultado |
| --- | --- |
| Não criar duplicidades silenciosas | Atendido |
| Não alterar saldo sem histórico | Atendido e coberto por rollback transacional |
| Não importar parcialmente arquivo inválido | Atendido |
| Explicar falhas por aba e linha | Atendido no modal e nas respostas da API |
| Bloquear reimportação/complementação duplicada | Atendido |
| Preservar sequência de saldo entre abas | Atendido |
| Persistir evidências de retirada e devolução | Atendido |

## Homologação Manual Concluída em 04/08/2026

A planilha real foi enviada pela interface em uma cópia isolada do banco `poprc_dev`.
O banco original não recebeu alterações e a cópia foi removida após a conferência.

Resultados da prévia:

- 62 materiais reconhecidos, sem itens bloqueados.
- Valor total reconhecido de R$ 57.241,97.
- Quatro abas de retirada reconhecidas: `ESPERANÇA`, `ALAGOA_NOVA`, `CUITÉ` e `CABEDELO`.
- Todas as abas foram vinculadas à OS de desenvolvimento disponível no ambiente isolado.

Resultados persistidos:

- 62 materiais processados.
- Quatro abas processadas.
- 71 registros históricos de retirada.
- 37 linhas com falta de material.
- 2.213 unidades retiradas e 1.719 unidades faltantes acumuladas segundo a sequência da planilha.

O mesmo arquivo foi enviado novamente e a operação foi bloqueada com a mensagem
`O estoque e as retiradas desta planilha já foram importados.`. Antes e depois da
tentativa repetida, os contadores permaneceram em três importações, 113 itens de
histórico de inventário, 132 retiradas importadas e 138 movimentações de estoque.

Foi tentada uma verificação visual adicional com uma planilha artificial inválida, mas
o arquivo temporário reexportado pela ferramenta de planilhas não foi aceito pelo
ExcelJS e, portanto, esse resultado foi descartado. O bloqueio de importação parcial
permanece comprovado pelos testes unitários e pelo teste transacional da suíte.

Com a importação real, a reimportação bloqueada e os 208 testes aprovados, a Fase 1
está concluída.
