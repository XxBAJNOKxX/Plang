package hu.ppke.itk.plang.prog;

import hu.ppke.itk.plang.gui.ExprNode;
import hu.ppke.itk.plang.gui.ProgramLine;

class UnOpExpression extends Expression {
   private Expression expr;
   private UnaryOperator op;
   private ErrorType errorType;

   static UnOpExpression createUnOpExpr(Expression e, UnaryOperator op) {
      if (e.getError() != null) {
         return new UnOpExpression(e, op, UnOpExpression.ErrorType.ARG);
      } else {
         Type t = e.getType().operatorType(op);
         return t == null ? new UnOpExpression(e, op, UnOpExpression.ErrorType.OPER) : new UnOpExpression(e, op, t);
      }
   }

   private UnOpExpression(Expression expr, UnaryOperator op, Type type) {
      super(type);
      this.expr = expr;
      this.op = op;
   }

   private UnOpExpression(Expression expr, UnaryOperator op, ErrorType errorType) {
      super((Type)null);
      this.expr = expr;
      this.op = op;
      this.errorType = errorType;
   }

   String render() {
      return this.errorType == UnOpExpression.ErrorType.OPER ? ProgramLine.bad(this.op.render(this.expr.render())) : this.op.render(this.expr);
   }

   public String toString() {
      return this.op.render(this.expr.toString());
   }

   Object getValue(State state) {
      Object val = this.expr.getValue(state);
      return val instanceof BadValue ? val : this.expr.getType().apply(this.op, val);
   }

   ExprNode[] getChildren(State state) {
      return new ExprNode[]{this.expr.getTree(state)};
   }

   public String getError() {
      if (this.errorType == UnOpExpression.ErrorType.ARG) {
         return this.expr.getError();
      } else {
         return this.errorType == UnOpExpression.ErrorType.OPER ? this.expr.getType() + " típuson nincs " + this.op + " művelet értelmezve." : null;
      }
   }

   private static enum ErrorType {
      NONE,
      ARG,
      OPER;
   }
}
