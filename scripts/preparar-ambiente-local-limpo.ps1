param(
    [string]$Database = "poprc_local",
    [string]$HostName = "localhost",
    [int]$Port = 5432,
    [string]$Username = "postgres",
    [string]$Password = $env:DB_PASSWORD,
    [string]$UploadDirectory = $(if ($env:APP_UPLOAD_DIR) { $env:APP_UPLOAD_DIR } else { Join-Path $env:USERPROFILE "rc_uploads" }),
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
$databaseRecreated = $false

if ($databaseExists -and -not $Recreate) {
    throw "O banco $Database ja existe. Use -Recreate para recria-lo com backup e confirmacao."
}

if ($databaseExists -and $Recreate) {
    $confirmation = Read-Host "Digite RECRIAR $Database para fazer backup completo e apagar o banco local"
    if ($confirmation -ne "RECRIAR $Database") {
        throw "Recriacao cancelada."
    }
    $backupScript = Join-Path $PSScriptRoot "backup-completo.ps1"
    $backupPackage = & $backupScript -Database $Database -HostName $HostName -Port $Port `
        -Username $Username -Password $Password -UploadDirectory $UploadDirectory |
        Select-Object -Last 1
    if (-not $backupPackage -or -not (Test-Path -LiteralPath $backupPackage)) {
        throw "O pacote de backup completo nao foi confirmado; o banco nao foi removido."
    }
    Write-Output "Backup completo confirmado: $backupPackage"
    Invoke-WithPgPassword $Password {
        & $dropdb --host $HostName --port $Port --username $Username --no-password `
            --if-exists --force $Database
        if ($LASTEXITCODE -ne 0) {
            throw "Nao foi possivel remover o banco local."
        }
    }
    $databaseRecreated = $true
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

if ($databaseRecreated) {
    Write-Output "Banco local recriado vazio: $Database"
    Write-Output "Os uploads foram incluidos no backup e preservados no diretorio original."
} else {
    Write-Output "Banco local vazio criado: $Database"
}
Write-Output "Configuracao local atualizada: $localConfig"
Write-Output "Inicie o sistema e configure o primeiro administrador na tela de login."
