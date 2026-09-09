package hu.ppke.itk.plang.prog;

import hu.ppke.itk.plang.gui.ProgramLine;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

class ParDecl {
   private Kind kind;
   private String name;
   private Type type;
   private Error error;

   private static List<ParDecl> parDecls(Kind kind, List<String> names, Type type, Error error, Environment env) {
      LinkedList<ParDecl> lst = new LinkedList();

      for(String n : names) {
         lst.add(new ParDecl(kind, n, type, error));
         if (env != null) {
            ((ParDecl)lst.getLast()).apply(env);
         }
      }

      return lst;
   }

   static List<ParDecl> parseParDecls(Kind kind, Lexer lex, Environment env) {
      LinkedList<String> names = new LinkedList();
      if (!lex.isIdent()) {
         return Arrays.asList(new ParDecl(kind, (String)null, (Type)null, ParDecl.Error.NAME));
      } else {
         names.add(lex.getString());
         lex.next();

         while(lex.isKeyword(",")) {
            lex.next();
            if (!lex.isIdent()) {
               names.add((String)null);
               return parDecls(kind, names, (Type)null, ParDecl.Error.NAME, (Environment)null);
            }

            names.add(lex.getString());
            lex.next();
         }

         if (!lex.isKeyword(":")) {
            return parDecls(kind, names, (Type)null, ParDecl.Error.COLON, (Environment)null);
         } else {
            lex.next();
            Type type = Type.parseType(lex, env);
            return type == null ? parDecls(kind, names, (Type)null, ParDecl.Error.TYPE, (Environment)null) : parDecls(kind, names, type, ParDecl.Error.NONE, env);
         }
      }
   }

   private ParDecl(Kind kind, String name, Type type, Error error) {
      this.kind = kind;
      this.name = name;
      this.type = type;
      this.error = error;
   }

   Kind getKind() {
      return this.kind;
   }

   String getName() {
      return this.name;
   }

   Type getType() {
      return this.type;
   }

   String getError() {
      return this.error.msg();
   }

   String renderName() {
      if (this.name == null) {
         return ProgramLine.bad("???");
      } else {
         return this.error == ParDecl.Error.EXIST ? ProgramLine.bad(this.name) : this.name;
      }
   }

   String renderType() {
      return this.type == null ? ProgramLine.bad("???") : this.type.render();
   }

   void apply(Environment env) {
      if (this.error == ParDecl.Error.NONE) {
         if (env.hasVar(this.name)) {
            this.error = ParDecl.Error.EXIST;
         } else {
            env.addVar(this.name, this.type);
         }
      }

   }

   static enum Kind {
      INPUT("BE"),
      OUTPUT("KI");

      private String str;

      private Kind(String str) {
         this.str = str;
      }

      public String toString() {
         return this.str;
      }
   }

   private static enum Error {
      NONE((String)null),
      NAME("Hiányzik a paraméter neve."),
      COLON("Hiányzik a kettőspont a nevek után."),
      EXIST("Már van ilyen nevű paraméter."),
      TYPE("Hibás a paraméter típusa.");

      private String msg;

      private Error(String msg) {
         this.msg = msg;
      }

      String msg() {
         return this.msg;
      }
   }
}
