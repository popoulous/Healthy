# Healthy — implementációs terv (v0.4 — az MVP elkészült)

> Termék-brief: `health-connect-dashboard-brief.md`. Design: `design.txt`
> (szöveges irány — ez a mérvadó) és `docs/design-mockup.png` (makett).
> Munkaszabályok: `CLAUDE.md`.
> Ez a dokumentum a **hogyan**-t rögzíti, és a döntések mögötti indoklást.
> A briefhez képest több ponton eltér — ezek az eltérések a 2. szakaszban,
> indoklással.
>
> **Állapot (2026-08-29): az F0–F9 elkészült.** Amit a megvalósítás felülírt a
> tervhez képest, azt a 13. szakasz sorolja. A mérleg BLE-olvasása (§15) és a
> helyi tárolás inkrementális szinkronnal (§16) is kész — utóbbi visszavonta a
> §2 „nincs adatbázis" döntését, indoklással.
>
> **Valódi eszközön igazolva (2026-08-29, Xiaomi 17T Pro + Mi Body Composition
> Scale 2):** az alvásfázisok megvannak (39 szegmens, könnyű/mély/REM), és a
> mérleg BLE-olvasása végigmegy a nyers bájtoktól a testösszetételig. A
> részleteket a 17. szakasz írja le.

---

## 1. Tézis

Egyetlen, olvasó Android app, ami a Health Connectben lévő **összes** elérhető
egészségadatot megmutatja egy dashboardon, forrás-app és időbélyeg
megjelölésével, tetszőleges időtávra visszamenőleg. Nem ír, nem szinkronizál,
nem küld ki semmit a telefonról.

A projekt nehézsége nem a Health Connect API — az egyszerű. A nehézség az,
hogy **62 rekordtípus van, típusonként külön engedéllyel és külön
lekérdezéssel**, és hogy a nyers adat mennyisége (pulzus: percenként több
minta, évekre visszamenőleg) egy naiv implementációt használhatatlanná tesz.
Az architektúra erre a két dologra válasz.

---

## 2. Döntések és indoklásuk

| Döntés | Indoklás |
|---|---|
| **minSdk 34** (a brief 28-at mondott) | Android 14 felett a Health Connect a platform része. minSdk 34-gyel kiesik a „nincs telepítve → Play Store" ág és a régi `ACTION_SHOW_PERMISSIONS_RATIONALE` activity is. A célkészülék Android 16, tehát ez semmit nem vesz el, viszont két olyan kódág nem születik meg, amit soha nem tudnánk letesztelni. |
| **`READ_HEALTH_DATA_HISTORY` kötelező** | Android 14+ alatt az app a *saját* adatát korlátlanul olvassa, de *idegen app* adatát csak 30 napra vissza. Itt minden adatot idegen app ír (Mi Fitness, Zepp Life / Google Fit), tehát enélkül az app 30 napnál nem lát régebbre. |
| Az engedélyt **az első futásnál** kérjük, a többivel együtt | A 30 napos ablak az *első engedélymegadás* dátumához rögzül, nem gördül. Ha a history-engedély később jön, a köztes idő nem nyerhető vissza. App-törlés után az ablak nullázódik — ezt a README-ben rögzíteni kell. |
| **Trend = aggregátum, nem nyers olvasás** | A `readRecords` alap lapmérete 1000 rekord. Egy év pulzusadat több százezer minta, ez több száz lapozás egyetlen kártyáért. Az `aggregateGroupByPeriod` a Health Connect oldalán összegez. |
| **Nyers olvasás csak a legfrissebb rekordra** | A brief forrás-appot és időbélyeget kér — ezek csak nyers rekordon vannak, aggregátumon nincsenek. Egy rekord viszont olcsó. |
| **Cikluskövetés kimarad** (7 típus) | Nincs rá adatforrás a készüléken, és az engedélykérő listát feleslegesen hosszabbítja. Ha mégis kell, egy sor a regiszterben. |
| **Medical records / FHIR kimarad** | Külön engedélykör és Google-jóváhagyás kell hozzá, és semmi köze az óra/mérleg adataihoz. |
| **Nincs háttérolvasás az MVP-ben** | A `READ_HEALTH_DATA_IN_BACKGROUND` csak akkor kell, ha az app magától frissül (widget, értesítés). Az MVP megnyitáskor olvas. |
| **Egy Gradle modul** | Az app egy képernyő plusz egy adatréteg. A modulokra bontás itt csak ceremónia lenne. |
| **Natív Kotlin, nem React Native** | A Health Connect Android-only, tehát az RN fő haszna (egy kódbázis két platformra) itt nem létezik — iOS-en a HealthKit más API, más adatmodell. A `react-native-health-connect` wrapper 40+ típust fed le a 62-ből, miközben az app teljes létjogosultsága az, hogy *mindent* mutat: a szűk keresztmetszet maga a wrapper lenne. A projekt nehéz részei ráadásul mind az SDK határán vannak (history-engedély, feature-check, aggregátumok, lapozás, kvóta) — ezeket a wrapper mögül kétszer olyan nehéz debuggolni. |
| **Nincs saját adatbázis az MVP-ben** ~~(visszavonva, lásd §16)~~ | A cache-érvénytelenítési érv téves volt: a Health Connectnek van változáskövető API-ja, és az inkrementális szinkron a támogatott minta. Room jön — a napi bucketeknek, a legfrissebb rekordoknak és a mérleg méréseinek. Nyers mintákat továbbra sem tárolunk, de nem elvből, hanem mert soha nem mutatjuk meg őket. |

---

## 3. Tech stack (ellenőrzött verziók, 2026-08-29)

