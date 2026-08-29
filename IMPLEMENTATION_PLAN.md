# Healthy — implementációs terv (v0.3, jóváhagyásra vár)

> Termék-brief: `health-connect-dashboard-brief.md`. Design: `design.txt`
> (szöveges irány — ez a mérvadó) és `docs/design-mockup.png` (makett).
> Munkaszabályok: `CLAUDE.md`.
> Ez a dokumentum a **hogyan**-t rögzíti, és a döntések mögötti indoklást.
> A briefhez képest több ponton eltér — ezek az eltérések a 2. szakaszban,
> indoklással. Kód csak ennek jóváhagyása után készül.

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
| **Nincs saját adatbázis az MVP-ben** | A Health Connect *maga* az adatbázis; egy Room-réteg mellé azonnal kapnánk egy cache-érvénytelenítési problémát nulla haszonért. A beállítások (téma, mértékegység, időablak, név) DataStore Preferences-be mennek. |

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

A manifestbe kell: `<queries>` a `com.google.android.apps.healthdata`
csomagra, az ~55 `android.permission.health.READ_*` deklaráció, a
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

1. **Alvás-pontszám (84/100).** A Health Connectben nincs ilyen adattípus.
   A Mi Fitness a saját appjában számolja, és nem írja be. Vagy elhagyjuk a
   részletek-képernyőről, vagy magunk számolunk egyet a fázisokból — de
   akkor jelöljük, hogy a *mi* számításunk, nem a Xiaomié. **Javaslat:**
   elhagyni; a fázis-bontás önmagában többet mond.
2. **Az engedély-képernyő kapcsolói.** A Health Connect engedélyeit app
   nem tudja be-/kikapcsolni: a változtatás kizárólag a HC saját
   párbeszédpanelén vagy a rendszerbeállításokban történik. A képernyő
   marad, de **állapotkijelzés** lesz (megadva / nincs megadva) plusz egy
   gomb, ami a HC felületére visz. A makett kapcsolói félrevezetőek.
3. **„Valós idejű frissítés, ahogy új adat érkezik."** Ehhez háttérolvasás
   kell (`READ_HEALTH_DATA_IN_BACKGROUND`) és a HyperOS akkumulátor-
   korlátozásainak kijátszása. Az MVP megnyitáskor és húzásra frissít. A
   marketing-mondatot a README-ben ehhez igazítjuk — háttérfrissítés v2.
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
miért kér ötvenvalahány engedélyt egy dashboard.

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

Lezárva azóta: stack **natív Kotlin**, tárolás **nincs DB, csak DataStore**,
sötét téma **kell** (rendszer/világos/sötét), időablak **7 és 30 nap** (a 90
később), mértékegység-választó **kell**, betűtípus **Roboto**, és van
**onboarding**.

Ami nyitva maradt:

1. **Alvás-pontszám**: elhagyjuk (ezt javaslom), vagy saját számítást írunk,
   jelölve, hogy nem a Xiaomié? (7.4/1)
2. **Megszólítás** („Good morning, …") — az app nem tud nevet a Health
   Connectből. Elhagyjuk, vagy a beállításokban megadható?
3. **Dinamikus szín** a kereten a fix metrika-accentek mellett — elfogadod a
   7.1-ben javasolt feloldást?
4. **Aláírt release build** kell-e, vagy elég a debug APK a saját telefonodra.
5. A `design.txt` végén ígért **további high-fidelity képernyőtervek**:
   megvárjuk őket, vagy F0 indulhat? A váz és az adatréteg nem függ tőlük.

## 12. Következő lépés

A terv jóváhagyása után **F0**: Gradle projekt-váz generálása,
`hu.galambos.healthy` package, a manifest engedély-deklarációkkal, és egy üres
dashboard, ami elindul a telefonon. Ez után a `CLAUDE.md` parancs-szekciója
kitölthető valódi, ellenőrzött build-parancsokkal.
