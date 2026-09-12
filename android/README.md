# PLanG fejlesztőkörnyezet – Android (tablet)

A PLanG oktatási programozási nyelv fejlesztőkörnyezetének **Android tablet**
változata: ugyanaz a VS Code stílusú felület, ugyanazokkal a funkciókkal,
a megegyező értelmezővel.

## Mit tud? (funkcióparitás az asztali változattal)

| Funkció | Állapot |
| --- | --- |
| PLanG értelmezés (`prog` csomag, **változatlan forrásból**) | ✔ |
| Szerkesztő: szintaxiskiemelés, sorszámozás, behúzás-segédvonalak | ✔ |
| Automatikus behúzás, Tab/Shift+Tab blokkbehúzás | ✔ |
| Megjegyzés ki/be, sor megkettőzése, sor mozgatása fel/le | ✔ |
| Visszavonás / Újra (csoportosított, szóhatáros) | ✔ |
| Keresés és csere (találszámláló, kis/nagybetű kapcsoló) | ✔ |
| Ugrás sorra | ✔ |
| Kódkiegészítés (kontextusfüggő: típusnevek / azonosítók / kulcsszavak) | ✔ |
| Értelmezett program listája, szintaxisszínekkel + hibajelöléssel | ✔ |
| Be-/kimeneti csatornák külön fülekkel, állapotkiemeléssel | ✔ |
| Futtatás, lépésenkénti végrehajtás, folytatás töréspontig | ✔ |
| Töréspontok (koppintás a sorszámsávra) | ✔ |
| Állapottábla (LÉPÉS + változók, `???` / `###` jelölések) | ✔ |
| Kifejezés kiértékelése fa (lebontás, alprogramba lépés) | ✔ |
| Hívási verem (alprogram-módban) | ✔ |
| Sötét (Dark+) és világos (Light+) téma | ✔ |
| Beállítások: betűtípus/-méret, lépésszám, téma, segédvonalak | ✔ |
| ISO-8859-2 fájlkezelés, automatikus `.plang` kiterjesztés | ✔ |
| Legutóbbi fájlok listája (max. 8) | ✔ |
| Állapotsor: futás, hibaszám, lépésszám, kurzor, kódolás, téma | ✔ |
| Vázlat (program, eljárások, függvények, VÁLTOZÓK) | ✔ |
| Alprogramok (eljárás/függvény) mód | ✔ (Beállításokban kapcsolható) |
| Hibák közötti ugrás, hibaüzenet kiírása | ✔ |

## Építés

```sh
cd android
./gradlew :app:assembleDebug     # android/app/build/outputs/apk/debug/
./gradlew :app:assembleRelease   # aláírás nélküli release APK
```

Telepítés eszközre:

```sh
adb install app/build/outputs/apk/debug/app-debug.apk
```

Android Studio: nyisd meg az `android/` könyvtárat, és futtasd az
`app` konfigurációt.

**Követelmény:** JDK 17, Android SDK (compileSdk 34).

## Hogyan használja az asztali forrásokat?

Az értelmező (`src/main/java/hu/ppke/itk/plang/prog/`) **egyáltalán nincs
lemásolva**: a `copySharedSources` Gradle-feladat a tárológyár
`src/main/java` könyvtárából szinkronizálja a forrásokat a Swing-only
fájlok kiszűrésével (build időben, a `build/generated/` alá). Az `gui`
csomagból csak a három Swing-mentes osztály kerül be
(`ProgramLine`, `ExprNode`, `PlangSyntax`); a felület teljesen az
`hu.ppke.itk.plang.android` csomagban íródott újra.

A `-Dhu.ppke.itk.plang.subprograms=on` parancssori kapcsoló
asztali megfelelője itt a **Beállítások ▸ Alprogramok** kapcsoló.

## Fájlműveletek

Az Android fájlelérés-szabályai miatt a Megnyitás/Mentés a rendszer
fájlválasztóján keresztül történik (Storage Access Framework); a
kódolás ugyanúgy ISO-8859-2, mint az asztali változatban.
