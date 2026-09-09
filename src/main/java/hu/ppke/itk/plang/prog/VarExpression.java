package hu.ppke.itk.plang.prog;

import hu.ppke.itk.plang.gui.ExprNode;
import hu.ppke.itk.plang.gui.ProgramLine;

public class VarExpression extends Expression {
   private String varName;
   private ErrorType errorType;

   static VarExpression parseVarExpr(String var, Lexer lex, Environment env) {
      return !env.hasVar(var) ? new VarExpression(var, VarExpression.ErrorType.EXISTS) : new VarExpression(var, env.getVarType(var));
   }

   private VarExpression(String name, Type type, ErrorType errType) {
      super(type);
      this.varName = name;
      this.errorType = errType;
   }

   private VarExpression(String name, Type type) {
      this(name, type, VarExpression.ErrorType.NONE);
   }

   private VarExpression(String name, ErrorType errType) {
      this(name, (Type)null, errType);
   }

   public String toString() {
      return this.varName;
   }

   String render() {
      return this.errorType == VarExpression.ErrorType.EXISTS ? ProgramLine.bad(this.varName) : this.varName;
   }

   Object getValue(State state) {
      Object val = state.getVar(this.varName);
      return val == null ? new EmptyValue("\"" + this.varName + "\" változó nem kapott kezdőértéket.") : val;
   }

   ExprNode[] getChildren(State state) {
      return new ExprNode[0];
   }

   public String getError() {
      return this.errorType == VarExpression.ErrorType.EXISTS ? "Nincs \"" + this.varName + "\" nevű változó." : null;
   }

   private static enum ErrorType {
      NONE,
      EXISTS;
   }
}
