param([switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

if (-not $env:JAVA_HOME) {
    $jdk = Get-ChildItem -Path "$env:ProgramFiles\Java" -Directory -Filter 'jdk-*' -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if ($jdk) { $env:JAVA_HOME = $jdk.FullName }
}
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\java.exe' } else { (Get-Command java -ErrorAction Stop).Source }
if (-not (Test-Path -LiteralPath $java)) { throw 'JDK 17 alebo novsi sa nenasiel. Nastavte JAVA_HOME.' }

if (-not $SkipBuild) {
    $npm = (Get-Command npm.cmd -ErrorAction Stop).Source
    $mavenCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($mavenCommand) { $maven = $mavenCommand.Source }
    else {
        $maven = Get-ChildItem -Path "$env:ProgramFiles\JetBrains\*\plugins\maven\lib\maven3\bin\mvn.cmd" -ErrorAction SilentlyContinue |
            Select-Object -First 1 -ExpandProperty FullName
    }
    if (-not $maven) { throw 'Maven sa nenasiel. Nainstalujte Maven alebo pouzite Maven v IntelliJ.' }
    Write-Host '1/3 Instalujem a zostavujem frontend...' -ForegroundColor Cyan
    Push-Location (Join-Path $PSScriptRoot 'frontend')
    try {
        $lockHash = (Get-FileHash -LiteralPath 'package-lock.json' -Algorithm SHA256).Hash
        $stamp = 'node_modules\.hodinovy-majster-lock'
        $installedHash = if (Test-Path -LiteralPath $stamp) { (Get-Content -LiteralPath $stamp -Raw).Trim() } else { '' }
        if ($installedHash -ne $lockHash -or -not (Test-Path -LiteralPath 'node_modules\vite\package.json')) {
            & $npm ci --no-fund --no-audit
            if ($LASTEXITCODE -ne 0) { throw 'Instalacia frontend zavislosti zlyhala.' }
            Set-Content -LiteralPath $stamp -Value $lockHash
        }
        & $npm run build
        if ($LASTEXITCODE -ne 0) { throw 'Frontend build zlyhal.' }
    } finally { Pop-Location }
    Write-Host '2/3 Balim Spring Boot s frontendom a demo profilom...' -ForegroundColor Cyan
    & $maven -B -Pdemo -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw 'Maven build zlyhal.' }
}
$jar = Join-Path $PSScriptRoot 'target\fx-0.0.1-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $jar)) { throw 'Najprv spustite skript bez parametra -SkipBuild.' }
Write-Host '3/3 Startujem ukazku. Po hlaskach Started FxApplication a priprave dat otvorte:' -ForegroundColor Green
Write-Host 'http://localhost:8080' -ForegroundColor Green
Write-Host 'Zakaznik: zakaznik@demo.sk | Poskytovatel: majster@demo.sk | Heslo: DemoHeslo123!'
Write-Host 'Fiktivne data su iba v pamati a po restarte sa vymazu. Zastavenie: Ctrl+C.'
& $java -jar $jar --spring.profiles.active=demo
exit $LASTEXITCODE
