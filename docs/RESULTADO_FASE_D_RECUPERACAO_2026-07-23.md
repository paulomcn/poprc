# Resultado da Fase D - Documentos e recuperacao

Data: 23/07/2026

## Escopo executado

- Raiz de uploads configuravel por `APP_UPLOAD_DIR`, preservando `/uploads/**`.
- Snapshot PDF da OR nos marcos `GERACAO`, `RETIRADA` e `DEVOLUCAO`.
- Registro append-only do snapshot com caminho, status, horario e SHA-256.
- Verificacao do hash antes de devolver um PDF arquivado.
- Backup completo com dump, uploads e manifesto SHA-256.
- Restauracao protegida em banco e pasta isolados.
- Testes automatizados de adulteracao e imutabilidade no PostgreSQL.

## Homologacao automatizada

- 95 testes aprovados, sem falhas.
- Flyway aplicado do zero ate V21.
- Fluxo operacional completo, concorrencia e seguranca existente preservados.
- PDF adulterado recusado.
- `UPDATE` e `DELETE` da tabela de documentos da OR recusados por trigger.

## Restauracao real

Pacote:

`backups/poprc-completo-20260723-141740.zip`

Destino:

- banco: `poprc_restore_test`
- uploads: `backups/restores/poprc_restore_test/rc_uploads`
- backend temporario: porta `8091`

Contagens comparadas entre origem e copia:

| Tabela | Origem | Restaurada |
|---|---:|---:|
| contratos | 8 | 8 |
| projetos | 10 | 10 |
| ordens_servico | 9 | 9 |
| ordens_retirada | 5 | 5 |
| documentos_internos | 6 | 6 |
| evidencias_foto | 5 | 5 |
| materiais | 7 | 7 |

O backup foi gerado com Flyway V20. Ao abrir a copia com o codigo atual, o
Flyway aplicou V21 normalmente.

## Provas de abertura

- `liveness`: HTTP 200, `UP`
- `readiness`: HTTP 200, `UP`
- listagem de ORs: 5 registros
- upload restaurado: HTTP 200, 12.197 bytes
- arquivos restaurados: 71
- PDF de OR: HTTP 200, 21.037 bytes
- snapshot criado na copia: `DEVOLUCAO`
- PDF de documento interno: HTTP 200, 39.045 bytes

O health agregado retornou `DOWN` somente pelo SMTP local indisponivel na porta
1025. Banco, aplicacao e armazenamento permaneceram prontos; os grupos
`liveness` e `readiness` ficaram `UP`.

## Resultado

Aceite digital aprovado. O processo recupera banco e arquivos sem tocar no
ambiente ativo, detecta adulteracao e permite abrir documentos pela API da
copia restaurada.

## Pendente separado

A impressao fisica A4 dos quatro cenarios documentais aguarda o feedback do
time e nao invalida o aceite digital desta fase.
