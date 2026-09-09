package hu.ppke.itk.plang.prog;

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.HashSet;
import java.util.Set;

public final class Lexer {
   private Token type;
   private String sval;
   private double nval;
   private static final Set<String> keywords;
   private static final Set<String> keywordPrefs;
   private StreamTokenizer tok;
   private static Set<String> emptySet;

   static {
      String[] kwd = new String[]{"program", "program_vege", "eljaras", "eljaras_vege", "fuggveny", "fuggveny_vege", "valtozok", "ha", "akkor", "ha_vege", "kulonben", "ciklus", "amig", "ciklus_vege", "egesz", "valos", "szoveg", "karakter", "logikai", "igaz", "hamis", "sv", "ki", "be", ",", ":", "+", "-", "*", "/", "^", "@", "div", "mod", "(", ")", "[", "]", "|", "**", ":=", "=", "/=", "<", ">", "<=", ">=", "sin", "cos", "tan", "log", "exp", "rnd", "arcsin", "arccos", "arctan", "kis", "nagy", "kerek", "betu", "szam", "nem", "es", "vagy", "kifajl", "befajl", "megnyit", "lezar", "vege"};
      HashSet<String> s = new HashSet();

      for(int i = 0; i < kwd.length; ++i) {
         s.add(kwd[i]);
      }

      keywords = s;
      kwd = new String[]{":", "/", "!", "<", ">", "*"};
      s = new HashSet();

      for(int i = 0; i < kwd.length; ++i) {
         s.add(kwd[i]);
      }

      keywordPrefs = s;
      emptySet = new HashSet(0);
   }

   double getNumber() {
      return this.nval;
   }

   String getString() {
      return this.type == Lexer.Token.IDENT ? this.sval.toLowerCase() : this.sval;
   }

   public Lexer(Reader r) {
      this.tok = new StreamTokenizer(r);
      this.tok.lowerCaseMode(false);
      this.tok.eolIsSignificant(true);
      this.tok.ordinaryChar(47);
      this.tok.ordinaryChar(45);
      this.tok.wordChars(95, 95);
      this.type = Lexer.Token.INIT;
      this.next();
   }

   void next() {
      if (this.type != Lexer.Token.EOF) {
         try {
            this.tok.nextToken();
            switch (this.tok.ttype) {
               case -3:
                  String word = this.deacc(this.tok.sval.toLowerCase());
                  if (keywords.contains(word)) {
                     this.type = Lexer.Token.KEYWORD;
                     this.sval = word;
                  } else {
                     this.type = Lexer.Token.IDENT;
                     this.sval = this.tok.sval;
                  }
                  break;
               case -2:
                  this.type = Lexer.Token.NUMBER;
                  this.nval = this.tok.nval;
                  this.sval = (double)((int)this.tok.nval) == this.tok.nval ? Integer.toString((int)this.tok.nval) : Double.toString(this.tok.nval);
                  break;
               case -1:
                  this.type = Lexer.Token.EOF;
                  break;
               case 10:
                  this.next();
                  return;
               default:
                  if (this.tok.sval != null) {
                     if (this.tok.ttype == 34) {
                        this.type = Lexer.Token.STRING;
                     } else {
                        this.type = Lexer.Token.CHAR;
                     }

                     this.sval = this.tok.sval;

                     for(int i = 0; i < this.sval.length(); ++i) {
                        if (this.sval.charAt(i) < ' ') {
                           this.sval = this.sval.replace(this.sval.charAt(i), ' ');
                        }
                     }
                  } else {
                     String ch = new String(new char[]{(char)this.tok.ttype});
                     if (keywordPrefs.contains(ch)) {
                        this.tok.nextToken();
                        if (this.tok.ttype >= 0 && keywords.contains(ch + (char)this.tok.ttype)) {
                           ch = ch + (char)this.tok.ttype;
                        } else {
                           this.tok.pushBack();
                        }
                     }

                     if (keywords.contains(ch)) {
                        this.type = Lexer.Token.KEYWORD;
                     } else {
                        this.type = Lexer.Token.IDENT;
                     }

                     this.sval = ch;
                  }
            }
         } catch (IOException e) {
            this.type = Lexer.Token.EOF;
            System.err.println(e.getMessage());
         }

      }
   }

   String skip() {
      return this.skip(emptySet);
   }

   String skip(Set<String> termKeyw) {
      if (termKeyw.contains(this.deacc(this.sval))) {
         return "";
      } else {
         String s = this.sval;

         try {
            this.tok.nextToken();

            for(; this.tok.ttype != -1 && this.tok.ttype != 10; this.tok.nextToken()) {
               if (this.tok.ttype == -2) {
                  s = s + " " + ((double)((int)this.tok.nval) == this.tok.nval ? Integer.toString((int)this.tok.nval) : Double.toString(this.tok.nval));
               } else if (this.tok.ttype == -3) {
                  if (termKeyw.contains(this.deacc(this.tok.sval.toLowerCase()))) {
                     this.tok.pushBack();
                     break;
                  }

                  s = s + " " + this.tok.sval;
               } else if (this.tok.sval != null) {
                  s = s + (char)this.tok.ttype + this.tok.sval + (char)this.tok.ttype;
               } else {
                  s = s + (char)this.tok.ttype;
               }
            }

            System.err.println("LEX: skip " + s);
            this.next();
         } catch (IOException e) {
            this.type = Lexer.Token.EOF;
            System.err.println(e.getMessage());
         }

         return s;
      }
   }

   private String deacc(String word) {
      char[] accent = new char[]{'á', 'é', 'í', 'ó', 'ö', 'ő', 'ú', 'ü', 'ű'};
      char[] noacc = new char[]{'a', 'e', 'i', 'o', 'o', 'o', 'u', 'u', 'u'};

      for(int i = 0; i < accent.length; ++i) {
         word = word.replace(accent[i], noacc[i]);
      }

      return word;
   }

   public boolean isKeyword(String kw) {
      return this.type == Lexer.Token.KEYWORD && this.sval.equals(kw);
   }

   public boolean isKeyword(Set<String> termKeyw) {
      return this.type == Lexer.Token.KEYWORD && termKeyw.contains(this.sval);
   }

   public boolean isIdent() {
      return this.type == Lexer.Token.IDENT;
   }

   public boolean isEof() {
      return this.type == Lexer.Token.EOF;
   }

   public boolean isData() {
      return this.type == Lexer.Token.CHAR || this.type == Lexer.Token.STRING || this.type == Lexer.Token.NUMBER;
   }

   public Token getType() {
      return this.type;
   }

   public static enum Token {
      INIT,
      EOF,
      KEYWORD,
      STRING,
      CHAR,
      NUMBER,
      IDENT;
   }
}