- Kotlin, Jetpack Compose (Material 3), Compose BOM `2026.08.00`.
- `androidx.health.connect:connect-client:1.1.0` (stabil; az `1.2.0-alpha06`
  tudatosan nem).
- AGP `9.3.0` → Gradle `9.5.0`, **JDK 17** (minimum és default).
- `compileSdk` / `targetSdk` 36, `minSdk` 34.
- ViewModel + `StateFlow`, coroutines. Nincs DI-keretrendszer (kézi
  konstruktor-injektálás elég ekkora felülethez).
- **DataStore Preferences** a beállításokhoz. Adatbázis nincs (lásd §2).
- Betűtípus: **Roboto**, a rendszerfont. A `design.txt` ezt ajánlja a natív
  érzés miatt, a makett Intert használ — a Roboto mellett döntök, mert nem
  kell beágyazni, és letöltő font provider amúgy sem jöhet szóba (az app nem
  hálózatozik). Ha mégis az Inter kell, az egy sor a témában plusz egy asset.
- **Nincs chart könyvtár.** A sparkline-ok, oszlopdiagramok és az
  alvásfázis-sáv Compose `Canvas`-szal készülnek. Egy diagram-library ekkora
  igényhez aránytalan függőség, és a legtöbbjük úgyis csak ezt csomagolja be.

**Függőséglista, teljes egészében:** Compose (BOM), `connect-client`,
DataStore, ViewModel/Lifecycle, Navigation. Ennyi. Minden további
függőséghez indoklás kell — az app egyik ígérete, hogy semmit nem küld ki a
telefonról, és ezt egy rövid függőséglista tartja hitelesen ellenőrizhetően.

**Buildkörnyezet:** a PATH-on lévő `java` JDK 26, amit az AGP nem támogat.
A `JAVA_HOME` az Android Studio bundle-olt JBR-jére mutasson. Az SDK-ban
`android-35` és `android-36.1` van telepítve; a `compileSdk = 36` platformot
az első build le fogja tölteni.

**Verzió-fegyelem:** a pontos engedély-stringeket és feature-konstansokat a
váz elkészültekor **a függőségből** ellenőrizzük (merged manifest, illetve a
`HealthPermission` és `HealthConnectFeatures` osztályok), nem dokumentációból
másolva.

---

## 4. Architektúra

Négy réteg, felülről lefelé függve:

```
ui/          Compose dashboard, ViewModel, UI state
domain/      MetricDescriptor regiszter, MetricSummary, engedély-állapotok
data/        HealthRepository interfész
data/hc/     HealthConnectRepository (az egyetlen hely, ami a HC SDK-t ismeri)
data/fake/   FakeRepository (preview + unit teszt)
```

### 4.1 A központi absztrakció: `MetricDescriptor`

Ez a projekt lelke. Egy adatosztály **rekordtípusonként egy példány**, és
ebből az egy regiszterből származik **minden**: az engedélylista, a
lekérdezés módja, és a dashboard kártyái.

```kotlin
data class MetricDescriptor<T : Record>(
    val id: MetricId,                    // stabil kulcs az UI state-hez
    val recordType: KClass<T>,           // ebből jön a read permission
    val category: MetricCategory,        // Activity / Body / Vitals / Sleep / Nutrition / Wellness
    val titleRes: Int,
    val readStrategy: ReadStrategy,
    val formatter: ValueFormatter,
)
```

Új típus felvétele = egy sor a regiszterben. Ez az, ami az „55 típus"-t
kezelhetővé teszi.

### 4.2 `ReadStrategy` — a három olvasási mód

Nem minden típushoz létezik aggregátum; a stratégia ezt kódolja.

| Stratégia | Mikor | Hogyan | Példa |
|---|---|---|---|
| `Aggregated` | van hozzá aggregate metrika | `aggregateGroupByPeriod` napi bontásban + legfrissebb nyers rekord | lépés (`COUNT_TOTAL`), kalória (`ACTIVE_CALORIES_TOTAL`, `ENERGY_TOTAL`), pulzus (`BPM_AVG/MIN/MAX`), súly (`WEIGHT_AVG/MIN/MAX`), távolság |
| `LatestOnly` | nincs aggregátum, pontszerű mérés | legfrissebb rekord + korlátozott ablakú minta a trendhez | SpO2, vérnyomás, testhőmérséklet, testzsír |
| `Session` | időtartam-alapú | maga a session, fázisokkal | alvás (`SLEEP_DURATION_TOTAL` + `stages`), edzés |

### 4.3 `HealthRepository` — a stabil interfész

```kotlin
interface HealthRepository {
    fun availability(): HealthConnectAvailability
    suspend fun grantedPermissions(): Set<String>
    suspend fun hasHistoryAccess(): Boolean
    suspend fun loadSummary(descriptor: MetricDescriptor<*>, range: TimeRange): MetricSummary
}
```

Miért interfész: emulátoron nincs Mi Fitness adat. A `FakeRepository` nélkül
a Compose preview és minden teszt valódi telefont igényelne. Ez a Movora
„stabil interfészek" elvének helyi megfelelője — a v2 (widget, export) új
implementációként csatlakozik, nem átírásként.

### 4.4 Domain modell

```kotlin
data class MetricSummary(
    val id: MetricId,
    val latest: DataPoint?,          // érték + időbélyeg + forrás app (dataOrigin)
    val trend: List<Bucket>,         // napi bontás a kért ablakra
    val state: LoadState,            // NotGranted / Empty / Loaded / Failed
)
```

