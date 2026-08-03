param(
    [Parameter(Mandatory = $true)][string]$PackageFile,
    [string]$TargetDatabase = "poprc_restore_test",
    [string]$HostName = "localhost",
    [int]$Port = 5432,
    [string]$Username = "postgres",
    [string]$Password = $env:DB_PASSWORD,
    [string]$RestoreUploadsTo = (Join-Path $PSScriptRoot "..\backups\restores\poprc_restore_test\rc_uploads"),
    [switch]$Force
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "PostgresTools.ps1")
Assert-SafeDatabaseName $TargetDatabase @("_restore_test")

$projectRoot = [System.IO.Path]::GetFullPath((Split-Path $PSScriptRoot -Parent))
$package = (Resolve-Path -LiteralPath $PackageFile).Path
$restoreRoot = [System.IO.Path]::GetFullPath((Join-Path $projectRoot "backups\restores"))
$uploadTarget = [System.IO.Path]::GetFullPath($RestoreUploadsTo)
$liveUploads = [System.IO.Path]::GetFullPath((Join-Path $env:USERPROFILE "rc_uploads"))

if ($uploadTarget.Equals($liveUploads, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Restauracao recusada sobre os uploads ativos. Use uma pasta isolada."
}
if (-not $uploadTarget.StartsWith($restoreRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "A pasta restaurada deve permanecer dentro de $restoreRoot."
}

$stage = Join-Path $restoreRoot "$TargetDatabase.stage"
New-Item -ItemType Directory -Path $restoreRoot -Force | Out-Null

try {
    if (Test-Path -LiteralPath $stage) {
        $resolvedStage = [System.IO.Path]::GetFullPath($stage)
        if (-not $resolvedStage.StartsWith($restoreRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
            throw "Pasta temporaria de restauracao invalida."
        }
        Remove-Item -LiteralPath $resolvedStage -Recurse -Force
    }
    New-Item -ItemType Directory -Path $stage -Force | Out-Null
    Expand-Archive -LiteralPath $package -DestinationPath $stage -Force

    $manifestPath = Join-Path $stage "manifest.json"
    if (-not (Test-Path -LiteralPath $manifestPath)) {
        throw "Pacote sem manifest.json."
    }
    $manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
    if ($manifest.formatVersion -ne 1) {
        throw "Versao de pacote de recuperacao nao suportada."
    }

    foreach ($entry in $manifest.files) {
        $file = [System.IO.Path]::GetFullPath((Join-Path $stage $entry.path))
        if (-not $file.StartsWith([System.IO.Path]::GetFullPath($stage), [System.StringComparison]::OrdinalIgnoreCase)) {
            throw "Caminho invalido no manifesto: $($entry.path)"
        }
        if (-not (Test-Path -LiteralPath $file -PathType Leaf)) {
            throw "Arquivo ausente no pacote: $($entry.path)"
        }
        $hash = (Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash.ToLowerInvariant()
        if ($hash -ne $entry.sha256) {
            throw "Falha de integridade no arquivo: $($entry.path)"
        }
    }

    $dump = Join-Path $stage $manifest.databaseDump
    & (Join-Path $PSScriptRoot "restore-postgres.ps1") `
        -BackupFile $dump `
        -TargetDatabase $TargetDatabase `
        -HostName $HostName `
        -Port $Port `
        -Username $Username `
        -Password $Password `
        -Force:$Force

    if (Test-Path -LiteralPath $uploadTarget) {
        Remove-Item -LiteralPath $uploadTarget -Recurse -Force
    }
    New-Item -ItemType Directory -Path $uploadTarget -Force | Out-Null
    $uploadsSource = Join-Path $stage "uploads"
    if (Test-Path -LiteralPath $uploadsSource) {
        Get-ChildItem -LiteralPath $uploadsSource -Force |
            Copy-Item -Destination $uploadTarget -Recurse -Force
    }

    Write-Output "Banco restaurado: $TargetDatabase"
    Write-Output "Uploads restaurados: $uploadTarget"
    Write-Output "Use APP_UPLOAD_DIR=$uploadTarget ao iniciar o backend de validacao."
}
finally {
    $resolvedStage = [System.IO.Path]::GetFullPath($stage)
    if ((Test-Path -LiteralPath $resolvedStage) -and $resolvedStage.StartsWith($restoreRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        Remove-Item -LiteralPath $resolvedStage -Recurse -Force
    }
}
