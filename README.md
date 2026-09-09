# PLanG fejlesztőkörnyezet

A PPKE ITK **PLanG** oktatási programozási nyelvének fejlesztőkörnyezete,
teljesen újratervezett, **Visual Studio Code** stílusú felülettel.

![PLanG IDE](docs/screenshot-dark.png)

## Indítás

```sh
java -jar plang.jar
```

## Fordítás forrásból

```sh
./build.sh          # fordítás + plang.jar
./build.sh run      # fordítás + azonnali indítás
```

Ha nincs `javac` a gépen, de van Eclipse-fordító:

```sh
ECJ_JAR=/eleresi/ut/ecj.jar ./build.sh
```

## A felület

| Terület | Tartalom |
| --- | --- |
| **Tevékenységsáv** (bal szél) | Kezelő, Futtatás és hibakeresés, Keresés; alul témaváltó és beállítások |
| **Oldalsáv** | a választott nézet gombjai és a program **vázlata** (program, eljárások, függvények) |
| **Szerkesztő** | szintaxiskiemelés, sorszámozás, behúzás-segédvonalak, hibaaláhúzás, keresősáv |
| **Fülek** | `névtelen.plang` (szerkesztő) és `Értelmezett program` (a feldolgozott sorok) |
| **Alsó panel** | `BEMENET` és `KIMENET` csatornák külön fülekkel |
| **Vizsgálópanel** (jobb oldal) | Változók (állapottábla), Kifejezés kiértékelése, Hívási verem |
| **Állapotsor** | futtatás, hibaszám, lépésszám, kurzorpozíció, kódolás, téma |

Sötét (Dark+) és világos (Light+) színséma is választható – a tevékenységsáv alsó
ikonjával vagy a `Nézet ▸ Téma váltása` menüponttal.

## Billentyűparancsok

| Parancs | Művelet |
| --- | --- |
| `Ctrl+N` | Új program |
| `Ctrl+O` | Megnyitás |
| `Ctrl+S` | Mentés |
| `Ctrl+B` | Értelmezés |
| `F5` | Futtatás |
| `Shift+F5` | Futtatás vége |
| `Ctrl+F` | Keresés / csere |
| `Ctrl+/` | Megjegyzés ki/be |
| `Ctrl+D` | Sor megkettőzése |
| `Alt+↑` / `Alt+↓` | Sor mozgatása |
| `Tab` / `Shift+Tab` | Behúzás növelése / csökkentése |
| `Ctrl+,` | Beállítások |

## Munkafolyamat

1. **Értelmez** (`Ctrl+B`) – a szöveget programsorokká alakítja, hibákat jelöl,
   és létrehozza a be-/kimeneti csatornák füleit.
2. A **BEMENET** fülre beírod a program bemenetét.
3. **Futtatás** (`F5`) – a program lefut, az állapottábla soronként mutatja a
   változók alakulását, a **KIMENET** fülön megjelenik az eredmény.
4. Az állapottábla egy sorára kattintva a szerkesztő kiemeli a hozzá tartozó
   programsort, a **Kifejezés kiértékelése** fa pedig lebontja az adott lépést.
5. **Futtatás vége** (`Shift+F5`) visszaáll szerkesztő módba.

## Alprogramok

Az eljárások és függvények kezelése – az eredeti alkalmazással egyezően – csak
kapcsolóval érhető el; ilyenkor jelenik meg a **Hívási verem** panel, valamint a
be-/kilépés gombjai:

```sh
java -Dhu.ppke.itk.plang.subprograms=on -jar plang.jar
```

## További kapcsolók

| Kapcsoló | Hatás |
| --- | --- |
| `-Dhu.ppke.itk.plang.subprograms=on` | alprogramok (eljárás, függvény) engedélyezése |
| `-Dhu.ppke.itk.plang.errorDlg=off` | belső hiba esetén párbeszédablak helyett kivétel |

## A projekt szerkezete

```
src/main/java/hu/ppke/itk/plang/
├── plang.java              belépési pont (téma + Look&Feel beállítása)
├── prog/                   a nyelv értelmezője és futtatója (változatlan)
└── gui/
    ├── MainFrame.java      a főablak (a munkafelületet fogja keretbe)
    ├── Workbench.java      a teljes VS Code stílusú munkafelület
    ├── StreamTabs.java     be-/kimeneti csatornák fülekkel
    ├── PrefDialog.java     beállítások
    ├── theme/Theme.java    színpaletták (Dark+ / Light+) és betűtípusok
    ├── editor/             kódszerkesztő, szintaxiskiemelés, sorszámozás
    └── widgets/            tevékenységsáv, fülsor, állapotsor, gombok, keresősáv
```

A `prog` csomag – a nyelv értelmezője – **érintetlen maradt**: a felújítás
kizárólag a felhasználói felületet érinti, a nyelvi viselkedés bitre azonos.
