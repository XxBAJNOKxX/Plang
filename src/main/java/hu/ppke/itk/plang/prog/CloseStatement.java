package hu.ppke.itk.plang.prog;

import hu.ppke.itk.plang.gui.ExprNode;
import hu.ppke.itk.plang.gui.ProgramLine;
import java.util.List;

class CloseStatement extends Statement {
   private String var;
   private ErrorType errorType;

   static CloseStatement parseCloseStmt(Lexer lex, Environment env) {
      if (lex.isIdent()) {
         String var = lex.getString();
         lex.next();
         return env.getVarType(var) != FileType.INPUT && env.getVarType(var) != FileType.OUTPUT ? new CloseStatement(var, CloseStatement.ErrorType.TYPE) : new CloseStatement(var, CloseStatement.ErrorType.NONE);
      } else {
         return new CloseStatement((String)null, CloseStatement.ErrorType.VAR);
      }
   }

   private CloseStatement(String var, ErrorType errorType) {
      this.var = var;
      this.errorType = errorType;
   }

   private String getError() {
      if (this.errorType == CloseStatement.ErrorType.VAR) {
         return "Hiányzik a fájlváltozó neve.";
      } else {
         return this.errorType == CloseStatement.ErrorType.TYPE ? "Lezárni csak fájl típusú változót lehet." : null;
      }
   }

   List<ProgramLine> getLines(int indent) {
      return oneLine(new ProgramLine(indent, this.getError()) {
         protected String render() {
            if (CloseStatement.this.errorType == CloseStatement.ErrorType.VAR) {
               return this.indent() + "LEZÁR " + bad("???");
            } else {
               return CloseStatement.this.errorType == CloseStatement.ErrorType.TYPE ? this.indent() + "LEZÁR " + bad(CloseStatement.this.var) : this.indent() + "LEZÁR " + CloseStatement.this.var;
            }
         }

         public void setLine(int l) {
            CloseStatement.this.setLineIndex(l);
         }

         public ExprNode getExpr(State state) {
            return ExprNode.EMPTY;
         }
      });
   }

   State execute(State state) {
      state = state.newState();
      state.setVar(this.var, "<--LEZART-->");
      state.setStatement(this.getNext());
      return state;
   }

   boolean hasError() {
      return this.errorType != CloseStatement.ErrorType.NONE;
   }

   private static enum ErrorType {
      NONE,
      VAR,
      TYPE;
   }
}
