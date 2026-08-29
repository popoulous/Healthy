# Projekt-brief: Egységes Health Connect Dashboard app (Android)

## A projekt célja

Egy saját Android alkalmazás, ami **egyetlen felületen** jeleníti meg az összes
egészség- és fitneszadatot, amit a telefonon lévő Health Connect gyűjt — függetlenül
attól, hogy melyik gyártó melyik appja írta be. Csak **olvasó** ("dashboard") app:
nem ír adatot, csak kiolvassa és megjeleníti a Health Connectből.

## Miért van rá szükség (kontextus)

A Xiaomi ökoszisztéma szét van tagolva több app közé, és ezek nem folynak össze:
- **Mi Fitness** app → egy Xiaomi Watch S4 okosóra adatai (alvás, pulzus, kalória, lépés).
- **Zepp Life** app → egy Mi Body Composition Scale 2 mérleg adatai (testsúly, testösszetétel).
- Nincs olyan gyári app, ami mindkettőt egy nézetben mutatná.

A jó hír: **mindkét rendszer beleír a Health Connectbe** (részben Google Fiten át tükrözve).
A Health Connectben ellenőrzötten már megjelenik:
- Testsúly (Tömeg) — forrás: Google Fit / Zepp Life
- Alvás, pulzus, oxigéntelítettség, kalória, lépés — forrás: Mi Fitness

Vagyis a Health Connect már MOST egy közös adatmedence. Ez az app ebből a medencéből
olvas, és egy egységes dashboardon mutatja meg az adatokat.

## Fő funkciók (MVP)

1. Ellenőrzi, hogy a Health Connect elérhető-e a készüléken; ha nincs telepítve,
   irányítsa a felhasználót a telepítéshez.
2. Kéri a szükséges **olvasási** engedélyeket a Health Connecttől.
3. Kiolvassa és megjeleníti egy görgethető dashboardon a következő adatokat, mindegyiket
   a legutóbbi értékkel + egy rövid trenddel (pl. utolsó 7 nap):
   - **Testsúly** (WeightRecord)
   - **Alvás** (SleepSessionRecord) — összes időtartam, és ha elérhető, a fázisok
   - **Pulzus** (HeartRateRecord)
   - **Oxigéntelítettség / SpO2** (OxygenSaturationRecord)
   - **Aktív + összes elégetett kalória** (ActiveCaloriesBurnedRecord, TotalCaloriesBurnedRecord)
   - **Lépésszám** (StepsRecord)
4. Minden adatnál mutassa a forrás appot (data origin) és az időbélyeget.

## Technikai követelmények

- **Platform:** Android (natív), Kotlin.
- **UI:** Jetpack Compose (egyszerű, tiszta, kártyás dashboard elrendezés).
- **API:** hivatalos Health Connect Jetpack SDK — `androidx.health.connect:connect-client`
  (a legfrissebb stabil verzió).
- **Min SDK:** a Health Connect követelményének megfelelően (jellemzően Android 9 / API 28
  felfelé, a connect-client aktuális elvárása szerint).
- Nincs backend, nincs felhő — minden lokálisan, a telefonon fut, a Health Connectből olvasva.
- Nincs adatírás, nincs harmadik fél felé küldés — csak olvasás és megjelenítés.

## Konkrét megvalósítási lépések, amikben kérek segítséget

1. **Projekt-váz:** a `build.gradle` (app szintű) függőségek a Health Connecthez, és a
   `AndroidManifest.xml` a szükséges engedély-deklarációkkal (a Health Connect
   olvasási engedélyek külön kezelése az újabb Androidon).
2. **Elérhetőség-ellenőrzés:** `HealthConnectClient.getSdkStatus(...)` alapján kezelni,
   hogy a Health Connect telepítve/frissítve van-e.
3. **Engedélykezelés:** a szükséges olvasási engedélyek definiálása
   (pl. `HealthPermission.getReadPermission(WeightRecord::class)` a fenti összes
   rekordtípusra), és az engedélykérő flow (ActivityResultContracts a Health Connect
   permission contracttal).
4. **Adatlekérdezés:** `ReadRecordsRequest` időablakra (pl. utolsó 7 nap) mindegyik
   rekordtípushoz; a legutóbbi érték kiemelése + a napi/heti értékek listája.
5. **UI:** egy Compose dashboard, kártyánként egy metrika (súly, alvás, pulzus, SpO2,
   kalória, lépés), mindegyik mutatja a legutóbbi értéket, az időbélyeget és a forrást.
   Egyszerű, olvasható, sallangmentes.

## Jó tudni / korlátok

- Az app csak azt látja, ami TÉNYLEGESEN bekerült a Health Connectbe. Amit egy forrás-app
  nem oszt meg (pl. a Mi Fitness a testzsírt nem írja be, csak a súlyt a mérleg oldaláról),
  azt ez az app sem tudja megjeleníteni. Az MVP a fent felsorolt, ellenőrzötten elérhető
  adatokra épül.
- A cél elsőre egy működő, olvasó dashboard. Későbbi bővítés lehet: grafikonok, hosszabb
  trendek, adat-export, widget a kezdőképernyőre.

## Fejlesztői háttér

A fejlesztő tapasztalt (13+ év), tehát nem kell alapoktól magyarázni a Kotlint/Androidot —
a Health Connect-specifikus részek (engedélykezelés, rekordtípusok, lekérdezés) és egy
tiszta induló projekt-váz a leghasznosabb.
