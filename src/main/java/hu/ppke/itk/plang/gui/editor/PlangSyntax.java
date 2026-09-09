package hu.ppke.itk.plang.gui.editor;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A PLanG nyelv szintaktikai kategóriái.
 *
 * A kulcsszavak listája a {@code hu.ppke.itk.plang.prog.Lexer} osztályból
 * származik; az ékezetes alakokat a lexerrel azonos módon, ékezettelenítve
 * hasonlítjuk össze, így a KÜLÖNBEN és a KULONBEN is felismerhető.
 */
public final class PlangSyntax {

   /** Vezérlési szerkezetek (VSC-ben lila). */
   public static final Set<String> CONTROL = unmod(
      "ha", "akkor", "ha_vege", "kulonben",
      "ciklus", "amig", "ciklus_vege", "vege");

   /** Deklarációs és szerkezeti kulcsszavak (VSC-ben kék). */
   public static final Set<String> KEYWORD = unmod(
      "program", "program_vege", "eljaras", "eljaras_vege",
      "fuggveny", "fuggveny_vege", "valtozok",
      "ki", "be", "megnyit", "lezar", "sv");

   /** Típusnevek (VSC-ben zöldeskék). */
   public static final Set<String> TYPE = unmod(
      "egesz", "valos", "szoveg", "karakter", "logikai",
      "kifajl", "befajl");

   /** Logikai állandók (VSC-ben kék). */
   public static final Set<String> CONSTANT = unmod("igaz", "hamis");

   /** Beépített függvények (VSC-ben halványsárga). */
   public static final Set<String> FUNCTION = unmod(
      "sin", "cos", "tan", "log", "exp", "rnd",
      "arcsin", "arccos", "arctan",
      "kis", "nagy", "kerek", "betu", "szam", "div", "mod");

   /** Szöveges operátorok. */
   public static final Set<String> WORD_OPERATOR = unmod("nem", "es", "vagy");

   /** Az összes kulcsszó a kódkiegészítéshez, megjelenítési alakban. */
   public static final String[] COMPLETIONS = {
      "PROGRAM", "PROGRAM_VÉGE", "VÁLTOZÓK:",
      "ELJÁRÁS", "ELJÁRÁS_VÉGE", "FÜGGVÉNY", "FÜGGVÉNY_VÉGE",
      "HA", "AKKOR", "KÜLÖNBEN", "HA_VÉGE",
      "CIKLUS", "AMÍG", "CIKLUS_VÉGE", "VÉGE",
      "BE:", "KI:", "MEGNYIT", "LEZÁR",
      "EGÉSZ", "VALÓS", "SZÖVEG", "KARAKTER", "LOGIKAI",
      "BEFÁJL", "KIFÁJL",
      "IGAZ", "HAMIS",
      "ÉS", "VAGY", "NEM", "DIV", "MOD",
      "SIN", "COS", "TAN", "LOG", "EXP", "RND",
      "ARCSIN", "ARCCOS", "ARCTAN",
      "KIS", "NAGY", "KEREK", "BETŰ", "SZÁM", "SV"
   };

   /** A behúzást növelő kulcsszavak. */
   public static final Set<String> INDENT_OPEN = unmod(
      "program", "eljaras", "fuggveny", "akkor", "kulonben", "ciklus", "valtozok");

   /** A behúzást csökkentő kulcsszavak. */
   public static final Set<String> INDENT_CLOSE = unmod(
      "program_vege", "eljaras_vege", "fuggveny_vege",
      "ha_vege", "kulonben", "ciklus_vege", "vege");

   /** Token-fajták. */
   public static final int PLAIN = 0;
   public static final int KW = 1;
   public static final int CTRL = 2;
   public static final int TYPE_T = 3;
   public static final int STRING = 4;
   public static final int NUMBER = 5;
   public static final int COMMENT = 6;
   public static final int FUNC = 7;
   public static final int IDENT = 8;
   public static final int OPERATOR = 9;
   public static final int CONST = 10;

   /** Egy felismert token. */
   public static final class Token {
      public final int start;
      public final int end;
      public final int kind;

      Token(int start, int end, int kind) {
         this.start = start;
         this.end = end;
         this.kind = kind;
      }
   }

   private static final char[] ACCENT = { 'á', 'é', 'í', 'ó', 'ö', 'ő', 'ú', 'ü', 'ű' };
   private static final char[] NOACC = { 'a', 'e', 'i', 'o', 'o', 'o', 'u', 'u', 'u' };

