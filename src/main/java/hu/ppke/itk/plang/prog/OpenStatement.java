package hu.ppke.itk.plang.prog;

import hu.ppke.itk.plang.gui.ExprNode;
import hu.ppke.itk.plang.gui.ProgramLine;
import java.util.List;

class OpenStatement extends Statement {
   private String var;
   private String name;
   private ErrorType errorType;

   static OpenStatement parseOpenStmt(Lexer lex, Environment env) {
      if (!lex.isIdent()) {
         return new OpenStatement((String)null, (String)null, OpenStatement.ErrorType.FVAR);
      } else {
         String var = lex.getString();
         lex.next();
         if (!lex.isKeyword(":")) {
            return new OpenStatement(var, (String)null, OpenStatement.ErrorType.COLON);
         } else {
            lex.next();
            if (lex.getType() == Lexer.Token.STRING) {
               String name = lex.getString();
               lex.next();
               if (env.getVarType(var) == FileType.INPUT) {
                  env.addStream(StreamKind.INPUT, name);
               } else {
                  if (env.getVarType(var) != FileType.OUTPUT) {
                     return new OpenStatement(var, name, OpenStatement.ErrorType.FTYPE);
                  }

                  env.addStream(StreamKind.OUTPUT, name);
               }

               return new OpenStatement(var, name, OpenStatement.ErrorType.NONE);
            } else {
               return new OpenStatement(var, (String)null, OpenStatement.ErrorType.FNAME);
            }
         }
      }
   }

   private OpenStatement(String var, String name, ErrorType errorType) {
      this.var = var;
      this.name = name;
      this.errorType = errorType;
   }

   private String getError() {
      if (this.errorType == OpenStatement.ErrorType.FVAR) {
         return "Hiányzik a fájlváltozó neve.";
      } else if (this.errorType == OpenStatement.ErrorType.FTYPE) {
         return "A fájlt egy KIFÁJL vagy BEFÁJL típusú változóval kell azonosítani.";
      } else if (this.errorType == OpenStatement.ErrorType.COLON) {
         return "Hiányzik a kettőspont a változó neve után.";
      } else {
         return this.errorType == OpenStatement.ErrorType.FNAME ? "A fájl nevét egy szövegkonstanssal kell megadni." : null;
      }
   }

   List<ProgramLine> getLines(int indent) {
      return oneLine(new ProgramLine(indent, this.getError()) {
         protected String render() {
            if (OpenStatement.this.errorType == OpenStatement.ErrorType.FVAR) {
               return this.indent() + "MEGNYIT " + bad("???");
            } else if (OpenStatement.this.errorType == OpenStatement.ErrorType.COLON) {
               return this.indent() + "MEGNYIT " + OpenStatement.this.var + bad(": ???");
            } else if (OpenStatement.this.errorType == OpenStatement.ErrorType.FNAME) {
               return this.indent() + "MEGNYIT " + OpenStatement.this.var + ": " + bad("???");
            } else {
               return OpenStatement.this.errorType == OpenStatement.ErrorType.FTYPE ? this.indent() + "MEGNYIT " + bad(OpenStatement.this.var) + ": \"" + OpenStatement.this.name + "\"" : this.indent() + "MEGNYIT " + OpenStatement.this.var + ": \"" + OpenStatement.this.name + "\"";
            }
         }

         public void setLine(int l) {
            OpenStatement.this.setLineIndex(l);
         }

         public ExprNode getExpr(State state) {
            return ExprNode.EMPTY;
         }
      });
   }

   State execute(State state) {
      state = state.newState();
      state.setVar(this.var, this.name);
      state.setStatement(this.getNext());
      return state;
   }

   boolean hasError() {
      return this.errorType != OpenStatement.ErrorType.NONE;
   }

   private static enum ErrorType {
      NONE,
      FVAR,
      FTYPE,
      COLON,
      FNAME;
   }
}
