package hu.ppke.itk.plang.prog;

import hu.ppke.itk.plang.gui.ExprNode;
import hu.ppke.itk.plang.gui.ProgramLine;

class EofExpression extends Expression {
   private String var;
   private ErrorType errorType;

   static EofExpression createEofExpr(Lexer lex, Environment env) {
      if (!lex.isIdent()) {
         return new EofExpression((String)null, EofExpression.ErrorType.VAR);
      } else {
         String var = lex.getString();
         lex.next();
         return env.getVarType(var) != FileType.INPUT ? new EofExpression(var, EofExpression.ErrorType.TYPE) : new EofExpression(var, EofExpression.ErrorType.NONE);
      }
   }

   private EofExpression(String var, ErrorType errorType) {
      super(BasicType.BOOLEAN);
      this.var = var;
      this.errorType = errorType;
   }

   public String getError() {
      if (this.errorType == EofExpression.ErrorType.VAR) {
         return "Hiányzik a fájlváltozó neve.";
      } else {
         return this.errorType == EofExpression.ErrorType.TYPE ? "Csak a BEFÁJL típuson értelmezett a VÉGE művelet." : null;
      }
   }

   String render() {
      if (this.errorType == EofExpression.ErrorType.VAR) {
         return "VÉGE " + ProgramLine.bad("???");
      } else {
         return this.errorType == EofExpression.ErrorType.TYPE ? "VÉGE " + ProgramLine.bad(this.var) : "VÉGE " + this.var;
      }
   }

   Object getValue(State state) {
      String name = (String)state.getVar(this.var);
      return name.equals("<--LEZART-->") ? new BadValue("Nincs megnyitva a fájl.") : new Boolean(state.getStreamState(name).isEof());
   }

   ExprNode[] getChildren(State state) {
      return new ExprNode[0];
   }

   private static enum ErrorType {
      NONE,
      VAR,
      TYPE;
   }
}
