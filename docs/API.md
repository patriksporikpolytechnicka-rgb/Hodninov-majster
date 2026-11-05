# API pre Angular – Hodinový majster

## Stav a podklady

Backend má 18 REST operácií vrátane GET /api/info. Základný Angular frontend je v priečinku frontend/.
Angular build, unit/MVC testy a demo integračný test prešli 28. 9. 2026.
Školská PostgreSQL databáza je stále odložená; pre kontrolu projektu použite docs/DEMO.md.

- [openapi.json](openapi.json) – OpenAPI 3.0.3 na import do Postmanu/Swagger Editora.
- [api-types.ts](api-types.ts) – TypeScript typy pre Angular.
- [examples.json](examples.json) – označené ukážkové dáta, nie záznamy z databázy.

Base URL: http://localhost:8080. JSON požiadavky: Content-Type: application/json.
CORS: http://localhost:4200; inú adresu backend načíta z FRONTEND_ORIGIN.

## Autentifikácia

POST /api/auth/register → 201:

```json
{"email":"customer@example.com","password":"dlhe-testovacie-heslo","role":"CUSTOMER"}
```

Registrácia povoľuje CUSTOMER alebo PROVIDER, nikdy ADMIN.
Heslo má aspoň 10 znakov a najviac 72 bajtov UTF-8. E-mail sa normalizuje na malé písmená.
POST /api/auth/login → 200: rovnaké email/password, bez role.
Oba endpointy vracajú:

```json
{"accessToken":"<JWT>","tokenType":"Bearer","expiresIn":3600,"user":{"id":1,"email":"customer@example.com","role":"CUSTOMER"}}
```

Na chránené požiadavky pridajte Authorization: Bearer <JWT>.
GET /api/auth/me vráti iba objekt user, nikdy heslo.
Token platí jednu hodinu; refresh token zatiaľ nie je implementovaný. Po 401 vyžiadajte nové prihlásenie.
Odhlásenie vo frontende odstráni token, ale vydaný token ostáva platný do expirácie.
Token pri vývoji držte v pamäti autentifikačnej služby. Server nepoužíva prihlasovacie cookies.
Produkčné nasadenie musí používať HTTPS a doplniť obmedzenie prihlasovacích pokusov.

## Endpointy

| Metóda | Cesta | Prístup / odpoveď |
|---|---|---|
| POST | /api/auth/register | Verejné; AuthResponse, 201 |
| POST | /api/auth/login | Verejné; AuthResponse |
| GET | /api/auth/me | Prihlásený; UserResponse |
| GET | /api/categories | Verejné; CategoryResponse[] podľa názvu |
| POST | /api/categories | ADMIN; CategoryRequest → CategoryResponse, 201 |
| GET | /api/providers/{id} | Verejné; ProviderResponse |
| GET | /api/providers/me | PROVIDER; vlastný profil, 404 ak neexistuje |
| PUT | /api/providers/me | PROVIDER; ProviderRequest → vytvorenie/úprava profilu, 200 |
| GET | /api/services | Verejné; stránka aktívnych služieb |
| GET | /api/services/{id} | Verejné; ServiceResponse, neaktívna služba má 404 |
| GET | /api/services/mine | PROVIDER; vlastné služby vrátane neaktívnych |
| POST | /api/services | PROVIDER s profilom; ServiceRequest → ServiceResponse, 201 |
| PUT | /api/services/{id} | Vlastník; ServiceRequest → ServiceResponse |
| POST | /api/orders | CUSTOMER; OrderRequest → OrderResponse, 201 |
| GET | /api/orders/my | Vlastné objednávky; ADMIN vidí všetky |
| GET | /api/orders/{id} | Účastník objednávky alebo ADMIN |
| PATCH | /api/orders/{id}/status | Účastník; StatusRequest → OrderResponse |

PUT posiela všetky povinné polia. DELETE zatiaľ neexistuje.
Služba sa skryje cez PUT s active=false; staré objednávky sa zachovajú.
Prvý ADMIN účet a kategórie pripravíme so správcom databázy; verejné API nepovyšuje roly.

## Profil a služba

PUT /api/providers/me:

```json
{"name":"Peter Novák","description":"Montáž nábytku","location":"Košice","phone":"+421900000000","profileImage":null,"availability":true}
```

profileImage je voliteľný odkaz, upload súborov zatiaľ nie je implementovaný.
Verejný profil obsahuje uvedené údaje plus id, bez prihlasovacieho e-mailu či hesla.

POST /api/services alebo PUT /api/services/{id}:

```json
{"categoryId":1,"name":"Montáž nábytku","description":"Montáž skríň a stolov","price":25.50,"priceUnit":"HOUR","active":true}
```

Poskytovateľ sa určí z JWT a profilu; providerId sa neposiela.
Jednotky: HOUR (hodina), JOB (úkon), SQUARE_METER (m²). Ceny sú v EUR, kladné, najviac na dve desatinné miesta.
Odpoveď obsahuje id, provider (verejný profil), category (id/name) a name/description/price/priceUnit/active.