Az `Empty` és a `NotGranted` **külön állapot**: „nincs rá engedélyed" és „van
engedélyed, de nincs adat" a felhasználónak két különböző üzenet, és a kettő
összemosása pont az a hiba, amit ennél az appnál a legkönnyebb elkövetni.

---

## 5. Engedély-flow

1. `HealthConnectClient.getSdkStatus(context)` — minSdk 34 mellett
   gyakorlatilag mindig `SDK_AVAILABLE`, de az ellenőrzés benne marad (olcsó,
   és a provider-frissítést kérő állapot valós eset).
2. A szükséges engedélyhalmaz a regiszterből generálódik:
   `descriptors.map { HealthPermission.getReadPermission(it.recordType) }`
   plusz a history-engedély.
3. `getGrantedPermissions()` → ami hiányzik, azt a Health Connect saját
   permission contractjával kérjük (`ActivityResultContracts`).
4. **Részleges megadás a normális eset, nem hibaág.** A HC engedélyképernyőjén
   a felhasználó típusonként pipál. Az app azzal működik, amit kapott: a nem
   engedélyezett típusok kártyái `NotGranted` állapotban, egy „engedélyezés"
   gombbal.
5. A history-engedély elérhetőségét feature-check-kel kérdezzük
   (`HealthConnectFeatures.getFeatureStatus(...)`), a konstans pontos nevét a
   függőségből ellenőrizve. Ha nincs meg, a UI jelzi, hogy 30 napnál régebbre
   nem lát.

**Mérve, nem becsülve (F0):** a `connect-client` 1.1.0 AAR-jából kiolvasva a
62 rekordtípus **38 olvasási engedélyre** képződik le — több típus osztozik
egy engedélyen. A cikluskövetés öt engedélyét elhagyva **33 marad**, a
history-engedéllyel 34. Az F3 tehát lényegesen kisebb, mint az „ötvenvalahány
engedély" becslés sugallta. A teljes lista bármikor újra kinyerhető az AAR-ból,
ezért nem másoljuk ide.

A manifestbe kell: `<queries>` a `com.google.android.apps.healthdata`
csomagra, a 33 `android.permission.health.READ_*` deklaráció, a
history-engedély, és a `ViewPermissionUsageActivity` activity-alias
(`android.permission.START_VIEW_PERMISSION_USAGE`, `VIEW_PERMISSION_USAGE`
action, `HEALTH_PERMISSIONS` kategória). Az alias mögötti activity a privacy
policy-t mutatja — mivel az app nem küld ki adatot, ez rövid lesz, de kötelező.

---

## 6. Lekérdezési stratégia és kockázatok

- **Kvóta.** A `readRecords` rate limit esetén `IllegalStateException`-t dob.
  55 típus párhuzamos lekérdezése megnyitáskor reális kvótakockázat.
  Ellenintézkedés: korlátozott párhuzamosság, kártyánként lusta betöltés
  (csak ami a képernyőn látszik), memóriában cache, exponenciális backoff.
- **Lapozás.** A `pageToken` a HC-verziótól függően `null` *vagy üres string*
  a végén — `isNullOrEmpty()` a helyes ellenőrzés. (Dokumentált csapda.)
- **Időzóna.** Az `aggregateGroupByPeriod` `LocalDateTime`-ot vár, a nyers
  olvasás `Instant`-ot. A napi bontás a felhasználó zónája szerint értelmes,
  tehát a kettő konverziója egy helyen, tesztelten.
- **Ismeretlen mennyiség.** Nem tudjuk előre, meddig nyúlik vissza az adat.
  Az MVP alapértelmezése 30 nap napi bontásban, a teljes időtáv opció; a
  lekérdező réteg időablakra paraméterezett, tehát ez UI-döntés marad.

---

## 7. UI

A `design.txt` a mérvadó, a `docs/design-mockup.png` az illusztráció; ahol a
kettő eltér, a `design.txt` nyer. A koncepció onnan: **„Personal Health
Console"** — adatközpont, nem fitness app. Nincs gamification, badge,
motivációs szöveg, nincs diagnózisra utaló vizuális elem.

Négy fül (bottom navigation), plusz egy metrika-részletek képernyő és egy
kétlépcsős onboarding az engedélykérés előtt:

| Fül | Tartalom |
|---|---|
| **Áttekintés** | fejléc (dátum, frissítés), „mai összegzés" három kiemelt értékkel, alatta kétoszlopos metrika-grid |
| **Trendek** | minden metrika egymás alatt, gyors összehasonlításra — nem mély analitika |
| **Források** | forrás-appok, és forrásonként **mit** írnak a Health Connectbe; „mi az a Health Connect" magyarázat |
| **Beállítások** | téma (rendszer/világos/sötét), mértékegységek, frissítés, HC-engedélyek, névjegy |

### 7.1 Design rendszer (a `design.txt` 3. és 13. pontja)

- A háttér semleges, **az accent szín a metrikáé**: lépés `#4CAF50`, pulzus
  `#E53935`, alvás `#6C5CE7`, SpO₂ `#20A4B8`, kalória `#F57C00`, súly
  `#2F80ED`. A szín az ikonra, a mini chartra és a trend-jelzőre kerül —
  **nem a kártya hátterére**.
- Világos: `#F7F7F3` háttér, `#FFFFFF` felület. Sötét: `#121417` háttér,
  `#1B1F24` felület, `#23272D` kiemelt kártya.
- 16–24 dp lekerekítés, visszafogott elevation, sok whitespace.
- Tipográfia: érték 32–40sp bold, képernyőcím 24sp, szekciócím 16sp,
  metrika-név 14sp, metaadat 12sp. **A szám sokkal nagyobb az egységénél.**
