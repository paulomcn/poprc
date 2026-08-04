$ErrorActionPreference = "Stop"
$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot ".."))
$stateFile = Join-Path $projectRoot ".runtime\rede-local.json"

if (-not (Test-Path -LiteralPath $stateFile)) {
    Write-Output "Nenhuma execução iniciada pelo script foi encontrada."
    exit 0
}

$state = Get-Content -LiteralPath $stateFile -Raw | ConvertFrom-Json
function Stop-ProcessTree([int]$ProcessId) {
    Get-CimInstance Win32_Process -Filter "ParentProcessId = $ProcessId" -ErrorAction SilentlyContinue `
        | ForEach-Object { Stop-ProcessTree -ProcessId $_.ProcessId }
    $processo = Get-Process -Id $ProcessId -ErrorAction SilentlyContinue
    if ($processo) { Stop-Process -Id $ProcessId }
}

@($state.backendPid, $state.frontendPid) | ForEach-Object {
    if ($_ -is [int] -or $_ -is [long]) { Stop-ProcessTree -ProcessId $_ }
}
Remove-Item -LiteralPath $stateFile
Write-Output "Frontend e backend locais foram encerrados."
