param(
    [string]$Database = "poprc_local",
    [string]$HostName = "localhost",
    [int]$Port = 5432,
    [string]$Username = "postgres",
    [string]$Password = $env:DB_PASSWORD,
    [string]$ContratoPreservado = "0001",
    [string]$UploadDirectory = $(if ($env:APP_UPLOAD_DIR) { $env:APP_UPLOAD_DIR } else { Join-Path $env:USERPROFILE "rc_uploads" }),
    [string]$BackupPackage,
    [string]$ConfirmToken,
    [switch]$Execute
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "PostgresTools.ps1")

$projectRoot = [System.IO.Path]::GetFullPath((Split-Path $PSScriptRoot -Parent))
$Password = Resolve-DatabasePassword -Password $Password -ProjectRoot $projectRoot
Assert-SafeDatabaseName $Database @("_local")

if ($ContratoPreservado -notmatch '^[A-Za-z0-9._-]+$') {
    throw "Numero de contrato invalido: $ContratoPreservado"
}

$psql = Resolve-PostgresTool "psql"
$expectedToken = "LIMPAR $Database PRESERVAR $ContratoPreservado"

if (-not $Execute) {
    Write-Output "Simulacao: nenhum dado foi alterado."
    Write-Output "Banco: $Database"
    Write-Output "Contrato preservado: $ContratoPreservado"
    Write-Output "Funcionarios, atividades padrao, configuracoes e locais de estoque serao preservados."
    Write-Output "Para executar, use -Execute -ConfirmToken '$expectedToken'."
    exit 0
}

if ($ConfirmToken -ne $expectedToken) {
    throw "Confirmacao invalida. Use exatamente: $expectedToken"
}

$contratoExiste = Invoke-WithPgPassword $Password {
    & $psql --host $HostName --port $Port --username $Username --no-password `
        --dbname $Database --tuples-only --no-align `
        --command "SELECT count(*) FROM contratos WHERE contrato = '$ContratoPreservado';"
}
if ($LASTEXITCODE -ne 0 -or ($contratoExiste | Out-String).Trim() -ne "1") {
    throw "O contrato $ContratoPreservado nao foi encontrado de forma unica. Nenhum dado foi alterado."
}

if ($BackupPackage) {
    $resolvedBackup = (Resolve-Path -LiteralPath $BackupPackage).Path
} else {
    $resolvedBackup = & (Join-Path $PSScriptRoot "backup-completo.ps1") `
        -Database $Database -HostName $HostName -Port $Port -Username $Username `
        -Password $Password -UploadDirectory $UploadDirectory |
        Select-Object -Last 1
}
if (-not $resolvedBackup -or -not (Test-Path -LiteralPath $resolvedBackup -PathType Leaf)) {
    throw "O pacote de backup completo nao foi confirmado. Nenhum dado foi alterado."
}

$cleanupSql = @'
BEGIN;
TRUNCATE TABLE
    as_builts,
    atividade_comarca_fotos,
    atividades_comarca,
    comarca_materiais,
    comarcas,
    documentos_assinaturas_log,
    documentos_internos,
    evidencias_foto,
    faturamentos,
    historico_status_os,
    importacoes_estoque_planilha_itens,
    importacoes_estoque_planilha,
    importacoes_notas_fiscais_itens,
    importacoes_notas_fiscais,
    importacoes_retiradas_planilha,
    logs_operacoes_sensiveis,
    materiais_projeto,
    movimentacoes_estoque,
    notificacoes_operacionais,
    ordem_retirada_alocacoes,
    ordem_retirada_itens,
    ordens_retirada_documentos,
    ordens_retirada,
    ordens_servico,
    prestacoes_contas,
    projetos_membros,
    projetos,
    registros_ponto,
    saldos_materiais_locais,
    spring_session_attributes,
    spring_session,
    unidades_estoque_rastreaveis,
    viagens,
    materiais
RESTART IDENTITY CASCADE;
DELETE FROM contratos WHERE contrato <> '__CONTRATO_PRESERVADO__';
COMMIT;
'@
$cleanupSql = $cleanupSql.Replace('__CONTRATO_PRESERVADO__', $ContratoPreservado)

Invoke-WithPgPassword $Password {
    & $psql --host $HostName --port $Port --username $Username --no-password `
        --dbname $Database --set ON_ERROR_STOP=1 `
        --command $cleanupSql
    if ($LASTEXITCODE -ne 0) {
        throw "A limpeza transacional falhou. Verifique o banco antes de continuar."
    }
}

$archiveRoot = [System.IO.Path]::GetFullPath((Join-Path $projectRoot "backups\dados-teste-arquivados"))
$resolvedUploads = [System.IO.Path]::GetFullPath($UploadDirectory)
$driveRoot = [System.IO.Path]::GetPathRoot($resolvedUploads)
if ($resolvedUploads.Equals($driveRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Diretorio de uploads inseguro para arquivamento: $resolvedUploads"
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$uploadArchive = Join-Path $archiveRoot "$Database-$timestamp\uploads"
New-Item -ItemType Directory -Path $uploadArchive -Force | Out-Null
if (Test-Path -LiteralPath $resolvedUploads) {
    Get-ChildItem -LiteralPath $resolvedUploads -Force |
        Move-Item -Destination $uploadArchive -Force
} else {
    New-Item -ItemType Directory -Path $resolvedUploads -Force | Out-Null
}

$validationSql = @'
SELECT 'funcionarios', count(*) FROM funcionarios
UNION ALL SELECT 'contratos', count(*) FROM contratos
UNION ALL SELECT 'contrato_preservado', count(*) FROM contratos WHERE contrato = '__CONTRATO_PRESERVADO__'
UNION ALL SELECT 'materiais', count(*) FROM materiais
UNION ALL SELECT 'projetos', count(*) FROM projetos
UNION ALL SELECT 'ordens_servico', count(*) FROM ordens_servico
UNION ALL SELECT 'ordens_retirada', count(*) FROM ordens_retirada
UNION ALL SELECT 'comarcas', count(*) FROM comarcas
UNION ALL SELECT 'movimentacoes_estoque', count(*) FROM movimentacoes_estoque
ORDER BY 1;
'@
$validationSql = $validationSql.Replace('__CONTRATO_PRESERVADO__', $ContratoPreservado)

Write-Output "Limpeza operacional concluida."
Write-Output "Backup utilizado: $resolvedBackup"
Write-Output "Uploads de teste arquivados em: $uploadArchive"
Invoke-WithPgPassword $Password {
    & $psql --host $HostName --port $Port --username $Username --no-password `
        --dbname $Database --tuples-only --no-align --field-separator "=" `
        --command $validationSql
}
