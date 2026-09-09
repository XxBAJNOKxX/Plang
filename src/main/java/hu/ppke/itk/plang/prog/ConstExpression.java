package hu.ppke.itk.plang.prog;

import hu.ppke.itk.plang.gui.ExprNode;
import hu.ppke.itk.plang.gui.ProgramLine;

class ConstExpression extends Expression {
   Object value;
   private ErrorType errorType;

   ConstExpression(String val, BasicType type) {
      super(type);
      this.errorType = ConstExpression.ErrorType.NONE;
      if (type == BasicType.CHARACTER && val.length() != 1) {
         this.value = type.constValue("?");
         this.errorType = ConstExpression.ErrorType.CHAR;
      } else {
         this.value = type.constValue(val);
      }

   }

   ConstExpression(double val, BasicType type) {
      super(type);
      this.errorType = ConstExpression.ErrorType.NONE;
      this.value = type.constValue(val);
   }

   public String toString() {
      return this.getType().toString(this.value);
   }

   String render() {
      return this.errorType != ConstExpression.ErrorType.NONE ? ProgramLine.bad(this.toString()) : this.getType().render(this.value);
   }

   Object getValue(State state) {
      return this.value;
   }

   ExprNode[] getChildren(State state) {
      return new ExprNode[0];
   }

   public String getError() {
      return this.errorType == ConstExpression.ErrorType.CHAR ? "A karakter konstansnak pontosan egy karakterből kell állnia." : null;
   }

   private static enum ErrorType {
      CHAR,
      NONE;
   }
}
