# Školská ukážka – Hodinový majster

## Najjednoduchšie spustenie vo Windows

Potrebujete JDK 17+, Node.js 22.12+ alebo 24 a Maven (skript vie nájsť aj Maven z IntelliJ v Program Files).
Pri prvom spustení potrebujete internet na stiahnutie závislostí.

V koreňovom priečinku projektu otvorte PowerShell a spustite:

```powershell
.\start-demo.ps1
```

Skript nainštaluje závislosti Angularu, zostaví frontend a zabalí ho do Spring Boot JAR.
Po hláške o spustení otvorte **http://localhost:8080**. Nezatvárajte terminál.
Prvá príprava môže trvať niekoľko minút. Ďalšie spustenie už zostaveného projektu:

```powershell
.\start-demo.ps1 -SkipBuild
```

Pri zmene zdrojového kódu spustite bez -SkipBuild. Zastavenie: Ctrl+C.
Ak port 8080 používa iná aplikácia, najprv ju ukončite alebo zvoľte iný port pri ručnom spustení JAR.

## Čo predviesť triednemu

1. Úvodná stránka zobrazí šesť ukážkových služieb načítaných zo Spring Boot API.
2. Vyhľadajte „Montáž“, zvoľte mesto Košice alebo kategóriu.
3. Otvorte detail služby a kliknite na Prihlásiť sa.
4. Na prihlásení kliknite **Som zákazník**, potom **Prihlásiť sa**.
5. Vráťte sa na detail Montáž nábytku, vyberte budúci termín, počet hodín a adresu a odošlite objednávku.
6. V Objednávkach uvidíte cenu a stav Čaká na prijatie.
7. Odhláste sa, prihláste sa cez **Som poskytovateľ** a otvorte Objednávky.
8. Objednávku prijmite, začnite prácu a dokončite ju.

Ukážkové účty (iba profil demo):

| Rola | E-mail | Heslo |
|---|---|---|
| Zákazník | zakaznik@demo.sk | DemoHeslo123! |
| Poskytovateľ montáže/opravy/sťahovanie | majster@demo.sk | DemoHeslo123! |
| Poskytovateľ záhrady | zahrada@demo.sk | DemoHeslo123! |
| Poskytovateľ IT | technik@demo.sk | DemoHeslo123! |

## Čo je reálne a čo je ukážkové

Frontend je Angular. Požiadavky idú na skutočné API: login/JWT, filtre, objednávky aj zmeny stavu.
API používa rovnaké controllery, služby, repository a entity ako plánovaná verzia s PostgreSQL.
Ukážková je iba vstupná sada dát a úložisko H2 v pamäti. Nič sa neinštaluje ako databázový server.
Pri reštarte sa nové účty a objednávky vymažú. Toto je výslovne označené na stránke.
Profil demo počúva iba na 127.0.0.1 a pri štarte generuje dočasný podpisovací kľúč JWT.
Žiadne skutočné osobné dáta ani školské heslá do ukážky nevkladajte.

Prihlásenie sa drží iba v pamäti Angularu; obnovenie stránky znamená nové prihlásenie.
Editor profilu a služieb poskytovateľa ešte nemá webové obrazovky; API preň je pripravené.
Chat, hodnotenia a online platby sú ďalšie etapy.

## Vývoj frontendu oddelene

Backend (Maven profil pridá H2, Spring profil nastaví demo dáta):

```powershell
mvn -Pdemo spring-boot:run '-Dspring-boot.run.profiles=demo'
```

Frontend v druhom termináli:

```powershell
cd frontend
npm ci
npm start
```

Otvorte http://localhost:4200. Vývojový proxy presmeruje /api na http://127.0.0.1:8080.
Frontend nemá natvrdo nastavenú adresu školského servera.

## Manuálne zostavenie a testy

```powershell
npm --prefix frontend ci
npm --prefix frontend run build
mvn -Pdemo test
mvn -Pdemo -DskipTests package
java -jar target/fx-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo
```

-Pdemo test aktivuje aj integračný DemoFlowTest, ktorý testuje prihlásenie, načítanie katalógu,
výpočet ceny a celý priebeh objednávky cez skutočné API a H2.
Tento test nenahrádza odložené overenie na školskej PostgreSQL databáze.

## Prechod na školskú databázu

Nespúšťajte profil demo. Nastavte DB_URL, DB_USERNAME, DB_PASSWORD a JWT_SECRET podľa README.
Frontend volá rovnaké endpointy, takže na PostgreSQL nemusí meniť kód.
Ukážkové účty vytvára výhradne komponent s @Profile("demo").
