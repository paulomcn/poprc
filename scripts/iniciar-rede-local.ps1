param(
    [int]$BackendPort = 8085,
    [int]$FrontendPort = 5173
)

$ErrorActionPreference = "Stop"
$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot ".."))
$frontendRoot = Join-Path $projectRoot "frontend"
$runtimeRoot = Join-Path $projectRoot ".runtime"
$localConfig = Join-Path $projectRoot "application-local.properties"

function Test-TcpPortInUse([int]$Port) {
    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $connection = $client.ConnectAsync("127.0.0.1", $Port)
        return $connection.Wait(500) -and $client.Connected
    }
    catch {
        return $false
    }
    finally {
        $client.Dispose()
    }
}

if (-not (Test-Path -LiteralPath $localConfig)) {
    throw "Crie application-local.properties a partir do arquivo de exemplo antes de iniciar."
}
if (Test-TcpPortInUse -Port $BackendPort) {
    throw "A porta $BackendPort do backend já está em uso."
}
if (Test-TcpPortInUse -Port $FrontendPort) {
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

$node = (Get-Command "node.exe" -ErrorAction Stop).Source
$vite = Join-Path $frontendRoot "node_modules\vite\bin\vite.js"
if (-not (Test-Path -LiteralPath $vite)) {
    throw "As dependências do frontend não estão instaladas. Execute npm install antes de iniciar."
}
$frontend = Start-Process -FilePath $node `
    -ArgumentList @($vite, "--host", "0.0.0.0", "--port", $FrontendPort, "--strictPort") `
    -WorkingDirectory $frontendRoot `
    -RedirectStandardOutput (Join-Path $runtimeRoot "frontend.log") `
    -RedirectStandardError (Join-Path $runtimeRoot "frontend.err.log") `
    -WindowStyle Hidden -PassThru

@{
    backendPid = $backend.Id
    frontendPid = $frontend.Id
    iniciadoEm = (Get-Date).ToString("o")
} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $runtimeRoot "rede-local.json") -Encoding UTF8

$ip = $null
try {
    $ip = Get-NetIPAddress -AddressFamily IPv4 -ErrorAction Stop `
        | Where-Object { $_.IPAddress -notlike "127.*" -and $_.PrefixOrigin -ne "WellKnown" } `
        | Select-Object -First 1 -ExpandProperty IPAddress
}
catch {
    $ip = [System.Net.Dns]::GetHostAddresses([System.Net.Dns]::GetHostName()) `
        | Where-Object { $_.AddressFamily -eq [System.Net.Sockets.AddressFamily]::InterNetwork -and $_.IPAddressToString -notlike "127.*" } `
        | Select-Object -First 1 -ExpandProperty IPAddressToString
}

Write-Output "Backend iniciado: http://localhost:$BackendPort/actuator/health"
Write-Output "Frontend local: http://localhost:$FrontendPort"
if ($ip) { Write-Output "Acesso na rede: http://${ip}:$FrontendPort" }
Write-Output "Logs: $runtimeRoot"
