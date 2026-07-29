# Resultado: Valorização do Estoque e Integração com Faturamento

Data da homologação: 29/07/2026

## Objetivo

Adicionar custo médio real aos produtos, calcular o valor financeiro do estoque e
conectar o encerramento da OS ao início do faturamento.

## Referência analisada

Foi utilizada como referência funcional a planilha
`controle_retiradas_estoque_inteligente.xlsx`.

A lógica aproveitada foi:

- saldo atual multiplicado pelo valor unitário;
- valor total por produto;
- soma do valor total do estoque;
- separação entre saldo físico e valor financeiro.

O sistema mantém uma proteção adicional em relação à planilha: não permite saldo
negativo nas retiradas.

## Implementado

### Estoque

- campo persistente `custoMedio` no cadastro do material;
- edição manual do custo médio no catálogo;
- custo opcional em cada entrada de material;
- recálculo pela média ponderada:
  `(saldo anterior * custo anterior + entrada * custo da entrada) / saldo posterior`;
- custo por unidade ou por metro, conforme o tipo de controle;
- valor total calculado por produto;
- indicador com o valor total de todo o estoque;
- custo real acumulado nos materiais utilizados pelo projeto;
- migração Flyway `V24__custo_medio_materiais.sql`.

### Relatório Excel

A exportação do estoque agora contém a aba `Estoque Atual`, com:

- código;
- produto;
- categoria;
- saldo;
- unidade;
- custo médio unitário;
- valor em estoque por produto;
- total geral do estoque.

As células de valor utilizam fórmulas e formatação monetária.

### Faturamento

- novos lançamentos exigem uma OS concluída do projeto selecionado;
- a tabela e os detalhes exibem o número da OS;
- ao emitir a nota fiscal, a máquina de estados altera a OS de `CONCLUIDA` para
  `FATURADA`;
- registros financeiros antigos sem OS continuam consultáveis;
- migração Flyway `V25__vincula_faturamento_a_ordem_servico.sql`.

## Homologação

- suíte completa: 189 testes, sem falhas;
- testes focados finais: 12 testes, sem falhas;
- build Vite concluído;
- Flyway validado e banco de desenvolvimento atualizado até V25;
- navegador sem erros de console;
- custo de homologação cadastrado no `Cabo de Rede DEV`: R$ 3,50;
- saldo homologado: 99 unidades;
- valor do produto e total do estoque homologados: R$ 346,50;
- modal financeiro exibiu a OS `DEV-CONTRATO-001 - OS 01`.

## Observação contábil

O custo médio do estoque representa custo de aquisição e não preço de venda. O
valor da medição/nota fiscal continua sendo informado no módulo de faturamento,
sem misturar receita com custo do material.
