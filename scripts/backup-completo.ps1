param(
    [string]$Database = "poprc",
    [string]$HostName = "localhost",
    [int]$Port = 5432,
    [string]$Username = "postgres",
    [string]$Password = $env:DB_PASSWORD,
    [string]$UploadDirectory = $(if ($env:APP_UPLOAD_DIR) { $env:APP_UPLOAD_DIR } else { Join-Path $env:USERPROFILE "rc_uploads" }),
    [string]$OutputDirectory = (Join-Path $PSScriptRoot "..\backups")
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path $PSScriptRoot -Parent
$outputRoot = [System.IO.Path]::GetFullPath($OutputDirectory)
New-Item -ItemType Directory -Path $outputRoot -Force | Out-Null

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$packageName = "$Database-completo-$timestamp"
$stage = Join-Path $outputRoot "$packageName.stage"
$archive = Join-Path $outputRoot "$packageName.zip"

try {
    New-Item -ItemType Directory -Path $stage -Force | Out-Null
    $dump = & (Join-Path $PSScriptRoot "backup-postgres.ps1") `
        -Database $Database `
        -HostName $HostName `
        -Port $Port `
        -Username $Username `
        -Password $Password `
        -OutputDirectory $stage
    $dump = [System.IO.Path]::GetFullPath(($dump | Select-Object -Last 1))

    $uploadsStage = Join-Path $stage "uploads"
    New-Item -ItemType Directory -Path $uploadsStage -Force | Out-Null
    if (Test-Path -LiteralPath $UploadDirectory) {
        Get-ChildItem -LiteralPath $UploadDirectory -Force |
            Copy-Item -Destination $uploadsStage -Recurse -Force
    }

    $arquivos = Get-ChildItem -LiteralPath $stage -File -Recurse |
        Sort-Object FullName |
        ForEach-Object {
            [ordered]@{
                path = [System.IO.Path]::GetRelativePath($stage, $_.FullName).Replace('\', '/')
                size = $_.Length
                sha256 = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
            }
        }

    $manifest = [ordered]@{
        formatVersion = 1
        createdAt = (Get-Date).ToUniversalTime().ToString("o")
        database = $Database
        databaseDump = [System.IO.Path]::GetRelativePath($stage, $dump).Replace('\', '/')
        uploadSource = [System.IO.Path]::GetFullPath($UploadDirectory)
        fileCount = @($arquivos).Count
        totalBytes = (@($arquivos) | Measure-Object -Property size -Sum).Sum
        files = @($arquivos)
    }
    $manifest | ConvertTo-Json -Depth 6 |
        Set-Content -LiteralPath (Join-Path $stage "manifest.json") -Encoding utf8

    Compress-Archive -Path (Join-Path $stage "*") -DestinationPath $archive -CompressionLevel Optimal
    Write-Output $archive
}
finally {
    $resolvedStage = [System.IO.Path]::GetFullPath($stage)
    if ((Test-Path -LiteralPath $resolvedStage) -and $resolvedStage.StartsWith($outputRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        Remove-Item -LiteralPath $resolvedStage -Recurse -Force
    }
}
