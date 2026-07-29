# Resultado da Homologacao de Retirada e Devolucao

Data: 29/07/2026

## Escopo

Homologacao assistida do ciclo da Ordem de Retirada vinculada a OS de desenvolvimento:

- conferencia dos itens e saldos reservados;
- retirada com dupla assinatura;
- baixa fisica e log dos responsaveis;
- liberacao da Virada de Rede;
- devolucao de ferramenta e material de consumo restante;
- conferencia dos saldos e do historico.

## Documentos utilizados

- OS: `DEV-CONTRATO-001 - OS 01`
- OR: `DEV-CONTRATO-001 - OS 01 - OR 01`
- Obra: `Obra Desenvolvimento`

## Retirada

Itens retirados:

- 2 unidades de Cabo de Rede DEV
- 1 unidade de Alicate DEV

Responsaveis:

- Conferente: `Estoque Desenvolvimento`
- Retirante: `Tecnico Campo A`
- Assinatura manuscrita do conferente: registrada
- Assinatura manuscrita do retirante: registrada

Resultado:

- OR alterada de `GERADA` para `RETIRADA`.
- Cabo de Rede DEV alterado de 100 para 98 unidades.
- Alicate DEV alterado de 5 para 4 unidades.
- Reservas da OS alteradas para zero.
- Data e hora da retirada registradas.
- Historico exibindo quem autorizou e quem retirou.

## Liberacao da Virada

Depois da retirada:

- o botao `Aguardando retirada da OR` passou a `Liberar Virada de Rede`;
- a transicao foi aceita pelo backend;
- a obra passou para a etapa 3, Virada de Rede;
- o progresso foi atualizado de 70% para 85%;
- a timeline da obra passou a exibir o horario real da retirada.

## Devolucao

Itens devolvidos:

- 1 unidade de Cabo de Rede DEV
- 1 unidade de Alicate DEV

Consumo liquido:

- Cabo de Rede DEV: 1 unidade
- Alicate DEV: 0 unidades

Responsaveis:

- Quem devolveu: `Tecnico Campo A`
- Quem recebeu e conferiu: `Estoque Desenvolvimento`
- Assinatura manuscrita de recebimento: registrada

Resultado:

- OR alterada para `DEVOLVIDA`.
- Cabo de Rede DEV alterado de 98 para 99 unidades.
- Alicate DEV alterado de 4 para 5 unidades.
- O retorno integral da ferramenta foi respeitado.
- Data e hora da devolucao registradas.
- Logs de devolucao vinculados a OS, OR, obra e projeto.

## Correcao aplicada

O historico de movimentacoes utilizava o rotulo `Retirou` tambem para movimentos de devolucao.

O frontend agora usa:

- `Retirou` para retiradas;
- `Devolveu` para devolucoes.

A correcao foi validada na propria tabela de historico para os dois itens devolvidos.

## Estado atual

- OS: `EM_EXECUCAO`
- OR: `DEVOLVIDA`
- Obra: etapa 3, Virada de Rede
- Progresso: 85%
- Estoque de cabos: 99 unidades
- Estoque de alicates: 5 unidades
- Consumo liquido registrado: 1 cabo

## Validacoes tecnicas

- Build de producao do frontend concluido com sucesso.
- Retirada e devolucao validadas pela interface.
- Saldos e estados confirmados diretamente no banco de desenvolvimento.
- Historico visual confirmado apos recarregar a pagina.
- A massa de desenvolvimento permanece em estado coerente para a proxima homologacao.

## Proximo passo operacional

Concluir formalmente a execucao tecnica da OS, marcar a Virada de Rede como concluida e encaminhar o fluxo para Auditoria de Retirada/Devolucao e homologacao do As-Built.
