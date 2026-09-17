# Tower Idle

Joc idle de tower defence, **complet offline**. Engine-ul de simulare e separat de UI, ca mai târziu același cod să ruleze și pe server.

Inspirat de *The Tower* (bandă verticală, inamici care urcă, turn care trage singur) și *Exponential Idle* (puține upgrade-uri, scalare abruptă, progres când închizi aplicația).

## Stack acum

| Modul | Rol |
|---|---|
| `engine` | Java 25 pur: comenzi, tick, catch-up offline, save. Fără libGDX, fără rețea. |
| `client/core` | Renderer libGDX 1.14.2 + save local |
| `client/desktop` | Fereastră portrait pentru iterație rapidă |

Maven 3.9, Java 25. **Nu există server în build.** `server/` e doar un reminder pentru WebFlux + Kafka + Redis + Postgres.

## Cum rulezi din IntelliJ

Deschide **folderul proiectului** (sau `pom.xml` din rădăcină) cu *Open as Project*. IntelliJ importă cele 4 module Maven singur.

Înainte de primul run, pune Java 25 în două locuri:

1. *File > Project Structure > Project > SDK* -> `jdk-25.0.4`
2. *Settings > Build, Execution, Deployment > Build Tools > Maven > Runner > JRE* -> `jdk-25.0.4`

Pasul 2 e necesar pentru că `mvn` din PATH pornește pe Java 21.

Apoi în dropdown-ul de run configurations ai deja:

- **Tower Idle (desktop)** - pornește jocul
- **Engine tests** - rulează testele din `engine`

## Cum rulezi din terminal

```bat
set JAVA_HOME=C:\Program Files\Java\jdk-25.0.4
mvn install -DskipTests
mvn -pl client/desktop exec:exec
```

`mvn install` e nevoie o singură dată (și după ce schimbi `engine` sau `client/core`), pentru ca modulul desktop să găsească celelalte module. Testele: `mvn -pl engine test`.

Save-ul stă în `%USERPROFILE%\.towerdefence\save.txt`. La deschidere, engine-ul simulează timpul trecut (maxim un run / 8 ore), inclusiv auto-buy dacă e deblocat.

## Loop-ul de joc

Turnul stă sus, inamicii urcă pe bandă, turnul trage singur. Tu doar cumperi upgrade-uri.

Patru monede, patru orizonturi de progres:

| Monedă | Formă | Vine din | Se pierde la | Upgrade-uri |
|---|---|---|---|---|
| **coins** | disc auriu | kill-uri | fiecare run nou | taburile ATTACK și DEFENSE din joc |
| **shards** | romb mov | prestige / moartea turnului | niciodată | tab-ul META: FOUNDATION, FORTUNE, AUTO |
| **cores** | inel cyan | fiecare 10 waves record | niciodată | tab-ul CORES din meniu |
| **sigils** | triunghi roz | fiecare 25 waves record | niciodată | tab-ul AUTO: offline și automatizare |

### Stat-urile de run

12 stat-uri cumpărate cu coins, în două taburi, toate resetate la run nou:

- **ATTACK**: POWER, TEMPO, REACH (rază plus damage la distanță), MULTI (până la 4 gloanțe), CRIT, BOUNCE
- **DEFENSE**: HEALTH, REGEN, WALL (al doilea bazin de viață), W.REGEN, THORNS (țepi lângă turn), PUSH (knockback)

Multiplicatorii din MULTI, CRIT și BOUNCE sunt **plafonați** intenționat. Mută peretele mai departe fără să schimbe exponentul per wave care face run-urile să se termine.

### Tipuri de inamici

Apar pe o cadență fixă, deci simularea rămâne deterministă: **FAST** (triunghi galben, rapid și fragil), **TANK** (pătrat mov, gras și lent), **RANGED** (inel roz, se oprește și trage în turn, deci nu ajunge niciodată la el), **PROTECTOR** (romb teal, scutește tot restul cât trăiește), **BOSS** (cerc ambră, închide fiecare al zecelea wave).

### Cum vin exact cores și sigils

