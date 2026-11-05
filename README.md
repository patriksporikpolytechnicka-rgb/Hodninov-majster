# Hodinový majster

Webová stránka v **Reacte** (TypeScript, Vite) napojená na **Spring Boot REST API**. Návod pre frontend: [docs/FRONTEND.md](docs/FRONTEND.md).
Plánovaná databáza je PostgreSQL na školskom serveri. Na školskú ukážku je pripravený samostatný profil s H2 v pamäti.

## Otvorenie webu pri kontrole projektu

V PowerShelli v koreňovom priečinku:

```powershell
.\start-demo.ps1
```

Po spustení servera otvorte **http://localhost:8080**.
Skript zostaví frontend a Spring Boot; pri prvom spustení stiahne závislosti.
Na tomto počítači už bol projekt zostavený, takže ho môžete spustiť rýchlejšie:

```powershell
.\start-demo.ps1 -SkipBuild
```

Parameter -SkipBuild používajte len po úspešnom zostavení a bez nových zmien kódu.
Web funguje v prehliadači; terminál drží spustený server. Zastavenie: Ctrl+C.

- Zákazník: **zakaznik@demo.sk**
- Poskytovateľ montáže: **majster@demo.sk**
- Heslo oboch ukážkových účtov: **DemoHeslo123!**

[Podrobný návod a scenár prezentácie](docs/DEMO.md)

## Čo funguje

Backend (REST API): registrácia a prihlásenie cez JWT, katalóg s filtrami a stránkovaním,
objednávky so serverovým výpočtom ceny a zmenami stavu, profil poskytovateľa a predplatné.

Frontend sa prepisuje z Angularu do Reactu. Zatiaľ je pripravený prázdny projekt napojený na API;
stránky sú popísané v [docs/FRONTEND.md](docs/FRONTEND.md).

## Vývoj

Potrebné: JDK 17+, Node.js 20.19+ alebo 22.12+, npm a Maven.
Skript vie nájsť Maven aj v IntelliJ pod Program Files.

Backend:

```powershell
mvn -Pdemo spring-boot:run '-Dspring-boot.run.profiles=demo'
```

Frontend v ďalšom termináli:

```powershell
cd frontend
npm install
npm run dev
```

Frontend: http://localhost:4200. Proxy presmeruje /api na backend 127.0.0.1:8080.

## Overenie

Overené 28. 9. 2026 (ešte s Angularom): production build, 26 unit/MVC testov a samostatný
integračný DemoFlowTest (prihlásenie → katalóg → objednávka → dokončenie) prešli.
PostgreSQL test zostáva odložený.

```powershell
npm --prefix frontend run build
mvn -Pdemo test
```

## Lokálna PostgreSQL databáza

Pre vývoj s lokálnym PostgreSQL (na tomto počítači port 6966):

```powershell
.\setup-local-db.ps1        # vytvorí používateľa fx_app a databázu fx, uloží pripojenie do .env.local
.\start-local.ps1 -Test     # spustí UserRepositoryPostgresTest proti databáze
.\start-local.ps1           # spustí backend s profilom local (tabuľky vytvorí Hibernate)
```

Setup si vypýta heslo superusera postgres. Súbor .env.local je v .gitignore a necommituje sa.
Iný port: `.\setup-local-db.ps1 -Port 5432`.

## Školská databáza

Databáza hodinovy_majster_db na cloud.kosickaakademia.sk:5432 má tabuľky aj základné kategórie.
V koreni projektu vytvorte .env.school (DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET) a spustite:

```powershell
.\start-local.ps1 -EnvFile .env.school -Profiles ''
```

Pre PostgreSQL nepoužite profil demo. Nastavte DB_URL, DB_USERNAME, DB_PASSWORD a náhodný
JWT_SECRET s aspoň 32 bajtmi. FRONTEND_ORIGIN je predvolene http://localhost:4200.
Heslá a kľúče neukladajte do repozitára.

Predvolený profil očakáva existujúcu schému (validate). Profil local používa Hibernate update
a smie sa použiť iba nad vlastnou vývojovou databázou po dohode so správcom.
VPN, SSL, migrácie, prvý ADMIN účet a kategórie zostávajú na dohodu so správcom.

Odložený PostgreSQL test sa aktivuje cez RUN_POSTGRES_TESTS=true.
H2 demo test nenahrádza overenie na PostgreSQL.

## Podklady

- [API pre frontend](docs/API.md)
- [OpenAPI](docs/openapi.json)
- [TypeScript typy](docs/api-types.ts)
- [TODO](TODO.md)

Verejná registrácia povoľuje iba CUSTOMER/PROVIDER. Produkčné nasadenie potrebuje HTTPS,
správu tajomstiev, obmedzenie prihlasovacích pokusov a dokončenie úloh v TODO.

## Predplatné poskytovateľa
Zákazník používa platformu zdarma. Poskytovateľ má od prvého vytvorenia profilu
30 dní zdarma, potom platí 10 € mesačne. Z ceny zákazky sa neodpočítava provízia.
Úprava profilu skúšobné obdobie neobnoví. Po vypršaní nemožno vytvoriť ani prijať
novú objednávku; už prijaté zákazky možno dokončiť.
Stránka /profil umožňuje vytvoriť profil a zobraziť stav TRIAL, ACTIVE alebo EXPIRED.
V demo režime možno po vypršaní simulovať platbu za jeden kalendárny mesiac.
Produkčná platobná brána a automatické účtovanie zatiaľ nie sú zapojené.
Pred spustením nad existujúcou PostgreSQL databázou musí správca vykonať
docs/provider-subscription.sql; migrácia sa automaticky nespúšťa.
