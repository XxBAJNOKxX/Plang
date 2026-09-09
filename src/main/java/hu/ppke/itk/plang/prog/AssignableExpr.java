package hu.ppke.itk.plang.prog;

import hu.ppke.itk.plang.gui.ExprNode;
import hu.ppke.itk.plang.gui.ProgramLine;

class AssignableExpr {
   private String varName;
   private Accessor accessor;
   private ErrorType errorType;

   static AssignableExpr parseAssignable(Lexer lex, Environment env) {
      if (!lex.isIdent()) {
         return new AssignableExpr("", (Accessor)null, AssignableExpr.ErrorType.IDENT);
      } else {
         String prefix = lex.getString();
         lex.next();
         return parseAssignable(prefix, lex, env);
      }
   }

   static AssignableExpr parseAssignable(String prefix, Lexer lex, Environment env) {
      Type baseType = env.getVarType(prefix);
      Accessor acc = Accessor.parseAccessors(lex, env, baseType);
      return baseType == null ? new AssignableExpr(prefix, acc, AssignableExpr.ErrorType.VAR) : new AssignableExpr(prefix, acc);
   }

   public AssignableExpr(String varName, Accessor accessor) {
      this(varName, accessor, AssignableExpr.ErrorType.NONE);
   }

   public AssignableExpr(String varName, Accessor accessor, ErrorType errorType) {
      this.varName = varName;
      this.accessor = accessor;
      this.errorType = errorType;
      if (errorType == AssignableExpr.ErrorType.NONE && accessor.getError() != null) {
         this.errorType = AssignableExpr.ErrorType.ACC;
      }

   }

   String getError() {
      if (this.errorType == AssignableExpr.ErrorType.IDENT) {
         return "Hiányzik egy változónév.";
      } else if (this.errorType == AssignableExpr.ErrorType.VAR) {
         return "Nincs \"" + this.varName + "\" nevű változó.";
      } else {
         return this.errorType == AssignableExpr.ErrorType.ACC ? this.accessor.getError() : null;
      }
   }

   Type getType() {
      return this.accessor == null ? null : this.accessor.getType();
   }

   String render() {
      if (this.errorType == AssignableExpr.ErrorType.IDENT) {
         return ProgramLine.bad(" ??? ");
      } else {
         return this.errorType == AssignableExpr.ErrorType.VAR ? ProgramLine.bad(this.varName) + this.accessor.render() : this.varName + this.accessor.render();
      }
   }

   public String toString() {
      return this.accessor == null ? this.varName : this.varName + this.accessor;
   }

   ExprNode getTree(State state) {
      return this.accessor.getTree(state, this.varName, new ExprNode(this.varName, new ExprNode[0], (String)null));
   }

   void assign(State state, Object value) {
      Object var = state.getVar(this.varName);
      if (var == null) {
         var = new EmptyValue("\"" + this.varName + "\" változó nem kapott kezdőértéket.");
      }

      Object newVal = this.accessor.access(state, var, value);
      if (newVal instanceof BadValue) {
         state.setError(newVal.toString());
      } else {
         state.setVar(this.varName, newVal);
      }

   }

   private static enum ErrorType {
      NONE,
      VAR,
      IDENT,
      ACC;
   }
}
