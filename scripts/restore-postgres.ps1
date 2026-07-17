param(
    [Parameter(Mandatory = $true)][string]$BackupFile,
    [string]$TargetDatabase = "poprc_restore_test",
    [string]$HostName = "localhost",
    [int]$Port = 5432,
    [string]$Username = "postgres",
    [string]$Password = $env:DB_PASSWORD,
    [switch]$AllowDevTarget,
    [switch]$Force
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "PostgresTools.ps1")

$allowedSuffixes = @("_restore_test")
if ($AllowDevTarget) {
    $allowedSuffixes += "_dev"
}
Assert-SafeDatabaseName $TargetDatabase $allowedSuffixes

$resolvedBackup = Resolve-Path $BackupFile
$dropDb = Resolve-PostgresTool "dropdb"
$createDb = Resolve-PostgresTool "createdb"
$pgRestore = Resolve-PostgresTool "pg_restore"

if (-not $Force) {
    $confirmation = Read-Host "Digite RESTAURAR $TargetDatabase para substituir o banco de destino"
    if ($confirmation -ne "RESTAURAR $TargetDatabase") {
        throw "Restauracao cancelada."
    }
}

Invoke-WithPgPassword $Password {
    & $dropDb --host $HostName --port $Port --username $Username --if-exists --force $TargetDatabase
    if ($LASTEXITCODE -ne 0) {
        throw "Nao foi possivel remover o banco de destino."
    }

    & $createDb --host $HostName --port $Port --username $Username $TargetDatabase
    if ($LASTEXITCODE -ne 0) {
        throw "Nao foi possivel criar o banco de destino."
    }

    & $pgRestore --host $HostName --port $Port --username $Username `
        --dbname $TargetDatabase --exit-on-error --no-owner --no-privileges $resolvedBackup
    if ($LASTEXITCODE -ne 0) {
        throw "pg_restore terminou com codigo $LASTEXITCODE."
    }
}

Write-Output "Backup restaurado com sucesso em $TargetDatabase."

