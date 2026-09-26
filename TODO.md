# TODO – Hodinový majster

Technológie: Angular + TypeScript, Java + Spring Boot, PostgreSQL.

## Odložené – PostgreSQL na školskom serveri

Používateľ 26. 9. 2026 rozhodol odložiť databázu. Lokálnu databázu teraz nevytvárať.

- [ ] Vytvoriť samostatnú databázu a používateľa na školskom serveri.
- [ ] Zistiť host, port, názov databázy a požiadavky na školskú sieť/VPN/SSL.
- [ ] Nastaviť DB_URL, DB_USERNAME a DB_PASSWORD lokálne; heslo neukladať do repozitára.
- [ ] Dohodnúť vytváranie schémy a oprávnenia; Hibernate update používať iba vo vlastnej vývojovej databáze, nie nad zdieľanými produkčnými tabuľkami.
- [ ] Overiť spustenie Spring Bootu s PostgreSQL.
- [ ] Spustiť UserRepositoryPostgresTest a potvrdiť uloženie/načítanie používateľa a rollback.

## Backend a podklady pre frontend

Zaškrtnuté položky znamenajú implementované v kóde; zostavenie a testy ešte neboli overené.

- [x] Dokončiť Category: identifikátor a názov.
- [x] Dokončiť Service: poskytovateľ, kategória, názov, popis, cena (BigDecimal), jednotka ceny.
- [x] Dokončiť Order: zákazník, poskytovateľ/služba, termín, stav, cena a čas vytvorenia; určiť povolené zmeny stavov.
- [x] Doplniť potrebné repository a pravidlá pre povinné polia a unikátny e-mail.
- [x] Dohodnúť API kontrakt a DTO pre frontend/backend.
- [x] Implementovať registráciu a login: hashovanie hesiel, validácia, oprávnenia a JWT; verejná registrácia nesmie prideliť ADMIN rolu.
- [ ] Spolupracovník: založiť Angular frontend: routing, rozloženie, formuláre registrácie/prihlásenia a API service.
- [ ] Spolupracovník: pripraviť zoznam služieb a profil poskytovateľa s jasne označenými ukážkovými dátami, kým databáza nebude dostupná.
- [ ] Overiť Maven zostavenie a kompiláciu testov; predchádzajúci pokus nebol potvrdený.

## Následne – funkčný hlavný proces

- [x] Správa profilu poskytovateľa a jeho ponuky služieb/cien.
- [x] Vyhľadávanie a filtrovanie služieb.
- [x] Vytvorenie, prijatie, odmietnutie, zrušenie a dokončenie objednávky s kontrolou oprávnení.
- [ ] Prepojenie Angularu s API a test celého procesu.

## Neskoršie časti MVP

- [ ] Chat zákazníka s poskytovateľom.
- [ ] Hodnotenia po dokončenej zákazke.
- [ ] Evidencia/simulácia platieb a provízií.
- [ ] Administrácia používateľov, kategórií a objednávok.
- [ ] Testovanie, dokumentácia a nasadenie.

## Už pripravené v kóde (nie potvrdené spustením)

- [x] Základné User, Role, ProviderProfile a UserRepository.
- [x] Spúšťacia trieda FxApplication.
- [x] Konfigurácia PostgreSQL cez premenné prostredia a profil local.
- [x] Integračný test uloženia/načítania používateľa proti PostgreSQL.
- [x] README s návodom na spustenie.

## Odovzdanie API a overenie

- [x] OpenAPI kontrakt, TypeScript typy, ukážkové JSON dáta a návod v docs/.
- [x] Unit/MVC testy oprávnení, ceny, stavov, JWT a vstupov (napísané; zatiaľ nespustené).
- [ ] Spustiť Maven testy a opraviť prípadné chyby; spustenie mimo sandboxu bolo zamietnuté.
- [ ] So správcom databázy pripraviť prvý ADMIN účet a základné kategórie.
- [ ] Overiť JPA mapovania, filtre, unikátnosť a súbežné zmeny na PostgreSQL.
- [ ] Doplniť databázové migrácie pred produkčným nasadením.
- [ ] Pred verejným nasadením doplniť obmedzenie pokusov o prihlásenie a HTTPS.
- [ ] Obnovenie hesla a podľa potreby refresh/revokácia tokenov.
