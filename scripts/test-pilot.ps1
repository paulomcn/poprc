param(
    [string]$Database = "poprc_pilot_test",
    [string]$HostName = "localhost",
    [int]$Port = 5432,
    [string]$Username = "postgres",
    [string]$Password = $env:TEST_DB_PASSWORD,
    [switch]$KeepDatabase
)

$ErrorActionPreference = "Stop"

$criticalTests = @(
    "FluxoOperacionalIntegrationTest",
    "DocumentoAssinaturaLogImutabilidadeIntegrationTest",
    "OrdemRetiradaDocumentoImutabilidadeIntegrationTest",
    "EstoqueConcorrenciaIntegrationTest",
    "SecurityAuthorizationIntegrationTest"
) -join ","

$testScript = Join-Path $PSScriptRoot "test.ps1"
$parameters = @{
    Database = $Database
    HostName = $HostName
    Port = $Port
    Username = $Username
    TestClasses = $criticalTests
}
if ($Password) {
    $parameters.Password = $Password
}
if ($KeepDatabase) {
    $parameters.KeepDatabase = $true
}

Write-Output "Executando regressao critica do piloto em banco isolado: $Database"
& $testScript @parameters
