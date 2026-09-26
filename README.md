# Hodinový majster – Spring Boot backend

Frontend pripravuje spolupracovník v Angulari. Backend: Java 17+, Spring Boot 3.5.5, PostgreSQL.

## Podklady pre frontend

- [API návod](docs/API.md)
- [OpenAPI 3.0.3](docs/openapi.json)
- [TypeScript typy](docs/api-types.ts)
- [Ukážkové dáta](docs/examples.json)
- [TODO a odložená databáza](TODO.md)

## Stav overenia

Entity a REST API sú implementované. Zostavenie a testy zatiaľ nie sú potvrdené:
sandbox blokuje terminálový prístup k OneDrive a žiadosť o testy mimo sandboxu bola zamietnutá.
PostgreSQL na školskom serveri je podľa dohody odložené; lokálna databáza sa nevytvára.

## Testy bez databázy

Z koreňa projektu s Mavenom a JDK:

```powershell
mvn test
```

Unit a MVC testy pokrývajú ceny, vlastníctvo, stavy, registráciu, JWT, validáciu a CORS.
UserRepositoryPostgresTest je predvolene preskočený; aktivuje ho RUN_POSTGRES_TESTS=true.
V IntelliJ možno použiť vstavaný Maven cez Maven → Lifecycle → test.

## Spustenie po príprave školskej databázy

V termináli v koreňovom priečinku nastavte pripojenie (SERVER/DATABAZA/POUZIVATEL nahradiť):

```powershell
$env:DB_URL = 'jdbc:postgresql://SERVER:5432/DATABAZA'
$env:DB_USERNAME = 'POUZIVATEL'
$dbCredential = Get-Credential -UserName $env:DB_USERNAME -Message 'Heslo pre PostgreSQL'
$env:DB_PASSWORD = $dbCredential.GetNetworkCredential().Password
# Náhodný podpisovací kľúč; jeho zmena zneplatní vydané tokeny.
$jwtBytes = New-Object byte[] 48
$jwtRng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRng.GetBytes($jwtBytes)
$jwtRng.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
$env:FRONTEND_ORIGIN = 'http://localhost:4200'
mvn spring-boot:run
```

JWT_SECRET musí mať aspoň 32 bajtov; pri nasadení ho uchovajte v konfigurácii servera.
Tajomstvá neukladajte do repozitára. Po práci ich odstráňte z terminálu:

```powershell
Remove-Item Env:DB_PASSWORD, Env:JWT_SECRET
```

Predvolený profil iba overuje schému (validate). Profil local používa Hibernate update
na vytvorenie/aktualizáciu tabuliek users, provider_profiles, categories, services a orders.
Použite ho iba nad vlastnou vývojovou databázou po dohode so správcom:

```powershell
mvn spring-boot:run '-Dspring-boot.run.profiles=local'
```

VPN, SSL a migrácie sú na dohodu so správcom. Backend používa port 8080.
Úspešný štart obsahuje v logu Started FxApplication.

## Odložený PostgreSQL test

S DB_URL/DB_USERNAME/DB_PASSWORD pre samostatnú vývojovú/testovaciu databázu:

```powershell
$env:RUN_POSTGRES_TESTS = 'true'
mvn '-Dtest=UserRepositoryPostgresTest' test
Remove-Item Env:RUN_POSTGRES_TESTS
```

Test uloží používateľa s náhodným e-mailom a hashom hesla, vyprázdni JPA cache a načíta ho podľa ID.
Transakcia sa vráti späť. Schéma a posun sekvencie môžu zostať; nepoužívať produkčnú databázu.

Prvý ADMIN účet a základné kategórie pripravíme so správcom databázy.
Obnovenie hesla, refresh tokeny, obmedzenie prihlasovacích pokusov, chat, hodnotenia, platby a nasadenie sú v TODO.
