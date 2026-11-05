# TODO – Hodinový majster

Technológie: React + TypeScript, Java + Spring Boot, PostgreSQL.

## Lokálna PostgreSQL a demo v2 – 4. 10. 2026

- [x] Vytvoriť lokálnu databázu fx a používateľa fx_app (setup-local-db.ps1, db/create-local-db.sql).
- [x] Pripojenie uložené v .env.local mimo repozitára; spustenie cez start-local.ps1.
- [x] Spring Boot beží proti lokálnej PostgreSQL; Hibernate vytvoril tabuľky a cudzie kľúče.
- [x] UserRepositoryPostgresTest prešiel lokálne: uloženie, načítanie a rollback.
- [x] Profil seed: DemoData naplní lokálnu databázu ukážkovými dátami (iba raz).
- [x] Demo v2: tlačidlá ukážkových účtov a pravdivý horný pruh cez app.demo-accounts.
- [x] Web na localhost:8080 zobrazuje katalóg a prihlásenie s dátami z PostgreSQL.

## PostgreSQL na školskom serveri – 5. 11. 2026

- [x] Vytvoriť samostatnú databázu a používateľa na školskom serveri. (hodinovy_majster_db, hodinovy_majster_admin)
- [x] Zistiť host, port, názov databázy a požiadavky na školskú sieť/VPN/SSL. (cloud.kosickaakademia.sk:5432)
- [x] Nastaviť DB_URL, DB_USERNAME a DB_PASSWORD lokálne; heslo neukladať do repozitára. (.env.local, .env.school)
- [x] Dohodnúť vytváranie schémy a oprávnenia; Hibernate update používať iba vo vlastnej vývojovej databáze, nie nad zdieľanými produkčnými tabuľkami. (tabuľky vytvorené raz cez profil local, ďalej iba validate)
- [x] Overiť spustenie Spring Bootu s PostgreSQL. (lokálne aj na školskom serveri)
- [x] Základné kategórie na školskom serveri (db/school-categories.sql).
- [ ] Spustiť UserRepositoryPostgresTest a potvrdiť uloženie/načítanie používateľa a rollback. (lokálne overené 4. 10. 2026, na školskom serveri ešte nie)

## Backend a podklady pre frontend

Základný backend a Angular demo boli zostavené a otestované 28. 9. 2026. PostgreSQL je stále odložené.

- [x] Dokončiť Category: identifikátor a názov.
- [x] Dokončiť Service: poskytovateľ, kategória, názov, popis, cena (BigDecimal), jednotka ceny.
- [x] Dokončiť Order: zákazník, poskytovateľ/služba, termín, stav, cena a čas vytvorenia; určiť povolené zmeny stavov.
- [x] Doplniť potrebné repository a pravidlá pre povinné polia a unikátny e-mail.
- [x] Dohodnúť API kontrakt a DTO pre frontend/backend.
- [x] Implementovať registráciu a login: hashovanie hesiel, validácia, oprávnenia a JWT; verejná registrácia nesmie prideliť ADMIN rolu.
- [x] Založiť základný Angular frontend: routing, rozloženie, formuláre registrácie/prihlásenia a API service.
- [x] Katalóg a detail poskytovateľa napojené na API; ukážkové dáta cez profil demo.
- [x] Maven zostavenie a kompilácia testov overené 28. 9. 2026.

## Následne – funkčný hlavný proces

- [x] Správa profilu poskytovateľa a jeho ponuky služieb/cien.
- [x] Vyhľadávanie a filtrovanie služieb.
- [x] Vytvorenie, prijatie, odmietnutie, zrušenie a dokončenie objednávky s kontrolou oprávnení.
- [ ] Prepojenie Angularu s API a test celého procesu.

## Neskoršie časti MVP

- [ ] Chat zákazníka s poskytovateľom.
- [ ] Hodnotenia po dokončenej zákazke.
- [ ] Produkčné mesačné predplatné poskytovateľov (10 €), platobná brána a overené potvrdenia platieb. Bez provízií zo zákaziek.
- [ ] Administrácia používateľov, kategórií a objednávok.
- [ ] Testovanie, dokumentácia a nasadenie.

## Pripravené v kóde

- [x] Základné User, Role, ProviderProfile a UserRepository.
- [x] Spúšťacia trieda FxApplication.
- [x] Konfigurácia PostgreSQL cez premenné prostredia a profil local.
- [x] Integračný test uloženia/načítania používateľa proti PostgreSQL.
- [x] README s návodom na spustenie.

## Odovzdanie API a overenie

- [x] OpenAPI kontrakt, TypeScript typy, ukážkové JSON dáta a návod v docs/.
- [x] Unit/MVC testy oprávnení, ceny, stavov, JWT a vstupov (úspešne spustené 28. 9. 2026).
- [x] Prešlo 26 unit/MVC testov a integračný demo test.
- [x] So správcom databázy pripraviť prvý ADMIN účet a základné kategórie. (5. 11. 2026: admin@hodinovymajster.sk, kategórie cez db/school-categories.sql)
- [ ] Overiť JPA mapovania, filtre, unikátnosť a súbežné zmeny na PostgreSQL. (mapovania lokálne overené 4. 10. 2026; filtre, unikátnosť a súbežnosť zatiaľ nie)
- [ ] Doplniť databázové migrácie pred produkčným nasadením.
- [ ] Pred verejným nasadením doplniť obmedzenie pokusov o prihlásenie a HTTPS.
- [ ] Obnovenie hesla a podľa potreby refresh/revokácia tokenov.

## Webová ukážka – 28. 9. 2026

- [x] Angular katalóg, filtre, detail služby, login/register a objednávky cez REST API.
- [x] Samostatný profil demo s H2 v pamäti, fiktívnymi účtami a náhodným JWT kľúčom.
- [x] Spustenie webu cez start-demo.ps1, dokumentácia docs/DEMO.md.
- [x] Angular production build a backendový integračný test celého objednávkového procesu.
- [ ] Kolega: doplniť webový editor profilu poskytovateľa a jeho služieb.
- [ ] Dokončiť reálne prepojenie a integračné overenie na školskej PostgreSQL. (prepojenie hotové, registrácia a profil overené; objednávky čakajú na editor služieb)
