param(
    [int]$Port = 6966,
    [string]$SuperUser = 'postgres'
)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

$psqlCommand = Get-Command psql.exe -ErrorAction SilentlyContinue
$psql = if ($psqlCommand) { $psqlCommand.Source } else {
    Get-ChildItem -Path "$env:ProgramFiles\PostgreSQL\*\bin\psql.exe" -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
}
if (-not $psql) { throw 'psql sa nenasiel. Nainstalujte PostgreSQL.' }

function New-RandomHex([int]$bytes) {
    $buffer = New-Object byte[] $bytes
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($buffer)
    -join ($buffer | ForEach-Object { $_.ToString('x2') })
}

# Heslo aplikácie a JWT kľúč sa vygenerujú iba raz a uložia do .env.local (je v .gitignore).
$envFile = Join-Path $PSScriptRoot '.env.local'
$values = [ordered]@{}
if (Test-Path -LiteralPath $envFile) {
    Get-Content -LiteralPath $envFile | Where-Object { $_ -match '^\s*([A-Z_]+)=(.*)$' } |
        ForEach-Object { $values[$Matches[1]] = $Matches[2] }
}
if (-not $values['DB_PASSWORD']) { $values['DB_PASSWORD'] = New-RandomHex 24 }
if (-not $values['JWT_SECRET']) { $values['JWT_SECRET'] = New-RandomHex 32 }
$values['DB_URL'] = "jdbc:postgresql://localhost:$Port/fx"
$values['DB_USERNAME'] = 'fx_app'

Write-Host "Vytvaram pouzivatela fx_app a databazu fx na localhost:$Port." -ForegroundColor Cyan
Write-Host "psql si vypyta heslo superusera '$SuperUser' (zadane pri instalacii PostgreSQL)."
& $psql -h localhost -p $Port -U $SuperUser -d postgres -v ON_ERROR_STOP=1 `
    -v "app_password=$($values['DB_PASSWORD'])" -f (Join-Path $PSScriptRoot 'db\create-local-db.sql')
if ($LASTEXITCODE -ne 0) { throw 'Vytvorenie databazy zlyhalo.' }

$values.GetEnumerator() | ForEach-Object { "$($_.Key)=$($_.Value)" } |
    Set-Content -LiteralPath $envFile -Encoding ascii
Write-Host "Hotovo. Pripojenie je ulozene v .env.local." -ForegroundColor Green
Write-Host 'Test:   .\start-local.ps1 -Test'
Write-Host 'Server: .\start-local.ps1'
