# Piloto automatizado e concorrencia - 01/09/2026

## Resultado

Concluida a rodada de automacao do piloto e testes de concorrencia prevista apos
o piloto local documentado em `RESULTADO_PILOTO_OPERACIONAL_2026-09-01.md`.

- Backend: 256 testes, zero falhas, zero erros e zero testes ignorados.
- Frontend: 16 testes de utilitarios aprovados.
- Build do frontend aprovado, com aviso preexistente de bundles maiores que 500 kB.
- 18 novos cenarios automatizados acrescentados nesta rodada.
- Banco operacional consultado apenas para auditoria; nenhuma movimentacao aplicada nele.

## Defeito encontrado e corrigido

Ao criar uma OS, a reserva era gravada em `Material`, mas nao atualizava
`SaldoMaterialLocal`. Assim, o total geral podia mostrar duas unidades reservadas
enquanto o deposito mostrava zero. O problema tambem afetava a reserva remanescente
de outra OS apos uma retirada.

Os novos testes reproduziram a divergencia antes da alteracao de producao.
A correcao sincroniza as reservas locais ao reservar, liberar, retirar, creditar,
ajustar e transferir saldo. Material e saldos locais permanecem protegidos por
locks transacionais; reserva acima da capacidade dos depositos e rejeitada.

A OS continua reservando no estoque geral. O espelho por deposito e distribuido
pela capacidade fisica, na ordem dos IDs dos depositos. Isso nao representa uma
escolha explicita de deposito por OS, nem altera o total reservado ou o custo.
Quando uma transferencia muda a capacidade local, o espelho e redistribuido.

Nao foi necessaria migracao do banco. O banco operacional auditado nao tinha
reservas ativas, portanto nao foi necessario corrigir registros anteriores.

## Cobertura acrescentada

| Teste | Cenarios | Verificacoes |
| --- | ---: | --- |
| PilotoOperacionalApiIntegrationTest | 2 | Ciclo completo com consumo integral ou devolucao parcial; As-Built exato ou divergente |
| OrdemRetiradaConcorrenciaIntegrationTest | 12 | Quatro disputas concorrentes, repetidas tres vezes cada |
| SaldoLocalReservaIntegrationTest | 3 | Unidade, fracionado e metragem; reserva, alteracao, transferencia e liberacao em dois depositos |
| SaldoLocalServiceTest | 1 novo | Reserva superior ao saldo local rejeitada antes de salvar espelho parcial |

O piloto exercita as rotas HTTP via MockMvc com login por CPF/senha, sessoes e
cookies CSRF reais: projeto, equipe, OS, OR automatica, documento inicial,
vistoria, retirada, checklist, foto do tecnico, validacao, virada de rede,
devolucao, auditoria, documento final e encerramento.

As fotos sao geradas para teste e lidas novamente pela API. Os documentos inicial
e final recebem tres assinaturas sinteticas identificadas como teste, logs e
verificacao de integridade. Seus PDFs sao gerados e abertos por um leitor PDF.
Tambem sao conferidos os oito eventos de status, os tres documentos da OR, os
responsaveis pelas movimentacoes e os saldos fisicos, reservados e financeiros.

As disputas concorrentes cobertas sao:

1. Duas retiradas simultaneas da mesma OR: somente uma pode efetivar.
2. Duas devolucoes simultaneas da mesma OR: somente uma pode efetivar.
3. Duas ORs distintas retirando e depois devolvendo o mesmo material.
4. Uma OR devolvendo enquanto outra retira o mesmo material.

## Isolamento e limites

- `scripts/test.ps1` cria e remove exclusivamente o banco `poprc_test`.
- Os 38 scripts Flyway foram aplicados no banco vazio de teste.
- O Maven direciona os uploads dos testes para `target/test-uploads`, sem usar os uploads operacionais.
- O piloto HTTP usa rollback ao final de cada cenario.
- A concorrencia usa threads, transacoes e commits reais em PostgreSQL; limpa somente seus registros de teste.
- Apenas a geracao de documentos da OR e o repositorio de historico de status sao substituidos por mocks nos testes concorrentes. Seus comportamentos reais e a imutabilidade sao cobertos separadamente pelo piloto e pelos testes existentes.
- O contexto do piloto HTTP e reiniciado antes da classe para nao herdar o repositorio CSRF substituido por utilitarios de outros testes. Nenhuma protecao de autenticacao foi removida.
- Estes testes nao substituem homologacao visual, uso fisico em campo ou teste de carga prolongado com muitos usuarios.

## Conferencia do estoque operacional

Auditoria somente de leitura em `poprc_local`:

| Indicador | Resultado |
| --- | ---: |
| Materiais ativos | 62 |
| Valor total | R$ 40.674,12 |
| Divergencias de saldo e reserva entre geral e depositos | 0 |
| Materiais com reserva ativa | 0 |
| Abas de OR importadas / ORs importadas | 12 / 12 |
| Vinculos incompletos | 0 |
| Movimentacoes | 98 |
| Reconciliacoes de planilha | 144 |

As faltas historicas permanecem registradas: CONDUITE 3/4 (86), PORCA GAIOLA (15)
e VELCRO (1). Nao foram apagadas nem transformadas em saldo fisico negativo.
O valor inicial homologado continua sendo R$ 40.674,36; a diferenca de R$ 0,24
decorre do piloto local anterior, nao destes testes automatizados.

## Posicao no plano e proximos passos

| Frente do plano operacional | Situacao |
| --- | --- |
| Checkpoint, backup e linha de base | Realizados na rodada anterior; esta rodada nao alterou dados operacionais |
| Estoque e retiradas existentes | Auditoria estrutural aprovada para os 12 registros de abas importadas; completude contra outra planilha nao reavaliada nesta rodada |
| Piloto operacional | Executado localmente na rodada anterior e agora coberto por testes automatizados |
| Defeitos do piloto e concorrencia | Rodada atual concluida; reserva local corrigida e regressao coberta |
| Usabilidade e responsividade | Rodada posterior concluida no navegador; aceite em aparelhos reais pendente. Ver RESULTADO_HOMOLOGACAO_RESPONSIVA_2026-09-01.md |
| Permissoes configuraveis e implantacao | Posteriores a homologacao da interface |
| Zoho e impressao fisica definitiva | Mantidos para etapa posterior, conforme combinado |

Sequencia recomendada:

1. Revisar e versionar este conjunto de testes e a correcao de reservas.
2. Homologar Estoque, Projetos, OS, Gestao de Obras e Area do Tecnico em desktop,
   tablet e celular, incluindo sidebar, temas, tabelas, filtros e modais.
3. Para cada defeito visual ou de interacao, registrar reproducao, corrigir e
   repetir o mesmo cenario sem movimentar o estoque operacional por engano.
4. Validar com a equipe os fluxos de uso diario e as permissoes por perfil.
5. Preparar implantacao e rotinas de backup/restauracao; manter Zoho e impressao
   definitiva no momento combinado.

## Como repetir

Na raiz do backend:

```powershell
.\scripts\test.ps1
.\scripts\auditar-estoque.ps1 -ExpectedInventoryValue '40674.12' -ExpectedImportedOrTabs 12
```

O valor esperado da auditoria corresponde a esta fotografia do banco. Movimentacoes
operacionais legitimas posteriores exigem atualizar o valor esperado.

Na pasta `frontend`:

```powershell
npm.cmd run test:utils
npm.cmd run build
```