- Animáció visszafogottan: betöltéskor fade + stagger, a chart finoman
  rajzolódik ki, pull-to-refresh a Material 3 natív animációjával.

**Feloldandó ütközés:** a makett „Material You (dinamikus szín)" elvet
hirdet, a `design.txt` fix hexákat ad — a kettő együtt nem megy. **Javaslat:**
a metrika-accentek fixek maradnak, mert jelentést hordoznak (a piros a pulzus,
nem dekoráció), a dinamikus szín pedig a kereten dolgozik: navigáció,
kijelölés, gombok. Így van saját karaktere, de illeszkedik a rendszertémához.

### 7.2 Kártya-anatómia

Minden metrika ugyanaz a komponens: ikon + név, nagy érték + egység, mini
trend, utolsó frissítés, forrás (`● Mi Fitness` — kis színes pötty plusz név).
Kétoszlopos grid. A `MetricDescriptor` tölti fel, tehát a hatból az ötvenötbe
növés nem új komponens, csak több sor a regiszterben.

### 7.3 Az „összes adat" és a „ne legyen túl sok adat egyszerre"

A `design.txt` kifejezetten kerülendőnek nevezi a túlzsúfolt felületet,
miközben a kérés az összes típus megjelenítése. Feloldás: az Áttekintés teteje
a három kiemelt érték és a hat fő metrika, **alatta kategóriánként
összecsukott szekciók** a többivel, üresek alapból elrejtve. Az alapélmény a
tervezett dashboard marad, de minden adat egy koppintásra ott van.

### 7.4 Amit a design ígér, de a Health Connect nem ad

Ez a négy pont tervezési döntést igényel, mert szó szerint implementálva mind
a négy hazugság lenne a felületen:

1. **Alvás-pontszám.** A Mi Fitness 94 pontot és „Kiváló" minősítést mutat,
   plusz egy korcsoportos percentilist. Egyik sem kerül be a Health
   Connectbe. **Döntés:** saját, átlátható pontszámot számolunk a
   fázisarányokból (lásd 7.7), és a felületen jelöljük, hogy ez a *mi*
   számításunk. A korcsoportos összehasonlítás elmarad — ahhoz populációs
   adat kellene, ami nincs és nem is lesz.
2. **Az engedély-képernyő kapcsolói.** A Health Connect engedélyeit app
   nem tudja be-/kikapcsolni: a változtatás kizárólag a HC saját
   párbeszédpanelén vagy a rendszerbeállításokban történik. A képernyő
   marad, de **állapotkijelzés** lesz (megadva / nincs megadva) plusz egy
   gomb, ami a HC felületére visz. A makett kapcsolói félrevezetőek.
3. **„Valós idejű frissítés, ahogy új adat érkezik."** Ehhez háttérolvasás
   kell (`READ_HEALTH_DATA_IN_BACKGROUND`) és a HyperOS akkumulátor-
   korlátozásainak kijátszása — ez v2. **Frissítési szabály az MVP-ben:**
   indításkor, **minden előtérbe hozáskor** (`Lifecycle.State.RESUMED`), és
   pull-to-refresh-re. Ez lefedi a valós használatot: az ember megnyitja az
   appot, átvált a Mi Fitnessre szinkronizálni, visszavált — és friss adatot
   lát. Egy **throttle** kell mellé (kb. 60 mp), különben a fülek közti
   ugrálás ötvenvalahány típust kérdez újra, és belefutunk a HC kvótájába.
   A README ehhez igazodik: „frissül, amikor megnyitod", nem „valós idejű".
4. **„Nyugalmi pulzus" a kiemelt sávban.** Ez külön rekordtípus
   (`RestingHeartRateRecord`), nem ugyanaz, mint a napi átlagpulzus. Hogy a
   Mi Fitness ír-e ilyet, csak a te telefonodon derül ki — F2 után nézzük
   meg, és ha nincs, a kártya átlagpulzust ír, úgy is címkézve.

Egy apróság: a designban „Good morning, **Ádám**!" szerepel. Ez placeholder;
az app nem tud nevet a Health Connectből, tehát vagy elhagyjuk a
megszólítást, vagy a beállításokban megadható.

### 7.5 A Források képernyő rejtett költsége

A `design.txt` 9. pontja forrásonként listázná, *mit* ír az adott app — és ez
a képernyő válaszolja meg az app egyik legfontosabb kérdését: „miért nincs itt
a testzsír adatom?". Technikailag viszont a rekord `dataOrigin`-ja
**csomagnevet** ad (pl. `com.xiaomi.wearable`), nem megjelenítendő nevet.
Ahhoz, hogy „Mi Fitness" jelenjen meg, a `PackageManager`-től kell címkét
kérni, amihez Android 11 felett a manifest `<queries>` szekciójában
deklarálni kell a lekérdezett csomagokat. **Terv:** a három ismert forrást
névre deklaráljuk, minden más forrásnál a csomagnév látszik.
`QUERY_ALL_PACKAGES` szóba sem jön.

Azt, hogy egy forrás mely típusokat írja, nem lehet előre lekérdezni: abból
derül ki, amit a beolvasott rekordok `dataOrigin`-jai ténylegesen mondanak.
A képernyő tehát a betöltött adatokból épül, nem egy előre ismert táblából.

### 7.6 Onboarding

Két képernyő az engedélykérés előtt (`design.txt` 10.): mit csinál az app
(privát / a te eszközödön / csak olvas), majd mit fog olvasni. Ez nem
kozmetika — a Health Connect engedélyképernyője önmagában nem magyarázza meg,
miért kér harmincvalahány engedélyt egy dashboard.

### 7.7 Alvás-részletek — a Mi Fitness képernyő rekonstrukciója

