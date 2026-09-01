param(
    [string]$Database = "poprc_local",
    [string]$HostName = "localhost",
    [int]$Port = 5432,
    [string]$Username = "postgres",
    [string]$Password = $env:DB_PASSWORD,
    [string]$ExpectedInventoryValue = "",
    [int]$ExpectedImportedOrTabs = 0
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "PostgresTools.ps1")

$projectRoot = Split-Path $PSScriptRoot -Parent
$Password = Resolve-DatabasePassword -Password $Password -ProjectRoot $projectRoot
$psql = Resolve-PostgresTool "psql"

$query = @"
with materiais_ativos as (
    select
        m.id,
        m.nome,
        m.tipo_controle,
        case
            when m.tipo_controle in ('FRACIONADO', 'METRAGEM', 'BOBINA', 'ROLO')
                then coalesce(m.metragem_disponivel, 0)
            else coalesce(m.quantidade_disponivel, 0)::numeric
        end as saldo_geral,
        case
            when m.tipo_controle in ('FRACIONADO', 'METRAGEM', 'BOBINA', 'ROLO')
                then coalesce(m.metragem_reservada, 0)
            else coalesce(m.quantidade_reservada, 0)::numeric
        end as reservado_geral,
        coalesce(m.custo_medio, 0) as custo_medio
    from materiais m
    where m.ativo = true
), saldos_locais as (
    select
        material.id,
        coalesce(sum(case
            when material.tipo_controle in ('FRACIONADO', 'METRAGEM', 'BOBINA', 'ROLO')
                then coalesce(saldo.metragem_disponivel, 0)
            else coalesce(saldo.quantidade_disponivel, 0)::numeric
        end), 0) as saldo_local,
        coalesce(sum(case
            when material.tipo_controle in ('FRACIONADO', 'METRAGEM', 'BOBINA', 'ROLO')
                then coalesce(saldo.metragem_reservada, 0)
            else coalesce(saldo.quantidade_reservada, 0)::numeric
        end), 0) as reservado_local
    from materiais_ativos material
    left join saldos_materiais_locais saldo on saldo.material_id = material.id
    group by material.id
), faltas as (
    select
        material.nome,
        sum(retirada.quantidade_faltante) as quantidade
    from importacoes_retiradas_planilha retirada
    join materiais material on material.id = retirada.material_id
    where retirada.quantidade_faltante > 0
    group by material.nome
)
select json_build_object(
    'materiaisAtivos', (select count(*) from materiais_ativos),
    'valorTotal', (select round(sum(saldo_geral * custo_medio), 2) from materiais_ativos),
    'locaisAtivos', (select count(*) from locais_estoque where ativo = true),
    'divergenciasSaldoLocal', (
        select count(*)
        from materiais_ativos material
        join saldos_locais saldo using (id)
        where abs(material.saldo_geral - saldo.saldo_local) > 0.0005
    ),
    'divergenciasReservaLocal', (
        select count(*)
        from materiais_ativos material
        join saldos_locais saldo using (id)
        where abs(material.reservado_geral - saldo.reservado_local) > 0.0005
    ),
    'materiaisReservados', (
        select count(*) from materiais_ativos where reservado_geral > 0
    ),
    'movimentacoes', (select count(*) from movimentacoes_estoque),
    'abasOrImportadas', (
        select count(distinct aba_origem) from importacoes_retiradas_planilha
    ),
    'orsImportadas', (
        select count(distinct ordem_retirada_id)
        from importacoes_retiradas_planilha
        where ordem_retirada_id is not null
    ),
    'vinculosOrIncompletos', (
        select count(*)
        from importacoes_retiradas_planilha retirada
        left join ordens_retirada ordem on ordem.id = retirada.ordem_retirada_id
        left join ordens_servico os on os.id = ordem.ordem_servico_id
        left join contratos contrato on contrato.id = os.contrato_id
        where retirada.comarca_id is null
           or retirada.material_id is null
           or retirada.ordem_retirada_id is null
           or os.id is null
           or contrato.id is null
    ),
    'reconciliacoesPlanilha', (
        select count(*)
        from reconciliacoes_retiradas_planilha
        where origem = 'PLANILHA'
    ),
    'faltas', coalesce((
        select json_agg(json_build_object('material', nome, 'quantidade', quantidade) order by nome)
        from faltas
    ), '[]'::json)
)::text;
"@

$raw = Invoke-WithPgPassword $Password {
    & $psql --host $HostName --port $Port --username $Username --dbname $Database `
        --no-password --tuples-only --no-align --command $query
    if ($LASTEXITCODE -ne 0) {
        throw "A auditoria SQL terminou com codigo $LASTEXITCODE."
    }
}

$json = $raw | Where-Object { $_ -and $_.Trim() } | Select-Object -Last 1
if (-not $json) {
    throw "A auditoria nao retornou resultado."
}

$resultado = $json | ConvertFrom-Json
$falhas = [System.Collections.Generic.List[string]]::new()

if ([int]$resultado.divergenciasSaldoLocal -ne 0) {
    $falhas.Add("Existem divergencias entre o estoque geral e os depositos.")
}
if ([int]$resultado.divergenciasReservaLocal -ne 0) {
    $falhas.Add("Existem divergencias entre as reservas gerais e locais.")
}
if ([int]$resultado.vinculosOrIncompletos -ne 0) {
    $falhas.Add("Existem retiradas importadas sem comarca, material, OR, OS ou contrato.")
}
if ($ExpectedImportedOrTabs -gt 0 -and [int]$resultado.abasOrImportadas -ne $ExpectedImportedOrTabs) {
    $falhas.Add("Esperadas $ExpectedImportedOrTabs abas de OR, mas o banco possui $($resultado.abasOrImportadas).")
}
if ($ExpectedInventoryValue) {
    $culture = [System.Globalization.CultureInfo]::InvariantCulture
    $esperado = [decimal]::Parse($ExpectedInventoryValue, $culture)
    $atual = [decimal]$resultado.valorTotal
    if ([math]::Abs($atual - $esperado) -gt 0.005) {
        $falhas.Add("Valor esperado $esperado, mas o estoque soma $atual.")
    }
}

[pscustomobject]@{
    Banco = $Database
    MateriaisAtivos = [int]$resultado.materiaisAtivos
    ValorTotal = [decimal]$resultado.valorTotal
    LocaisAtivos = [int]$resultado.locaisAtivos
    DivergenciasSaldoLocal = [int]$resultado.divergenciasSaldoLocal
    MateriaisReservados = [int]$resultado.materiaisReservados
    AbasOrImportadas = [int]$resultado.abasOrImportadas
    OrsImportadas = [int]$resultado.orsImportadas
    VinculosOrIncompletos = [int]$resultado.vinculosOrIncompletos
    ReconciliacoesPlanilha = [int]$resultado.reconciliacoesPlanilha
    Movimentacoes = [int]$resultado.movimentacoes
    MateriaisEmFalta = @($resultado.faltas).Count
} | Format-List

if (@($resultado.faltas).Count -gt 0) {
    Write-Output "Faltas historicas registradas:"
    $resultado.faltas | ForEach-Object {
        Write-Output "- $($_.material): $($_.quantidade)"
    }
}

if ($falhas.Count -gt 0) {
    throw ($falhas -join " ")
}

Write-Output "Auditoria concluida sem divergencias estruturais."
