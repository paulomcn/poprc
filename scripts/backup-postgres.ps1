param(
    [string]$Database = "poprc",
    [string]$HostName = "localhost",
    [int]$Port = 5432,
    [string]$Username = "postgres",
    [string]$Password = $env:DB_PASSWORD,
    [string]$OutputDirectory = (Join-Path $PSScriptRoot "..\backups")
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "PostgresTools.ps1")

if ($Database -notmatch '^[A-Za-z0-9_]+$') {
    throw "Nome de banco invalido: $Database"
}

$pgDump = Resolve-PostgresTool "pg_dump"
New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$outputFile = Join-Path (Resolve-Path $OutputDirectory) "$Database-$timestamp.dump"

Invoke-WithPgPassword $Password {
    & $pgDump --host $HostName --port $Port --username $Username `
        --format custom --compress 9 --no-owner --no-privileges `
        --file $outputFile $Database
    if ($LASTEXITCODE -ne 0) {
        throw "pg_dump terminou com codigo $LASTEXITCODE."
    }
}

Write-Output $outputFile
