# Hodinový majster

Webová stránka v **Angulari** napojená na **Spring Boot REST API**.
Plánovaná databáza je PostgreSQL na školskom serveri. Na školskú ukážku je pripravený samostatný profil s H2 v pamäti.

## Otvorenie webu pri kontrole projektu

V PowerShelli v koreňovom priečinku:

```powershell
.\start-demo.ps1
```

Po spustení servera otvorte **http://localhost:8080**.
Skript zostaví Angular a Spring Boot; pri prvom spustení stiahne závislosti.
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

- Responzívna úvodná stránka a katalóg služieb z API.
- Hľadanie podľa názvu, mesta a kategórie, stránkovanie.
- Detail služby a poskytovateľa.
- Registrácia a prihlásenie cez JWT.
- Vytvorenie objednávky so serverovým výpočtom ceny.
- Zoznam objednávok a oprávnené zmeny stavu zákazníkom/poskytovateľom.
- Ukážkový režim je viditeľne označený. Dáta sa po reštarte vymažú.

Účet zostáva prihlásený iba počas aktuálneho otvorenia stránky; refresh vyžaduje nové prihlásenie.
Webový editor profilu a stav predplatného sú dostupné. Editor ponúk, chat, hodnotenia a produkčné platby sú ďalšie kroky.

## Vývoj

Potrebné: JDK 17+, Node.js kompatibilný s Angular 21 (napr. 22.12+ alebo 24), npm a Maven.
[Oficiálna kompatibilita Angularu](https://angular.dev/reference/versions).
Skript vie nájsť Maven aj v IntelliJ pod Program Files.

Backend:

```powershell
mvn -Pdemo spring-boot:run '-Dspring-boot.run.profiles=demo'
```

Frontend v ďalšom termináli:

```powershell
cd frontend
npm ci
npm start
```

Frontend: http://localhost:4200. Proxy presmeruje /api na backend 127.0.0.1:8080.

## Overenie

Overené 28. 9. 2026: Angular production build, 26 unit/MVC testov a samostatný
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

## Školská databáza – neskôr

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
