#!/bin/sh
#
# A PLanG fejlesztőkörnyezet fordítása és csomagolása.
#
#   ./build.sh            – fordítás + plang.jar készítése
#   ./build.sh compile    – csak fordítás a build/classes könyvtárba
#   ./build.sh run        – fordítás után azonnali indítás
#
# Szükséges: egy Java fordító (javac) és futtatókörnyezet (java) a PATH-on.
# Ha nincs javac, de van ECJ (Eclipse Compiler for Java), állítsd be:
#   ECJ_JAR=/eleresi/ut/ecj.jar ./build.sh
#
set -e

SRC=src/main/java
RES=src/main/resources
OUT=build/classes
JAR=plang.jar
MAIN=hu.ppke.itk.plang.plang

rm -rf "$OUT"
mkdir -p "$OUT"

echo "==> Források fordítása…"
FILES=$(find "$SRC" -name '*.java')

if [ -n "$ECJ_JAR" ]; then
   java -jar "$ECJ_JAR" -nowarn -8 -encoding UTF-8 -d "$OUT" $FILES
elif command -v javac >/dev/null 2>&1; then
   javac -encoding UTF-8 -d "$OUT" $FILES
else
   echo "HIBA: nem található sem javac, sem ECJ_JAR." >&2
   exit 1
fi

if [ -d "$RES" ]; then
   echo "==> Erőforrások másolása…"
   cp -r "$RES"/* "$OUT"/
fi

case "$1" in
   compile)
      echo "Kész: $OUT"
      ;;
   run)
      echo "==> Indítás…"
      java -cp "$OUT" "$MAIN"
      ;;
   *)
      echo "==> $JAR összeállítása…"
      if command -v jar >/dev/null 2>&1; then
         jar cfe "$JAR" "$MAIN" -C "$OUT" .
      else
         (cd "$OUT" && zip -qr "../../$JAR" .)
         echo "FIGYELEM: nincs 'jar' parancs, a kézi csomagoláshoz add hozzá a"
         echo "          META-INF/MANIFEST.MF állományt Main-Class: $MAIN sorral."
      fi
      echo "Kész: $JAR   (indítás: java -jar $JAR)"
      ;;
esac
