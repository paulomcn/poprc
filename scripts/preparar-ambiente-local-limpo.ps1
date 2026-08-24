param(
    [string]$Database = "poprc_local",
    [string]$HostName = "localhost",
    [int]$Port = 5432,
    [string]$Username = "postgres",
    [string]$Password = $env:DB_PASSWORD,
    [switch]$Recreate
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "PostgresTools.ps1")

$projectRoot = Split-Path $PSScriptRoot -Parent
$localConfig = Join-Path $projectRoot "application-local.properties"
$Password = Resolve-DatabasePassword -Password $Password -ProjectRoot $projectRoot
Assert-SafeDatabaseName $Database @("_local")

$psql = Resolve-PostgresTool "psql"
$createdb = Resolve-PostgresTool "createdb"
$dropdb = Resolve-PostgresTool "dropdb"
$databaseQuery = Invoke-WithPgPassword $Password {
    $result = & $psql --host $HostName --port $Port --username $Username --no-password `
        --dbname postgres --tuples-only --no-align `
        --command "SELECT 1 FROM pg_database WHERE datname = '$Database'"
    if ($LASTEXITCODE -ne 0) {
        throw "Nao foi possivel consultar o PostgreSQL."
    }
    ($result | Out-String).Trim()
}
$databaseExists = ($databaseQuery | Out-String).Trim() -eq "1"

if ($databaseExists -and -not $Recreate) {
    throw "O banco $Database ja existe. Use -Recreate para recria-lo com backup e confirmacao."
}

if ($databaseExists -and $Recreate) {
    $confirmation = Read-Host "Digite RECRIAR $Database para fazer backup e apagar o banco local"
    if ($confirmation -ne "RECRIAR $Database") {
        throw "Recriacao cancelada."
    }
    $backupScript = Join-Path $PSScriptRoot "backup-postgres.ps1"
    & $backupScript -Database $Database -HostName $HostName -Port $Port `
        -Username $Username -Password $Password
    if ($LASTEXITCODE -ne 0) {
        throw "O backup falhou; o banco nao foi removido."
    }
    Invoke-WithPgPassword $Password {
        & $dropdb --host $HostName --port $Port --username $Username --no-password `
            --if-exists --force $Database
        if ($LASTEXITCODE -ne 0) {
            throw "Nao foi possivel remover o banco local."
        }
    }
}

Invoke-WithPgPassword $Password {
    & $createdb --host $HostName --port $Port --username $Username --no-password $Database
    if ($LASTEXITCODE -ne 0) {
        throw "Nao foi possivel criar o banco local."
    }
}

$url = "spring.datasource.url=jdbc:postgresql://${HostName}:$Port/$Database"
$lines = if (Test-Path -LiteralPath $localConfig) {
    @(Get-Content -LiteralPath $localConfig)
} else {
    @()
}
$found = $false
$updated = foreach ($line in $lines) {
    if ($line -match '^spring\.datasource\.url=') {
        $found = $true
        $url
    } else {
        $line
    }
}
if (-not $found) {
    $updated += $url
}
$updated | Set-Content -LiteralPath $localConfig -Encoding UTF8

Write-Output "Banco local vazio criado: $Database"
Write-Output "Configuracao local atualizada: $localConfig"
Write-Output "O banco anterior nao foi alterado."
Write-Output "Inicie o sistema e configure o primeiro administrador na tela de login."
