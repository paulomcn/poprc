param(
    [int]$BackendPort = 8085,
    [int]$FrontendPort = 5173
)

$ErrorActionPreference = "Stop"
$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot ".."))
$frontendRoot = Join-Path $projectRoot "frontend"
$runtimeRoot = Join-Path $projectRoot ".runtime"
$localConfig = Join-Path $projectRoot "application-local.properties"

if (-not (Test-Path -LiteralPath $localConfig)) {
    throw "Crie application-local.properties a partir do arquivo de exemplo antes de iniciar."
}
if (Get-NetTCPConnection -State Listen -LocalPort $BackendPort -ErrorAction SilentlyContinue) {
    throw "A porta $BackendPort do backend já está em uso."
}
if (Get-NetTCPConnection -State Listen -LocalPort $FrontendPort -ErrorAction SilentlyContinue) {
    throw "A porta $FrontendPort do frontend já está em uso."
}

New-Item -ItemType Directory -Path $runtimeRoot -Force | Out-Null
$env:SPRING_CONFIG_ADDITIONAL_LOCATION = "optional:file:$($localConfig.Replace('\', '/'))"
$env:SERVER_PORT = $BackendPort.ToString()
$env:SERVER_ADDRESS = "0.0.0.0"

$backend = Start-Process -FilePath (Join-Path $projectRoot "mvnw.cmd") `
    -ArgumentList @("spring-boot:run") `
    -WorkingDirectory $projectRoot `
    -RedirectStandardOutput (Join-Path $runtimeRoot "backend.log") `
    -RedirectStandardError (Join-Path $runtimeRoot "backend.err.log") `
    -WindowStyle Hidden -PassThru

$frontend = Start-Process -FilePath "npm.cmd" `
    -ArgumentList @("run", "dev", "--", "--host", "0.0.0.0", "--port", $FrontendPort) `
    -WorkingDirectory $frontendRoot `
    -RedirectStandardOutput (Join-Path $runtimeRoot "frontend.log") `
    -RedirectStandardError (Join-Path $runtimeRoot "frontend.err.log") `
    -WindowStyle Hidden -PassThru

@{
    backendPid = $backend.Id
    frontendPid = $frontend.Id
    iniciadoEm = (Get-Date).ToString("o")
} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $runtimeRoot "rede-local.json") -Encoding UTF8

$ip = Get-NetIPAddress -AddressFamily IPv4 `
    | Where-Object { $_.IPAddress -notlike "127.*" -and $_.PrefixOrigin -ne "WellKnown" } `
    | Select-Object -First 1 -ExpandProperty IPAddress

Write-Output "Backend iniciado: http://localhost:$BackendPort/actuator/health"
Write-Output "Frontend local: http://localhost:$FrontendPort"
if ($ip) { Write-Output "Acesso na rede: http://${ip}:$FrontendPort" }
Write-Output "Logs: $runtimeRoot"
