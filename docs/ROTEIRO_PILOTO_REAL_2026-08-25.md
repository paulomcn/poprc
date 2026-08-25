# Roteiro do Primeiro Piloto Real

Data de preparação: 25/08/2026

## Objetivo

Validar uma operação real completa sem alterar artificialmente o estoque:

Contrato -> Projeto -> OS -> OR -> Retirada -> Técnico -> Devolução -> Auditoria -> Encerramento.

Este roteiro complementa a homologação sintética já aprovada. As assinaturas, quantidades, fotos, horários e pessoas deste piloto devem representar a operação física real.

## Proteção antes do piloto

1. Gerar backup do banco e da pasta de uploads.
2. Executar `./scripts/test-pilot.ps1` e exigir resultado sem falhas.
3. Registrar o saldo inicial de cada material selecionado.
4. Confirmar quem atuará como administrador, supervisor técnico, técnico e responsável pelo estoque.
5. Não executar reset do banco de desenvolvimento durante o piloto.

## Regressão de preparação

Em 25/08/2026, o comando `./scripts/test-pilot.ps1` foi executado no banco isolado `poprc_pilot_test`:

- 32 migrações Flyway aplicadas;
- 95 testes críticos executados;
- 0 falhas e 0 erros;
- banco temporário removido ao final;
- nenhum dado do estoque operacional alterado.

## Backup anterior ao piloto

O backup operacional completo foi gerado e restaurado para validação em 25/08/2026:

- pacote: `backups/poprc_local-completo-20260825-145940.zip`;
- SHA-256: `d569b9d3336da7a0248c136ebbb11369705e2eeb81c2233a2041c63ac28ad302`;
- banco restaurado isolado: `poprc_pilot_restore_test`;
- 42 tabelas comparadas, sem diferenças de contagem;
- 1.494 arquivos de upload comparados, sem ausências ou diferenças de tamanho/hash;
- cópia dos uploads: `backups/restores/poprc_pilot_restore_test/rc_uploads`.

O backend operacional foi reiniciado usando `poprc_local`, com o login de desenvolvimento desativado. A cópia restaurada não está conectada à aplicação ativa.

## Critérios para iniciar

- Contrato e projeto reais cadastrados e ativos.
- Supervisor técnico e técnicos atribuídos ao projeto.
- Materiais cadastrados com saldo, depósito, unidade e custo médio revisados.
- Data de início, fim e deadline da OS validados.
- Navegadores dos participantes acessando o mesmo backend.

## Execução acompanhada

| Passo | Responsável | Ação e evidência obrigatória | Resultado esperado |
| --- | --- | --- | --- |
| 1 | Administrador | Criar a OS com materiais previstos e equipe | OS sequencial e primeira OR geradas |
| 2 | Supervisor | Preencher documento inicial, vistoria, foto e assinatura real | OS liberada para retirada |
| 3 | Estoque e retirante | Conferir a OR e coletar as duas assinaturas | Saldo baixado uma única vez e log completo |
| 4 | Técnico | Registrar checklist e relatório fotográfico | Evidências visíveis e vinculadas à OS |
| 5 | Supervisor | Validar infraestrutura e provas da virada de rede | Obra preparada para validação administrativa |
| 6 | Administrador | Aprovar a execução | OS aguardando devolução |
| 7 | Técnico e estoque | Devolver ferramentas e sobras com assinatura do recebedor | Estoque recomposto e divergências calculadas |
| 8 | Auditor | Conferir quantidades e homologar o As-Built | Conciliação e justificativas registradas |
| 9 | Responsáveis | Preencher e assinar o documento final | PDF definitivo íntegro e arquivado |
| 10 | Administrador | Encerrar a obra | OS, obra e projeto concluídos; histórico preservado |

## Critérios de parada

Interromper o piloto antes de avançar se ocorrer qualquer uma destas situações:

- saldo do estoque diferente da movimentação física;
- retirada sem OR ou sem duas assinaturas;
- ferramenta devolvida parcialmente;
- foto, assinatura ou PDF não disponível após atualizar a página;
- usuário sem permissão consegue executar ação sensível;
- etapa avança sem cumprir os requisitos anteriores;
- erro é exibido, mas a operação é gravada parcialmente.

## Registro de defeitos

| ID | Data/hora | Etapa | Perfil | OS/OR | Esperado | Obtido | Severidade | Evidência | Teste de regressão | Status |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| PIL-001 | 25/08/2026 15:13 | Virada de Rede | Administrador | 0001 - OS 02 | Preservar a foto após concluir a etapa | Texto substituiu o caminho da foto | Alta | Reabertura da prova retornou arquivo não encontrado | `bloqueiaSubstituicaoDaFotoDaViradaDeRedePorTexto` | Corrigido |

Severidade:

- Crítica: risco de saldo incorreto, acesso indevido, perda de documento ou avanço inválido.
- Alta: bloqueia uma etapa operacional sem alternativa segura.
- Média: operação funciona com dificuldade ou informação inconsistente.
- Baixa: problema visual ou textual sem impacto no registro.

## Encerramento do piloto

O piloto é aprovado somente quando:

- todos os passos forem concluídos por pessoas reais;
- o saldo final do sistema coincidir com a contagem física;
- todos os documentos e evidências permanecerem disponíveis após novo login;
- nenhum defeito crítico ou alto permanecer aberto;
- cada defeito corrigido tiver um teste automatizado ou uma justificativa técnica registrada.
