param(
    [switch]$Test,
    [string]$EnvFile = '.env.local',
    [string]$Profiles = 'local,seed'
)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

$envFile = Join-Path $PSScriptRoot $EnvFile
if (-not (Test-Path -LiteralPath $envFile)) { throw "Chyba $EnvFile. Pre lokalnu DB spustite .\setup-local-db.ps1" }
Get-Content -LiteralPath $envFile | Where-Object { $_ -match '^\s*([A-Z_]+)=(.*)$' } |
    ForEach-Object { Set-Item -Path "env:$($Matches[1])" -Value $Matches[2] }

$mavenCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue
if ($mavenCommand) { $maven = $mavenCommand.Source }
else {
    $maven = Get-ChildItem -Path "$env:ProgramFiles\JetBrains\*\plugins\maven\lib\maven3\bin\mvn.cmd" -ErrorAction SilentlyContinue |
        Select-Object -First 1 -ExpandProperty FullName
}
if (-not $maven) { throw 'Maven sa nenasiel. Nainstalujte Maven alebo pouzite Maven v IntelliJ.' }

if ($Test) {
    Write-Host "Spustam PostgreSQL test proti $env:DB_URL" -ForegroundColor Cyan
    $env:RUN_POSTGRES_TESTS = 'true'
    & $maven -B test '-Dtest=UserRepositoryPostgresTest' '-Dsurefire.failIfNoSpecifiedTests=false'
} else {
    Write-Host "Startujem backend s PostgreSQL ($env:DB_URL), profily: '$Profiles'." -ForegroundColor Green
    Write-Host 'Frontend v dalsom terminali: cd frontend; npm start  ->  http://localhost:4200'
    $runArgs = @('spring-boot:run')
    if ($Profiles) { $runArgs += "-Dspring-boot.run.profiles=$Profiles" }
    & $maven @runArgs
}
exit $LASTEXITCODE
