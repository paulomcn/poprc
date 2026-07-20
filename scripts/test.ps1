param(
    [string]$Database = "poprc_test",
    [string]$HostName = "localhost",
    [int]$Port = 5432,
    [string]$Username = "postgres",
    [string]$Password = $env:TEST_DB_PASSWORD,
    [switch]$KeepDatabase
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "PostgresTools.ps1")

Assert-SafeDatabaseName $Database @("_test")

if (-not $Password) {
    $localProperties = Join-Path (Split-Path $PSScriptRoot -Parent) "application-local.properties"
    if (Test-Path -LiteralPath $localProperties) {
        $passwordLine = Get-Content -LiteralPath $localProperties |
            Where-Object { $_ -match '^spring\.datasource\.password=' } |
            Select-Object -First 1
        if ($passwordLine) {
            $Password = $passwordLine.Substring($passwordLine.IndexOf('=') + 1)
        }
    }
}

$dropDb = Resolve-PostgresTool "dropdb"
$createDb = Resolve-PostgresTool "createdb"
$projectRoot = Split-Path $PSScriptRoot -Parent
$databaseCreated = $false
$testExitCode = 1

try {
    Invoke-WithPgPassword $Password {
        & $dropDb --host $HostName --port $Port --username $Username --no-password --if-exists --force $Database
        if ($LASTEXITCODE -ne 0) {
            throw "Nao foi possivel limpar o banco de testes."
        }

        & $createDb --host $HostName --port $Port --username $Username --no-password $Database
        if ($LASTEXITCODE -ne 0) {
            throw "Nao foi possivel criar o banco de testes."
        }
    }
    $databaseCreated = $true

    $previousUrl = $env:TEST_DB_URL
    $previousUsername = $env:TEST_DB_USERNAME
    $previousPassword = $env:TEST_DB_PASSWORD
    try {
        $env:TEST_DB_URL = "jdbc:postgresql://${HostName}:${Port}/${Database}"
        $env:TEST_DB_USERNAME = $Username
        $env:TEST_DB_PASSWORD = $Password

        Push-Location $projectRoot
        try {
            & .\mvnw.cmd test
            $testExitCode = $LASTEXITCODE
        }
        finally {
            Pop-Location
        }
    }
    finally {
        $env:TEST_DB_URL = $previousUrl
        $env:TEST_DB_USERNAME = $previousUsername
        $env:TEST_DB_PASSWORD = $previousPassword
    }

    if ($testExitCode -ne 0) {
        throw "A suite de testes falhou com codigo $testExitCode."
    }
}
finally {
    if ($databaseCreated -and -not $KeepDatabase) {
        Invoke-WithPgPassword $Password {
            & $dropDb --host $HostName --port $Port --username $Username --no-password --if-exists --force $Database
            if ($LASTEXITCODE -ne 0) {
                Write-Warning "Nao foi possivel remover o banco temporario $Database."
            }
        }
    }
}

Write-Output "Suite concluida no banco isolado $Database."
if ($KeepDatabase) {
    Write-Output "Banco preservado porque -KeepDatabase foi informado."
}
