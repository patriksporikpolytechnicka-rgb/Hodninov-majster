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
{"accessToken":"<JWT>","tokenType":"Bearer","expiresIn":900,"user":{"id":1,"email":"customer@example.com","role":"CUSTOMER"},"refreshToken":"<opaque>"}
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

## Hodnotenia

POST /api/orders/{id}/review (iba CUSTOMER, ktorý objednávku vytvoril; objednávka musí byť COMPLETED) → 201:
`{"rating":5,"comment":"Rýchlo a čisto."}` – rating 1 až 5, comment nepovinný (max. 1000 znakov).
Každú objednávku možno ohodnotiť iba raz; druhý pokus vráti 409.

GET /api/orders/{id}/review – hodnotenie objednávky pre zákazníka, poskytovateľa alebo admina; 404, ak ešte nie je.

GET /api/providers/{id}/reviews?page=0&size=10 (verejné) – PageResponse s ReviewResponse, najnovšie prvé:
`{"id":7,"orderId":5,"providerId":3,"serviceName":"Montáž nábytku","rating":5,"comment":"...","createdAt":"2026-11-19T11:00:00"}`

GET /api/providers/{id}/rating (verejné) → `{"providerId":3,"average":4.5,"count":2}`; average je null, kým nie je žiadne hodnotenie.

Tabuľku vytvorí Flyway migrácia V2 pri štarte aplikácie.

## Administrácia (iba ADMIN)

PUT /api/categories/{id} `{"name":"Záhrada"}` – premenovanie; rovnaký názov ako iná kategória vráti 409.
DELETE /api/categories/{id} → 204; kategóriu, ktorú používa nejaká služba, nemožno zmazať (409).

GET /api/admin/users?q=email&role=PROVIDER&blocked=false&page=0&size=20 – PageResponse s
`{"id":2,"email":"...","role":"CUSTOMER","blocked":false,"createdAt":"..."}`.

PATCH /api/admin/users/{id} `{"blocked":true}` alebo `{"role":"PROVIDER"}` (jedno alebo oboje):
- admin nemôže meniť vlastný účet (409),
- rolu možno zmeniť iba účtu bez histórie (bez profilu poskytovateľa a bez objednávok), inak 409 – taký účet treba zablokovať.

Zablokovaný účet sa nemôže prihlásiť (403 „Account is blocked“) a jeho platné tokeny prestanú fungovať.
Služby a verejný profil zablokovaného poskytovateľa sa nezobrazujú a nedajú sa objednať.

## Mazanie služieb

DELETE /api/services/{id} → 204 (vlastník-poskytovateľ alebo ADMIN):
- s otvorenými objednávkami (PENDING, ACCEPTED, IN_PROGRESS) vráti 409 – najprv ich dokončiť alebo zrušiť,
- služba bez objednávok sa odstráni úplne,
- služba so starými objednávkami sa iba označí ako zmazaná: zmizne z katalógu aj z /api/services/mine, história objednávok zostane.

Nové stĺpce pridá Flyway migrácia V3 pri štarte aplikácie.

## Prihlásenie, obnovenie tokenu a odhlásenie

- Access token (JWT) platí 15 minút (expiresIn 900). Login a registrácia vracajú aj refreshToken (platí 14 dní).
- POST /api/auth/refresh `{"refreshToken":"..."}` → nový AuthResponse s novým access aj refresh tokenom.
  Starý refresh token sa tým zneplatní. Opätovné použitie už vymeneného tokenu ukončí všetky relácie používateľa (401).
- POST /api/auth/logout `{"refreshToken":"..."}` → 204, ukončí jednu reláciu (verejné, stačí token).
- POST /api/auth/logout-all (prihlásený) → 204, ukončí všetky relácie; aj ešte platné access tokeny prestanú fungovať.
- Frontend: po 401 skúsiť raz /refresh; ak zlyhá, odhlásiť. Refresh token neposielať nikam inam ako na /api/auth.

