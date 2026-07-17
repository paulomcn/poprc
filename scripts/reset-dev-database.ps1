param(
    [string]$Database = "poprc_dev",
    [string]$HostName = "localhost",
    [int]$Port = 5432,
    [string]$Username = "postgres",
    [string]$Password = $env:DEV_DB_PASSWORD,
    [switch]$Force
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "PostgresTools.ps1")

Assert-SafeDatabaseName $Database @("_dev")

if (-not $Force) {
    $confirmation = Read-Host "Digite RESETAR $Database para apagar todos os dados de desenvolvimento"
    if ($confirmation -ne "RESETAR $Database") {
        throw "Reset cancelado."
    }
}

$dropDb = Resolve-PostgresTool "dropdb"
$createDb = Resolve-PostgresTool "createdb"

Invoke-WithPgPassword $Password {
    & $dropDb --host $HostName --port $Port --username $Username --if-exists --force $Database
    if ($LASTEXITCODE -ne 0) {
        throw "Nao foi possivel remover o banco de desenvolvimento."
    }

    & $createDb --host $HostName --port $Port --username $Username $Database
    if ($LASTEXITCODE -ne 0) {
        throw "Nao foi possivel recriar o banco de desenvolvimento."
    }
}

Write-Output "Banco $Database recriado vazio."
Write-Output "Inicie com: .\mvnw.cmd -Dspring-boot.run.profiles=dev spring-boot:run"
Write-Output "O Flyway aplicara o schema e a massa de desenvolvimento automaticamente."