## Filtrovanie a stránkovanie

GET /api/services?categoryId=1&providerId=3&q=montáž&location=Košice&page=0&size=20

Filtre sú voliteľné, kombinujú sa cez AND. q hľadá časť názvu služby, location časť lokality,
bez rozlišovania veľkosti písmen. Služby sú zoradené podľa id vzostupne.
Služby nedostupných poskytovateľov sa zobrazujú, ale ich objednanie sa odmietne.
page začína od 0, size je 1–100 (predvolene 20). Platí aj pre /services/mine a /orders/my.
Objednávky sú zoradené podľa createdAt a id zostupne.

```json
{"content":[],"page":0,"size":20,"totalElements":0,"totalPages":0}
```

## Objednávky

POST /api/orders:

```json
{"serviceId":4,"scheduledAt":"2027-01-10T10:00:00","quantity":2,"address":"Ukážková 1, Košice","note":"Montáž skrine"}
```

scheduledAt je budúci miestny čas Europe/Bratislava, bez Z alebo UTC offsetu.
Angular môže poslať hodnotu datetime-local; neprevádzať ju cez toISOString().
quantity je kladný počet hodín, úkonov alebo m², najviac na dve desatinné miesta; pri JOB celé číslo.
Pri online službe môže address obsahovať Online. note je voliteľné.
Poskytovateľ musí byť dostupný a služba aktívna.

customerId, providerId, status, unitPrice a totalPrice sa neposielajú; určí ich server.
Cena = jednotková cena × množstvo, zaokrúhlenie na centy HALF_UP.
Pri hodinovej práci ide o objednaný počet hodín; meranie skutočného času či zmena dohodnutej ceny zatiaľ neexistuje.
Názov, jednotková cena a jednotka služby sa uložia v objednávke; zmena cenníka nezmení jej históriu.
Presný OrderResponse je v api-types.ts a examples.json.

PATCH /api/orders/5/status:

```json
{"status":"ACCEPTED"}
```

| Z | Na | Kto |
|---|---|---|
| PENDING | ACCEPTED / REJECTED | Poskytovateľ objednávky |
| PENDING | CANCELLED | Zákazník objednávky |
| ACCEPTED | IN_PROGRESS | Poskytovateľ objednávky |
| ACCEPTED | CANCELLED | Zákazník / poskytovateľ objednávky |
| IN_PROGRESS | COMPLETED | Poskytovateľ objednávky |

COMPLETED, CANCELLED a REJECTED sú konečné. ADMIN má prístup na čítanie, nie na zmenu stavu.
Neplatný prechod → 409. Súbežné úpravy kontroluje JPA version; pri konflikte obnovte detail.
Kontrola kolízií kalendára zatiaľ nie je implementovaná; poskytovateľ rozhoduje prijatím objednávky.

## Chyby

```json
{"status":400,"message":"Invalid request","fieldErrors":{"name":"must not be blank"}}
```

- 400: neplatné polia, JSON, enum, dátum alebo parametre. Neznáme polia sa odmietajú.
- 401: chýbajúci/neplatný/expirovaný token alebo nesprávne prihlasovacie údaje.
- 403: nesprávna rola alebo cudzia služba/objednávka.
- 404: záznam alebo profil neexistuje.
- 409: duplicitný e-mail/kategória, nedostupná služba, neplatný prechod alebo súbežná úprava.

Zobrazte fieldErrors pri poliach a message ako všeobecnú chybu.
Sieťové chyby a 5xx nemusia mať tento formát; zobrazte všeobecné hlásenie.

## Postup pre spolupracovníka

1. Prevziať api-types.ts a použiť examples.json v mock API službe.
2. Pripraviť register/login, katalóg, filtre a detaily poskytovateľa/služby.
3. Pripraviť vlastný provider profil, editor služieb, objednanie a zoznam objednávok.
4. Akcie objednávky zobraziť podľa role a tabuľky stavov.
5. Pri integrácii vymeniť mock službu za HttpClient a pridávať Bearer hlavičku len na adresu tohto backendu.

Základný Angular frontend je pripravený v priečinku frontend/. Chat, hodnotenia, platby a kompletná administrácia sú ďalšie etapy.

## Predplatné poskytovateľa
GET /api/providers/me/subscription (iba PROVIDER) vracia status TRIAL/ACTIVE/EXPIRED,
subscriptionStart a subscriptionEnd (UTC ISO-8601), monthlyFee = 10.00, currency = EUR,
canReceiveOrders a demoPayment. PUT /api/providers/me začína 30-dňový trial iba pri
prvom vytvorení profilu. Zmeny profilu nemenia jeho dátumy.
POST /api/providers/me/subscription/demo-payment bez tela simuluje mesačnú platbu
iba v demo režime a iba po vypršaní predplatného. V produkcii vracia 403.
Nová objednávka alebo prijatie objednávky pri vypršaní predplatného vracia 402.
Cena objednávky zostáva cena služby × množstvo, bez provízie.
Produkčné inkaso a automatické obnovovanie zatiaľ nie sú implementované.