Limit pokusov: po 5 neúspešných prihláseniach na jeden e-mail (alebo 30 z jednej IP) v priebehu 15 minút
vráti /login aj pri správnom hesle 429 „Too many attempts“. Rovnaký limit platí pre žiadosti o obnovenie hesla.
Počítadlo je v pamäti servera (reštart ho vynuluje).

## Obnovenie hesla

1. POST /api/auth/password-reset/request `{"email":"..."}` → vždy 202 (neprezradí, či e-mail existuje).
   Ak účet existuje, príde e-mail s odkazom `PASSWORD_RESET_LINK + token`, platí 30 minút a iba raz.
2. POST /api/auth/password-reset/confirm `{"token":"...","newPassword":"..."}` → 204. Heslo 10–72 znakov.
   Zmena hesla odhlási používateľa zo všetkých zariadení. Neplatný, použitý alebo starý token → 400.

E-mail potrebuje SMTP server (spring.mail.host, spring.mail.port, spring.mail.username, spring.mail.password)
a premenné PASSWORD_RESET_LINK (napr. https://.../obnova-hesla?token=) a MAIL_FROM.
V profiloch demo a local sa odkaz iba vypíše do konzoly servera.

## Databázové migrácie (Flyway)

Tabuľky vytvára a mení Flyway zo súborov src/main/resources/db/migration (V1, V2, ...); Hibernate ich iba kontroluje (validate).
- Školská DB bez histórie Flyway sa pri prvom štarte označí ako verzia 1 a dostane iba novšie migrácie.
- Každá zmena tabuliek = nový súbor V<číslo>__popis.sql. Už nasadený súbor sa nikdy neupravuje.
- Profil demo (H2 v pamäti) Flyway nepoužíva.
- docs/openapi.json a docs/api-types.ts zatiaľ neobsahujú hodnotenia, administráciu, mazanie ani nové /api/auth endpointy.

## Dvojfaktorové prihlásenie (Google Authenticator)

Používa štandard TOTP (RFC 6238): 6-miestny kód, nový každých 30 sekúnd. Funguje s Google Authenticator,
Microsoft Authenticator, Authy a pod. Server nič neposiela – aplikácia a server počítajú kód z rovnakého tajomstva.

Zapnutie (prihlásený používateľ):
1. POST /api/auth/2fa/setup → `{"secret":"JBSW...","otpauthUri":"otpauth://totp/..."}`.
   Frontend z otpauthUri vykreslí QR kód (alebo zobrazí secret na ručné zadanie).
2. Používateľ naskenuje QR kód a pošle prvý kód: POST /api/auth/2fa/enable `{"code":"123456"}` → 204.
   Až potom je 2FA zapnuté.

Prihlásenie so zapnutým 2FA:
- POST /api/auth/login bez code → 401 s message „Two-factor code required“ (heslo bolo správne).
- Frontend zobrazí pole na kód a pošle znova `{"email":"...","password":"...","code":"123456"}`.
- Zlý kód → 401 „Invalid two-factor code“ a počíta sa do limitu 5 pokusov. Každý kód platí iba raz.
- Tolerancia: prijme sa aj kód o 30 s starší alebo novší (rozdiel hodín v telefóne).

Vypnutie: POST /api/auth/2fa/disable `{"code":"123456"}` → 204 (vyžaduje platný kód).
Stratený telefón: admin cez PATCH /api/admin/users/{id} `{"disableTwoFactor":true}`.
UserResponse (/me, login) aj AdminUserResponse obsahujú twoFactorEnabled. Stĺpce pridá migrácia V5.

## Overenie e-mailu

- Registrácia pošle e-mail s odkazom `EMAIL_VERIFICATION_LINK + token` (platí 24 hodín, iba raz). Účet sa dá hneď používať,
  ale kým nie je e-mail overený, zákazník nemôže objednávať a poskytovateľ nemôže vytvoriť profil ani pridať službu
  (403 „Verify your e-mail address first“).
- POST /api/auth/verify-email `{"token":"..."}` (verejné) → 204; neplatný, použitý alebo starý odkaz → 400.
- POST /api/auth/verify-email/resend (prihlásený) → 202, nový odkaz (staré prestanú platiť); limit ako pri prihlásení.
- UserResponse aj AdminUserResponse majú emailVerified. Admin môže e-mail potvrdiť ručne: PATCH /api/admin/users/{id} `{"markEmailVerified":true}`.
- Účty, ktoré existovali pred touto zmenou (aj demo účty), sú považované za overené (migrácia V6).

## E-maily – nastavenie

SMTP server cez premenné SPRING_MAIL_HOST, SPRING_MAIL_PORT, SPRING_MAIL_USERNAME, SPRING_MAIL_PASSWORD
(+ podľa servera SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true), odosielateľ MAIL_FROM.
Odkazy: PASSWORD_RESET_LINK, EMAIL_VERIFICATION_LINK. Bez SMTP servera profily demo a local e-maily iba vypíšu do konzoly
(app.mail.log-without-server=true). Chyba pri odoslaní e-mailu nikdy nezhodí požiadavku, iba sa zapíše do logu.

## E-mailové upozornenia k objednávkam

Posielajú sa automaticky po uložení zmeny (na pozadí, požiadavka na ne nečaká):
- nová objednávka → majster (termín, adresa, cena, poznámka),
- prijatá, odmietnutá, začatá, dokončená → zákazník,
- zrušená → druhá strana (zrušil zákazník → majster, zrušil majster → zákazník).
Každý e-mail obsahuje odkaz FRONTEND_URL/objednavky/{id} (predvolene http://localhost:4200).

## Hodnotenie v katalógu a triedenie

- ProviderResponse (aj vnútri ServiceResponse) má `rating` (priemer zaokrúhlený na 1 desatinné miesto, null bez hodnotení)
  a `reviewCount`. Hodnota sa prepočíta pri každom novom hodnotení (migrácia V7 ju doplní aj pre staré hodnotenia).
- GET /api/services?sort=newest | price_asc | price_desc | rating – bez sort ostáva pôvodné poradie (najstaršie prvé).
  `rating` = najlepšie hodnotení majstri prví (pri zhode viac hodnotení). Neznáma hodnota → 400.

## Chat k objednávke

- POST /api/orders/{id}/messages `{"text":"..."}` → 201 MessageResponse; píše iba zákazník alebo majster danej objednávky (max. 2000 znakov).
- GET /api/orders/{id}/messages?after=0 → zoznam od najstaršej, najviac 100 naraz. Frontend si pamätá id poslednej správy
  a každých pár sekúnd sa pýta `?after=<id>` (polling, bez WebSocketu). Admin môže správy čítať, nie písať.
- MessageResponse: `{"id":9,"orderId":5,"senderId":1,"senderRole":"CUSTOMER","text":"...","createdAt":"..."}`.

## Dostupnosť majstra

- GET /api/providers/{id}/availability (verejné) → `{"workingHours":[{"dayOfWeek":1,"start":"08:00:00","end":"16:00:00"}],"daysOff":["2026-12-24"]}`
  (dayOfWeek 1 = pondelok … 7 = nedeľa; daysOff iba od dnes ďalej).
- PUT /api/providers/me/working-hours `{"hours":[{"dayOfWeek":1,"start":"08:00","end":"16:00"}, ...]}` – nahradí celý týždeň,
  každý deň najviac raz, start < end. Prázdny zoznam = bez obmedzenia.
- POST /api/providers/me/days-off `{"date":"2026-12-24"}` (dnes alebo neskôr), DELETE /api/providers/me/days-off/2026-12-24 → 204.
- Objednávka: v deň voľna → 409 „The handyman is not available on that day“; mimo pracovného času → 409
  „Outside the handyman's working hours“ (začiatok musí byť od start do end, end už nie). Majster bez pracovných hodín
  sa dá objednať kedykoľvek ako doteraz.

## Profilová fotka majstra

- POST /api/providers/me/photo – multipart/form-data, pole `file`; JPEG, PNG alebo WebP, max. 2 MB.
  Typ sa overuje podľa obsahu súboru (nie podľa názvu). Vráti ProviderResponse s `profileImage` = `/api/providers/{id}/photo?v=...`.
  Chyby: 413 príliš veľký, 415 iný typ, 400 chýba súbor.
- GET /api/providers/{id}/photo (verejné) – obrázok, cache 1 deň (parameter v sa mení pri každom nahratí).
- DELETE /api/providers/me/photo → 204, profileImage bude null.
- PUT /api/providers/me už pole profileImage ignoruje (nastavuje ho iba nahratie fotky).

## Štatistiky majstra

GET /api/providers/me/stats (iba PROVIDER) →
`{"ordersTotal":4,"ordersByStatus":{"PENDING":1,"ACCEPTED":0,...},"upcomingOrders":0,"completedThisMonth":2,"earningsThisMonth":80.00,"completedTotal":3,"earningsTotal":120.50,"rating":4.3,"reviewCount":4}`
- Mesiac sa určuje podľa termínu objednávky (Europe/Bratislava). Zárobok = súčet cien DOKONČENÝCH objednávok (bez provízie).
- upcomingOrders = prijaté alebo prebiehajúce objednávky s termínom od teraz.

## Audit log (iba ADMIN)

GET /api/admin/audit?action=USER_BLOCKED&actorId=1&page=0&size=50 → PageResponse, najnovšie prvé:
`{"id":1,"actorId":1,"actorEmail":"admin@demo.sk","action":"USER_BLOCKED","targetType":"USER","targetId":2,"details":"...","createdAt":"..."}`
Zaznamenáva sa: USER_BLOCKED, USER_UNBLOCKED, USER_ROLE_CHANGED, USER_2FA_RESET, USER_EMAIL_VERIFIED, CATEGORY_CREATED,
CATEGORY_RENAMED, CATEGORY_DELETED, SERVICE_DELETED, PASSWORD_RESET, 2FA_ENABLED, 2FA_DISABLED, TOKEN_REUSE_DETECTED,
ACCOUNT_DELETED. Záznam vznikne v tej istej transakcii ako akcia (ak akcia zlyhá, záznam nevznikne). Záznamy sa nemažú.

## GDPR – export údajov a zrušenie účtu

- GET /api/account/export (prihlásený) → JSON na stiahnutie (Content-Disposition: attachment): účet, profil majstra, služby,
  objednávky ako zákazník aj ako majster, napísané hodnotenia a správy, dostupnosť. Heslá, 2FA tajomstvá ani tokeny v ňom nie sú.
- POST /api/account/delete `{"password":"...","code":"123456"}` → 204 (code iba pri zapnutom 2FA).
  - Zlé heslo alebo kód → 400; otvorené objednávky (čakajúce, prijaté, prebiehajúce) → 409; admin → 409.
  - Účet sa anonymizuje: e-mail → deleted-{id}@deleted.invalid, heslo sa zmení, účet sa zablokuje, odhlási zo všetkých zariadení.
    Profil majstra sa vymaže (meno „Zmazaný účet“), služby, fotka, pracovný čas a voľno zmiznú. V objednávkach zákazníka
    sa vymaže adresa a poznámka, v hodnoteniach komentár (hviezdičky ostanú), texty správ sa nahradia „[správa zmazaná]“.
  - Pôvodný e-mail sa dá potom znova použiť na novú registráciu. Do audit logu sa zapíše ACCOUNT_DELETED (bez e-mailu).
