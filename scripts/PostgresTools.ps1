Set-StrictMode -Version Latest

function Resolve-PostgresTool {
    param([Parameter(Mandatory = $true)][string]$Name)

    $command = Get-Command $Name -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $installRoot = Join-Path $env:ProgramFiles "PostgreSQL"
    if (Test-Path $installRoot) {
        $candidate = Get-ChildItem $installRoot -Directory |
            Where-Object { $_.Name -match '^\d+' } |
            Sort-Object { [int]([regex]::Match($_.Name, '^\d+').Value) } -Descending |
            ForEach-Object { Join-Path $_.FullName "bin\$Name.exe" } |
            Where-Object { Test-Path $_ } |
            Select-Object -First 1
        if ($candidate) {
            return $candidate
        }
    }

    throw "Ferramenta PostgreSQL '$Name' nao encontrada. Instale o cliente PostgreSQL ou adicione-o ao PATH."
}

function Assert-SafeDatabaseName {
    param(
        [Parameter(Mandatory = $true)][string]$Database,
        [Parameter(Mandatory = $true)][string[]]$AllowedSuffixes
    )

    if ($Database -notmatch '^[A-Za-z0-9_]+$') {
        throw "Nome de banco invalido: $Database"
    }
    if (-not ($AllowedSuffixes | Where-Object { $Database.EndsWith($_) })) {
        throw "Operacao recusada para '$Database'. Sufixos permitidos: $($AllowedSuffixes -join ', ')."
    }
}

function Invoke-WithPgPassword {
    param(
        [string]$Password,
        [Parameter(Mandatory = $true)][scriptblock]$Action
    )

    $previous = $env:PGPASSWORD
    try {
        if ($Password) {
            $env:PGPASSWORD = $Password
        }
        & $Action
    }
    finally {
        $env:PGPASSWORD = $previous
    }
}