A `D:\mifitness` képernyőképek alapján (ezek **nem kerülnek a repóba**, mert
valódi egészségadatot tartalmaznak, a repó pedig nyilvános). A Mi Fitness
alvás-nézete hét blokkból áll; alább, hogy melyik mit igényel:

| Mi Fitness blokk | Health Connectből? | Terv |
|---|---|---|
| Összes alvásidő + forrás | ✅ `SleepSessionRecord`, `dataOrigin` | 1:1 átvehető |
| Elalvás / ébredés időpontja | ✅ a session `startTime` / `endTime` | 1:1 |
| Hipnogram (Mély / Könnyű / REM sáv) | ✅ `stages` lista | 1:1, Canvas-szal |
| Fázis-donut % + időtartam | ✅ a `stages` összegzéséből | 1:1 |
| Referenciaértékek (REM 10–30%, könnyű 20–60%, mély 20–40%) | ⚠️ nem adat, hanem klinikai tartomány | fixen beírjuk, forrás megjelölve |
| Alvás alatti átlag pulzus / véroxigén / légzésszám | ⚠️ közvetve | a session időablakára aggregálunk `HeartRateRecord`, `OxygenSaturationRecord`, `RespiratoryRateRecord` fölött |
| Pontszám (94, „Kiváló") + korcsoportos percentilis | ❌ nincs a HC-ben | saját pontszám (lásd lent); a percentilis elmarad |
| Szöveges értelmezés, „alvásjavító terv" | ❌ licencelt tartalom (World Sleep Society stb.) | kimarad |

**A saját pontszám.** Nem a Xiaomi képletét próbáljuk visszafejteni — az nem
publikus, és a találgatás rosszabb, mint a semmi. Helyette átlátható,
kiszámolható érték: az alvásidő és a három fázisarány távolsága a fenti
referencia-tartományoktól, plusz az ébredések száma. A képlet a kódban egy
helyen áll, a felületen pedig oda van írva, hogy ez a **Healthy** pontszáma,
nem a Mi Fitnessé. Így a szám összehasonlítható marad önmagával az idő
során, és senki nem hiszi azt, hogy a Xiaomiét látja.

**Amit F2 után ellenőrizni kell a telefonodon:** a Mi Fitness beírja-e
egyáltalán a *fázisokat* a Health Connectbe (lehet, hogy csak összesített
alvásidőt ír), és ír-e `RespiratoryRateRecord`-ot. Ha a fázisok nincsenek
meg, a donut, a hipnogram és a pontszám is elesik — akkor az alvás-kártya
csak időtartamot mutat. Ez a terv legnagyobb egyedi kockázata; F2 pont
ezért néz rá korán a valódi adatra.

---

## 8. Fázisok

- **F0 — váz.** Gradle projekt, `hu.galambos.healthy`, Compose, HC függőség,
  manifest az engedélyekkel, téma (paletta, tipográfiai skála,
  világos/sötét), navigáció a négy fül között üres képernyőkkel. Kimenet:
  fordul és elindul.
- **F1 — onboarding + engedélyek.** A két onboarding képernyő, SDK-státusz,
  engedélykérő flow,
  history-engedély, részleges megadás kezelése, rationale activity, és az
  engedély-képernyő (állapot + ugrás a HC-be). Kimenet: telefonon megkapja
  az engedélyeket.
- **F2 — egy metrika végig.** Súly (ritka, pontszerű, van aggregátuma).
  Repository, descriptor, kártya sparkline-nal, fake implementáció, teszt.
  Kimenet: **a valódi adatod látszik a telefonon** — az első pont, ahol a
  terv szembesül a valósággal.
- **F3 — a regiszter feltöltése.** Előbb a makett hat metrikája (ezek adják
  az Áttekintés tetejét), aztán a maradék ~49, kategóriánként. Kimenet:
  teljes Áttekintés fül.
- **F4 — Trendek fül és metrika-részletek.** **7 és 30 napos** váltó (a 90
  nap a `design.txt` szerint is későbbre való), oszlopdiagram, és
  alvásfázis-bontás — utóbbi csak ha az adat tényleg tartalmaz fázisokat.
- **F5 — Források és Beállítások fül.** Forrás-appok a `dataOrigin` alapján,
  forrásonként azzal, hogy mit írnak (7.5); téma-választó, mértékegységek,
  „Refresh data", ugrás a HC engedélyekhez, névjegy. Itt jön be a DataStore.
- **F6 — csiszolás.** Üres és hibaállapotok, kvóta-backoff, lusta betöltés,
  ikon, README.

F2 után érdemes megállni és ránézni, mi van *tényleg* a Health Connectedben —
lehet, hogy a regiszter egy része sosem fog adatot mutatni, és az átrendezi az
F3 sorrendjét. Ugyanitt derül ki a 7.1 negyedik pontja (nyugalmi pulzus) is.

## 9. Non-goals (szándékos kihagyások)

Adatírás. Bármilyen hálózati hívás, felhő, analytics, crash-reporting. Több
felhasználó. Wear OS társapp. Medical records / FHIR. Cikluskövetés. Google Fit
közvetlen API (a HC-n át jön, ami kell). Értesítések, célok, kihívások.
Grafikonok, export, widget — ezek v2, nem MVP.

---

## 10. Tesztelés és CI

- **Unit teszt** a `FakeRepository` fölött: aggregátum → `Bucket` leképezés,
  időzóna-konverzió, `LoadState` levezetése (a `NotGranted` / `Empty`
  megkülönböztetés kifejezetten), formázók.
- **Nincs instrumentált teszt** az MVP-ben: a HC adat a készüléken él,
  emulátoron nincs mit olvasni. A valódi ellenőrzés kézi, a telefonon, `adb`
  fölött.
- **CI (GitHub Actions):** `./gradlew assembleDebug testDebugUnitTest lint`.
  Ehhez a runnernek JDK 17 kell, az SDK-t a `setup-android` action hozza.
  Akkor állítjuk be, amikor F0 lefordul — üres projektre CI-t írni értelmetlen.

---

## 11. Nyitott kérdések

Lezárva azóta: **alvás-pontszám** saját, átlátható számítással (7.7),
**megszólítás marad** (a név a beállításokból), **frissítés** indításkor,
előtérbe hozáskor és húzásra (7.4/3), stack **natív Kotlin**,
tárolás **nincs DB, csak DataStore**,
sötét téma **kell** (rendszer/világos/sötét), időablak **7 és 30 nap** (a 90
később), mértékegység-választó **kell**, betűtípus **Roboto**, és van
**onboarding**.

Ami nyitva maradt:

1. **Dinamikus szín** a kereten a fix metrika-accentek mellett — elfogadod a
   7.1-ben javasolt feloldást?
2. **Aláírt release build** kell-e, vagy elég a debug APK a saját telefonodra.
3. A `design.txt` végén ígért **további high-fidelity képernyőtervek**:
   megvárjuk őket, vagy F0 indulhat? A váz és az adatréteg nem függ tőlük.

## 12. Következő lépés

A terv jóváhagyása után **F0**: Gradle projekt-váz generálása,
`hu.galambos.healthy` package, a manifest engedély-deklarációkkal, és egy üres
dashboard, ami elindul a telefonon. Ez után a `CLAUDE.md` parancs-szekciója
kitölthető valódi, ellenőrzött build-parancsokkal.

---

## 13. Amit a megvalósítás felülírt

A terv jó volt, de öt ponton a valóság mást mondott. Ezek nem elfelejtett
döntések, hanem kijavítottak — itt maradnak, hogy ne kelljen újra végigmenni
rajtuk.

| Terv | Valóság | Miért |
|---|---|---|
| **minSdk 34** | **28** | Az A71 Android 13-on áll meg, és azon is futnia kell. Visszajött a „Health Connect nincs telepítve" ág és a régi rationale-intent is — és mindkettő azonnal élesben tesztelhető lett, mert az A71-en tényleg nem volt telepítve. |
| Gradle 9.5 | **9.7.1** | A 9.5 nem létező disztribúció; az AGP táblázata minimumot ír, nem kiadást. |
| `NavHost` | **nullable id + BackHandler** | Négy lapos fül és egy részletszint. A navigációs könyvtár egy verifikálatlan verziószám lett volna nulla haszonért. |
| `Icons.Filled.*` | **saját vektor ikonok** | A Compose már nem hozza a Material ikonkészletet. |
| ~55 engedély | **31 olvasási engedély** | A 62 rekordtípus 31 engedélyre képződik le; több típus osztozik egyen. Az AAR-ból kiolvasva, nem becsülve. |

**Az alvás-fázisok kockázata (7.7) még nyitva van.** Az A71-en nincs óra, tehát
nem derült ki, hogy a Mi Fitness beírja-e a fázisokat a Health Connectbe. A
kód mindkét esetet kezeli: fázisok nélkül csak időtartam látszik, pontszám
nélkül, és a képernyő megmondja, miért. Ez a 17T Prón dől el.

**Amit a telefon talált meg, nem a fordító:**
- Az engedély-cache versenyhelyzete: a dashboard ugyanabban a pillanatban
  indult, mint az engedély-ellenőrzés, és az üres cache-t válasznak vettük —
  minden kártya „nincs engedély"-t mutatott. A cache most nullable: a „még nem
  kérdeztük" más, mint a „semmi nincs engedélyezve".
- Az aggregate API nulla értékű bucketeket ad olyan napokra, amikor nem
  történt semmi. Ettől egy kártya adatot ígért, majd nem volt mit mutatnia.
  Most a legfrissebb rekord megléte dönti el, van-e adat.

## 14. Ami az MVP-ből tudatosan kimaradt

Háttérolvasás és widget (v2, `READ_HEALTH_DATA_IN_BACKGROUND` kell hozzá).
90 napos ablak. Adat-export. Aláírt release build. Grafikonok animációja a
betöltéskori fade-en túl. Instrumentált tesztek — az emulátoron nincs mit
olvasni, a valódi ellenőrzés a telefonon történik.

---

## 15. Testösszetétel a mérlegtől (BLE) — nem opció, hanem alapképesség

**A helyzet (ellenőrizve, 2026-08-29):** a Zepp Life **csak a testsúlyt** írja a
Health Connectbe. A testzsír, izomtömeg, csontsúly és testvíz nem kerül át, és
nincs olyan nyilvános Zepp API sem, amin elkérhető lenne. Health Connecten át
ez az adat nem létezik — nem az app korlátja, hanem a forrásé.

Ez viszont pont az a hiány, ami miatt az app egyáltalán elkészült: a mérleg
adata szét van szórva, és a felhasználó a saját méréséhez nem fér hozzá. Egy
dashboard, ami erre azt mondja, hogy „a forrásod nem osztotta meg", technikailag
igaz, de nem oldja meg a problémát. Ezért a mérleg **közvetlen olvasása az app
alapképessége, nem későbbi bővítés.**

**Ami lehetővé teszi:** a Mi Body Composition Scale 2 a BLE-hirdetésében
sugározza a **nyers súlyt és impedanciát** (`0x181B` service data). Párosítás
nélkül, bárki hallgathatja, aki hatótávon belül van. A testösszetételt nem a
mérleg számolja, hanem az app — a Zepp Life is ezt teszi.

### 15.1 Amit ez jelent, és amit nem

| Kérdés | Válasz |
|---|---|
| Meg tudjuk-e szerezni a nyers adatot? | Igen, passzív BLE-hallgatással. |
| Ugyanazt a testzsírt kapjuk, mint a Zepp Life? | **Nem feltétlenül.** A közösségi képlet (openScale „Xiaomi mód") a 2017-es Mi Fit algoritmusból visszafejtett, nem a mai Zepp Life-éból. Az openScale hibajegyei szerint egyes értékek elcsúsznak. |
| Sérti-e a „csak olvasás" szabályt? | Nem, amíg **nem írunk vissza** a Health Connectbe. |
| Sérti-e az „on-device" ígéretet? | Nem. A BLE rádió helyi; semmi nem megy hálózatra. |

### 15.2 Döntések, előre

1. **A számolt értékek a mieink, és ezt kiírjuk.** Pontosan úgy, ahogy az
   alvás-pontszámnál (7.4/1): nem utánozzuk a Zepp Life számát, mert egy
   ránézésre azonosnak tűnő, de máshogy számolt érték rosszabb, mint egy
   vállaltan saját. A kártyán ott lesz, hogy a Healthy számolta a mérleg nyers
   impedanciájából.
2. **Nem írunk vissza a Health Connectbe.** Csábító volna, hogy „minden egy
   helyen legyen", de a csak-olvasás az app első számú megkötése, és egy
   visszafejtett képletből származó értéket beírni mások adattárába rossz
   irány. A testösszetétel a Healthyben marad.
3. **Profil-adat kell** (magasság, életkor, nem) — a képlet enélkül nem
   működik. Beállítás lesz belőle. Ez az első adat, amit a felhasználó *megad*,
   nem pedig olvasunk.
4. **Új engedélyek**: `BLUETOOTH_SCAN` (a `neverForLocation` jelzővel, hogy ne
   kelljen helyadat-engedély) és `BLUETOOTH_CONNECT`. Az engedélylista része az
   app ígéretének, tehát a README-ben ki kell mondani, mit kér és miért.
5. **Tárolás kell hozzá — jön a Room.** Ez az első adat, aminek nincs Health
   Connect otthona, tehát a Healthynek kell megőriznie. Ez nem a §2 döntés
   visszavonása: a Health Connectből olvasott adatot továbbra sem másoljuk,
   csak azt tároljuk, aminek máshol nincs helye.

### 15.3 Kockázatok

- **Az impedancia nem mindig érkezik.** A mérleg csak stabilizálódott mérés
  után, mezítláb küldi; addig csak súly van a hirdetésben. A felületnek kezelnie
  kell a „megvan a súly, nincs testösszetétel" állapotot.
- **Időzítés.** A hirdetés akkor szól, amikor a mérlegen állsz. Első lépésben az
  app **előtérben figyel**: megnyitod, rálépsz, elkapja. Ez bizonyítja, hogy a
  protokoll működik, és nem kér cserébe se értesítést, se akkumulátort. A
  háttérben figyelő változat ezután jöhet, ha kell.
- **A bájt-elrendezést nem találgatjuk.** Az openScale és a Home Assistant
  integrációk a referencia; a konkrét mezőket implementációkor valódi mérésen
  ellenőrizzük, nem leírásból.
- **Két súlyforrás lesz.** A Health Connect (Zepp Life-on át) és a mérleg
  közvetlenül ugyanazt a súlyt adja, kissé eltérő időbélyeggel. A súly-kártyán a
  Health Connect marad a mérvadó, a BLE csak a testösszetételt adja — így nem
  lesz két, egymásnak ellentmondó szám ugyanarról.

### 15.4 Fázisok

- **F7 — a nyers adat.** BLE-engedélyek, előtérben futó szkennelés, a hirdetés
  értelmezése, és egy nyers kijelzés: súly + impedancia, ahogy jön. Kimenet: a
  mérlegre lépve megjelenik a nyers érték. Ez a kockázatos rész; ha ez megy, a
  többi számtan.
- **F8 — a testösszetétel.** A profil-beállítások, a képlet (tiszta függvény,
  tesztekkel), és a kártyák: testzsír, izomtömeg, csontsúly, testvíz — mind
  jelölve, hogy a Healthy számolta.
- **F9 — megőrzés.** Room: a mérleg mérései és a napi bucketek is (§16). Ezzel
  nyílik meg a hosszabb időablak és az azonnali hideg indítás.

---

## 16. Helyi tárolás és inkrementális szinkron

> **Javítás.** A §2-ben azt írtam, hogy egy másolat „cache-érvénytelenítési
> problémát" hozna. Ez egy általános aggály volt, amit nem ellenőriztem: a
> Health Connectnek **van rendes változáskövető API-ja**. A `getChangesToken` /
> `getChanges` pontosan azt adja, ami kell — beszúrásokat, módosításokat **és
> törléseket** —, tehát az inkrementális szinkron a támogatott, dokumentált
> minta, nem valami kerülőút. Az érvem gyengébb volt, mint ahogy előadtam.

Két oka van annak, hogy mégis tárolunk:

**1. Van adat, aminek nincs Health Connect otthona.** A testösszetétel a
mérlegtől jön (§15), és sehová máshová nem tehető.

**2. A trend nem trend, ha csak addig lát, ameddig épp beolvasunk.** A Health
Connect automatikus törlése a *felhasználó* beállítása, nem a miénk. Egy
súlykövetés, ami fél év múlva elfelejti a tavaszt, nem az, amiért ez az app
készült.

### 16.1 A szinkron

1. **Első alkalommal** végigolvassuk, ameddig az engedély enged (a
   history-engedéllyel a teljes múltat), és **napi bontásban** eltároljuk.
2. Kérünk egy **changes tokent**, és onnantól minden frissítés csak a token óta
   történt változásokat kéri le.
3. Az `UpsertionChange` megmondja, **mely napok** érintettek — azokat a napokat
   újraszámoljuk és felülírjuk.
4. A `DeletionChange` **csak az azonosítót adja, a típust nem** (adatvédelmi
   okból). Ezért törléskor nem tudjuk, melyik nap romlott el — ilyenkor az
   aktuális ablakot számoljuk újra. A törlés ritka, ez bőven megfizethető.
5. **A token 30 nap használatlanság után lejár.** Ilyenkor a dokumentált
   stratégia: az utolsó olvasás időbélyegétől újraolvasunk, és rekord-azonosító
   alapján deduplikálunk. Ezért az utolsó sikeres szinkron idejét is tároljuk.

### 16.2 Mit tárolunk, és mit nem

| Adat | Hol él | Miért |
|---|---|---|
| **Nyers minták** (pl. percenkénti pulzus) | Marad a Health Connectben | Nem azért, mert nem lehetne, hanem mert soha nem mutatjuk meg őket. Egyetlen nap pulzusa több ezer sor; egy évnyi olyan adatbázis, amiből egyetlen képernyő sem olvas. |
| **Napi bucketek** (metrika + nap → érték) | Room | Ez a trend, és ez az, ami túléli a Health Connect törlését. |
| **A legfrissebb rekord** metrikánként (érték, idő, forrás) | Room | A kártya fejléce. Egy sor metrikánként. |
| **Mérleg-mérések** | Room | Nincs más otthonuk (§15). |
| Beállítások, profil | DataStore | Ahogy eddig. |

Vagyis a szabály nem „ne másolj", hanem **„azt tárold, amit mutatsz"**. A napi
bucket és a legfrissebb rekord pont az, amit a felület kirajzol.

### 16.3 Amit ez megnyit

- **A 90 napos és a teljes időtáv**, amit a design költség miatt későbbre tett.
  Az archívumból azonnali.
- **Hideg indítás.** A dashboard a Roomból rajzol, a Health Connect a háttérben
  frissít. Nem harminchárom lekérdezésnyi üres képernyő.
- **Kvóta.** A változás-alapú frissítés töredékét kérdezi annak, amit a mostani
  „olvassunk újra mindent" ciklus.
- **A mérleg trendje.** Testzsír és izomtömeg időben, nem csak pillanatkép.

### 16.4 Sorrend

A Room az **F9**-ben érkezik (§15.4), egyszerre a mérleg méréseivel, a napi
bucketekkel és a changes-token alapú szinkronnal — nem érdemes kétszer
hozzányúlni ugyanahhoz a réteghez. Utána jön a hosszabb időablak, ami innentől
olcsó.

---

## 17. Amit a valódi eszköz megtanított

A 17T Pro és a mérleg négy hibát hozott elő, amit sem a fordító, sem a tesztek
nem találhattak meg. Mind a négy abból fakadt, hogy **dokumentációt hittem el
hardver helyett**.

| Amit hittem | Amit a hardver mondott |
|---|---|
| A 14-es bit jelzi, hogy az impedancia kész | A bit végig 0, az impedancia (419 Ω) közben ott van. Erre a feltételre kötve **soha semmit nem rögzítettünk volna**. |
| A mérleg helyi időt küld | UTC-t küld. Két órával korábbra datáltunk minden mérést. |
| A „stabilizálódott" bit végleges mérést jelent | Átmeneti értékeknél is be van állítva. Egy mérésből 23 sor lett, a végén 3,25 kg-mal — a lelépés pillanata, testsúlyként. |
| Elég a változás-API-ra bízni a frissítést | Igen, de a *kézi* frissítésre nem: aki megnyomja, épp azt gyanítja, hogy állott az adat. |

**Az impedancia a mérés egyetlen megbízható végjele.** A naplóban egy teljes
mérési sorozatban pontosan kétszer jelent meg, mindkétszer a valódi 95,1 kg-nál.
A rögzítés feltétele ez lett; a valódi hirdetés bekerült regressziós tesztnek.

**Egy ötödik hiba a tárolóban lapult:** két forrás ugyanarra a metrikára
(mérleg + Health Connect), és az nyert, amelyik később írt. Mivel a szinkron a
mérleg után fut, a friss súlyt minden előtérbe hozáskor felülírta egy régebbi.
Mostantól a *valóban legfrissebb* nyer.

### 17.1 Amit a Health Connectről megtudtunk

- **Testzsír nincs benne.** A brief állítása igazolva; a mérleg BLE-olvasása
  az egyetlen út. Most már ez adja a testzsírt, izomtömeget, testvizet,
  csonttömeget és az alapanyagcserét.
- **A Mi Fitness típusonként válogat.** Ugyanazon a napon beírta az alvást
  (06:02), a véroxigént (08:03) és az aktív kalóriát (10:29) — a **pulzust
  viszont három napja nem**, miközben a saját appjában percre friss. Nem
  engedély és nem hiba: a forrás dönt. Pontosan ezért van Források fül.
- **Az óra adata alig pár napra megy vissza** a Health Connectben, miközben a
  Google Fit származtatott metrikái 366 napot adtak. Ettől lett igazán értelme
  a helyi archívumnak: amit ma beolvasunk, azt megőrizzük, miután a Health
  Connect már elfelejtette.