La fiecare **10 waves** atinse *ca record într-un tier* primești `tier` cores. Deci în tier 1 iei 1 core la wave 10, încă unul la 20, etc. În tier 3 iei **3 cores** per aceleași 10 waves. Sunt acordate o singură dată per milestone, nu se resetează, și ecranul de moarte arată cât mai e până la următorul.

Sigils merg pe aceeași regulă, dar la **25 de waves**, deci sunt mult mai rare.

Cores se cheltuie pe tab-ul **CORES**: OVERCHARGE (damage), REINFORCE (hp), REPAIR DRONES (regen), SALVAGE (coins).

Sigils se cheltuie pe tab-ul **AUTO**: NIGHT SHIFT (orele de progres offline), AUTO PRESTIGE (regulile de mai jos), FORMULA EDITOR.

### Automatizare

**AUTO** (shards) cumpără singur cel mai ieftin stat de run care are badge-ul `A` aprins. Badge-ul se comută direct pe card, deci controlezi exact ce atinge. Merge și cât timp aplicația e închisă.

**AUTO PRESTIGE** (sigils) încheie și repornește run-ul singur, după condiții pe care le combini din meniu: `prestige la wave >= N` și/sau `dacă run-ul stagnează N secunde`. Se evaluează în engine la granițele de wave, deci funcționează și în catch-up offline.

### Progres offline

Începe la **30 de minute** și se dublează cu fiecare nivel de NIGHT SHIFT, până la 12 ore. Costul se dublează la fel. Intenția: early game-ul se joacă, idle-ul se cumpără.

### Tiers

Tier-ul se alege din meniu, tab-ul **TIERS**. Fiecare tier înmulțește viața inamicilor cu 2.6 și recompensele cu 2.2 (coins) / 1.9 (shards). Înmulțitorul de viață e intenționat mai mare decât cel de recompense — un tier nou trebuie să fie un pas real, nu bani gratis. Ca să deblochezi tier-ul următor, ajungi la **wave 20** în cel curent, adică plafonul natural al primului run. Schimbarea tier-ului încheie run-ul curent și încasează shards (cere confirmare dacă ești în run).

### Alte reguli

- Gloanțele **călătoresc** pe bandă și lovesc unde e inamicul; dispar la marginea range-ului, deci REACH contează.
- **PRESTIGE** dă shards oricând, nu doar la wave-uri mari.

## Curba de balans

Două reguli țin jocul în viață, ambele acoperite de teste.

**Run-urile trebuie să se termine.** Damage-ul cumpărabil scalează cu `coinPerWave^(log(powerPerLevel)/log(powerCostGrowth))`. Dacă `enemyHpGrowth` scade sub asta, turnul câștigă teren la infinit și tot loop-ul de prestige dispare. Un test verifică direct inegalitatea, altul cere ca un run nesupravegheat să moară în 40 de minute simulate.

**Scurgerile trebuie să doară proporțional.** Damage-ul unui leak e legat de viața inamicului (`leakDamageFraction`), nu de un număr care crește liniar. Altfel HEALTH și WALL fac turnul invulnerabil și run-urile se lungesc la ore.

Cu numerele actuale, run-urile succesive cu auto-buy mor pe la wave 20, 67, 107, 126, 138 — converg, ceea ce te împinge spre tier-ul următor.

Clientul doar trimite `GameCommand` și desenează `GameSnapshot`. Tot ce e simulare stă în `engine`, acoperit de teste.

## Asset-uri

Singurul fișier binar din repo e fontul UI, `client/core/src/main/resources/ui-font.ttf` — Roboto, licență Apache 2.0. E rasterizat la runtime cu gdx-freetype la rezoluția reală a ferestrei, deci textul rămâne clar la orice dimensiune. Restul graficii e desenată din forme generate în cod, fără sprite-uri.

## Ce urmează

Nefăcut încă: abilitățile ultimate cu cooldown (Golden Tower, Death Wave, Chain Lightning, Black Hole), labs pe ceas real, upgrade-ul de viteză a jocului și editorul de formulă pentru auto-prestige.