   /** A lexerrel megegyező ékezettelenítés. */
   public static String deacc(String word) {
      String w = word.toLowerCase();
      for (int i = 0; i < ACCENT.length; i++) {
         w = w.replace(ACCENT[i], NOACC[i]);
      }
      return w;
   }

   public static boolean isWordChar(char c) {
      return Character.isLetterOrDigit(c) || c == '_';
   }

   /**
    * Egyetlen sor tokenizálása. A PLanG-ban a megjegyzés a {@code **}
    * jelöléssel kezdődik és a sor végéig tart.
    */
   public static List<Token> tokenize(String line) {
      java.util.ArrayList<Token> out = new java.util.ArrayList<Token>();
      int i = 0;
      int n = line.length();

      while (i < n) {
         char c = line.charAt(i);

         if (Character.isWhitespace(c)) {
            i++;
            continue;
         }

         // megjegyzés: ** a sor végéig
         if (c == '*' && i + 1 < n && line.charAt(i + 1) == '*') {
            out.add(new Token(i, n, COMMENT));
            break;
         }

         // szöveg- és karakterliterál
         if (c == '"' || c == '\'') {
            int j = i + 1;
            while (j < n && line.charAt(j) != c) {
               j++;
            }
            if (j < n) {
               j++;
            }
            out.add(new Token(i, j, STRING));
            i = j;
            continue;
         }

         // szám
         if (Character.isDigit(c)) {
            int j = i;
            while (j < n && (Character.isDigit(line.charAt(j)))) {
               j++;
            }
            if (j < n && line.charAt(j) == '.' && j + 1 < n && Character.isDigit(line.charAt(j + 1))) {
               j++;
               while (j < n && Character.isDigit(line.charAt(j))) {
                  j++;
               }
            }
            out.add(new Token(i, j, NUMBER));
            i = j;
            continue;
         }

         // azonosító vagy kulcsszó
         if (Character.isLetter(c) || c == '_') {
            int j = i;
            while (j < n && isWordChar(line.charAt(j))) {
               j++;
            }
            String raw = line.substring(i, j);
            String w = deacc(raw);
            int kind;
            if (CONTROL.contains(w)) {
               kind = CTRL;
            } else if (KEYWORD.contains(w)) {
               kind = KW;
            } else if (TYPE.contains(w)) {
               kind = TYPE_T;
            } else if (CONSTANT.contains(w)) {
               kind = CONST;
            } else if (FUNCTION.contains(w)) {
               kind = FUNC;
            } else if (WORD_OPERATOR.contains(w)) {
               kind = CTRL;
            } else {
               kind = IDENT;
            }
            out.add(new Token(i, j, kind));
            i = j;
            continue;
         }

         // operátorok
         if (":=<>/+-*^@|=,()[]".indexOf(c) >= 0) {
            int j = i + 1;
            if (j < n) {
               String two = line.substring(i, j + 1);
               if (two.equals(":=") || two.equals("<=") || two.equals(">=") || two.equals("/=")) {
                  j++;
               }
            }
            out.add(new Token(i, j, OPERATOR));
            i = j;
            continue;
         }

         i++;
      }

      return out;
   }

   /**
    * Megadja, hogy egy sor mennyivel változtatja a behúzást, illetve hogy
    * maga a sor mennyivel legyen beljebb.
    *
    * @return kételemű tömb: [a sor saját behúzás-korrekciója, a következő sorra
    *         gyakorolt hatás]
    */
   public static int[] indentEffect(String line) {
      List<Token> toks = tokenize(line);
      int own = 0;
      int next = 0;
      boolean first = true;

      for (int t = 0; t < toks.size(); t++) {
         Token tk = toks.get(t);
         if (tk.kind != KW && tk.kind != CTRL) {
            first = false;
            continue;
         }
         String w = deacc(line.substring(tk.start, tk.end));

         if (w.equals("kulonben")) {
            if (first) {
               own = -1;
            }
            next += 1;
         } else if (INDENT_CLOSE.contains(w)) {
            if (first) {
               own = -1;
            }
            next -= 1;
         } else if (w.equals("akkor") || w.equals("valtozok")
                    || w.equals("program") || w.equals("eljaras") || w.equals("fuggveny")) {
            next += 1;
         } else if (w.equals("ciklus")) {
            next += 1;
         }
         first = false;
      }

      return new int[] { own, next };
   }

   private static Set<String> unmod(String... items) {
      return Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(items)));
   }

   private PlangSyntax() {
   }
}
