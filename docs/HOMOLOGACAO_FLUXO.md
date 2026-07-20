# Homologacao do fluxo operacional

## Preparacao

1. Pare a aplicacao que estiver conectada ao banco `poprc_dev`.
2. Execute `./scripts/reset-dev-database.ps1`.
3. Confirme digitando `RESETAR poprc_dev`.
4. Inicie o backend com `./mvnw.cmd -Dspring-boot.run.profiles=dev spring-boot:run`.
5. Inicie o frontend normalmente com `npm run dev` na pasta `frontend`.

O ambiente deve apresentar o contrato `DEV-CONTRATO-001`, a obra
`Obra Desenvolvimento`, um supervisor, dois tecnicos e tres materiais no
`Deposito Desenvolvimento`.

## Cenario principal

1. Abra **Ordens de Servico** e emita uma OS para a obra de desenvolvimento.
2. Defina datas validas e selecione ao menos um consumo, uma ferramenta e a
   bobina entre os materiais previstos.
3. Confira se a OS e a primeira OR nasceram vinculadas.
4. Execute a retirada com as duas assinaturas obrigatorias.
5. Entre na **Area do Tecnico** com um membro da equipe, registre o checklist e
   envie evidencias fotograficas.
6. Complete Vistoria, Infraestrutura e Virada de Rede em **Gestao de Obras**.
7. Envie a OS para validacao administrativa e aprove a execucao.
8. Registre a devolucao: a ferramenta deve retornar integralmente; consumo e
   metragem podem retornar parcialmente.
9. Em **Retirada e Devolucao**, informe os valores auditados e homologue o
   As-Built, inclusive com divergencia quando ela for justificada.
10. Preencha, assine e salve o documento final de encerramento.

## Resultados esperados

- Nenhuma retirada acontece sem OR e dupla assinatura.
- O estoque reservado, livre e devolvido permanece conciliado por deposito.
- Ferramentas nao podem encerrar a devolucao com quantidade pendente.
- A OS nao avanca para validacao sem checklist e evidencia.
- Depois do envio para validacao, checklist e evidencias ficam bloqueados.
- A obra permanece em 90% apos a Virada de Rede e chega a 100% somente depois
  do encerramento formal.
- A OS concluida fica somente para consulta no Portal Tecnico.
- Fotos, documentos, assinaturas e movimentacoes continuam disponiveis no
  historico.

## Cenarios adicionais

- Tente retirar saldo maior que o disponivel.
- Gere uma segunda OR para a mesma OS.
- Tente devolver a mesma OR duas vezes.
- Altere o deadline para testar OS proxima do prazo e atrasada.
- Reduza e depois recomponha um saldo para validar alerta ativo, resolucao e
  nova ocorrencia.
- Homologue uma auditoria com sobra e outra com falta de material.
