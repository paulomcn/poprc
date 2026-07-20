# Banco de desenvolvimento e recuperacao

## Perfil de desenvolvimento

O perfil `dev` usa por padrao o banco `poprc_dev` e inclui a massa idempotente
de `src/main/resources/db/dev`. Inicie a aplicacao com:

```powershell
.\mvnw.cmd -Dspring-boot.run.profiles=dev spring-boot:run
```

A massa cria o contrato `DEV-CONTRATO-001`, uma obra livre para emitir OS,
um supervisor tecnico, dois tecnicos de campo, um deposito e materiais dos
tipos consumo, ferramenta e bobina. A OS e a OR nao sao inseridas por SQL:
elas devem ser criadas pela interface para homologar as regras reais.

Para usar outra conexao, configure `DEV_DB_URL`, `DEV_DB_USERNAME` e
`DEV_DB_PASSWORD`. O banco de desenvolvimento deve continuar terminando em
`_dev` para ser aceito pelos scripts destrutivos. A propria aplicacao tambem
interrompe a inicializacao antes do Flyway quando o perfil `dev` aponta para um
banco sem esse sufixo.

Os scripts nao abrem prompt interativo de senha. Configure `DEV_DB_PASSWORD`
para o reset e `DB_PASSWORD` para backup/restauracao antes de executa-los; se a
autenticacao nao estiver disponivel, a operacao falha imediatamente.

## Reset controlado

```powershell
.\scripts\reset-dev-database.ps1
.\mvnw.cmd -Dspring-boot.run.profiles=dev spring-boot:run
```

O script exige a confirmacao textual `RESETAR poprc_dev`. Ele recusa qualquer
banco sem o sufixo `_dev`. Ao iniciar a aplicacao, o Flyway recria o schema,
aplica todas as migracoes e insere a massa de desenvolvimento.

O reset encerra conexoes abertas no banco de desenvolvimento. Pare qualquer
instancia da aplicacao que esteja usando `poprc_dev` antes de executa-lo.

Depois da inicializacao, siga o roteiro em `docs/HOMOLOGACAO_FLUXO.md`.

## Backup

```powershell
.\scripts\backup-postgres.ps1 -Database poprc
```

O arquivo em formato custom do PostgreSQL e criado em `backups/`, pasta
ignorada pelo Git. A senha pode ser informada por `DB_PASSWORD`.

## Restauracao segura

Teste sempre a restauracao em um banco descartavel:

```powershell
$backup = .\scripts\backup-postgres.ps1 -Database poprc
.\scripts\restore-postgres.ps1 -BackupFile $backup -TargetDatabase poprc_restore_test
```

Por padrao, o script aceita somente destinos terminados em `_restore_test`.
Para restaurar deliberadamente sobre um banco `_dev`, use `-AllowDevTarget`.
O banco principal `poprc` nunca e aceito como destino.

Depois da restauracao, valide o schema e os dados iniciando temporariamente a
aplicacao contra o banco restaurado:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/poprc_restore_test"
.\mvnw.cmd -Dspring-boot.run.arguments="--server.port=0" spring-boot:run
```

Remova `DB_URL` da sessao ao terminar:

```powershell
Remove-Item Env:DB_URL
```

## Regras

- Mudancas de schema entram somente em `src/main/resources/db/migration`.
- Nunca edite uma migracao versionada que ja foi aplicada.
- Dados reproduziveis locais ficam em `src/main/resources/db/dev`.
- Backups, senhas e arquivos restaurados nao devem ser versionados.
- Uma restauracao so e considerada validada depois de abrir o sistema contra o
  banco restaurado e conferir a versao do Flyway.
