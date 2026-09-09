package hu.ppke.itk.plang.prog;

import hu.ppke.itk.plang.gui.ExprNode;

class ParenExpression extends Expression {
   private Expression expr;

   ParenExpression(Expression expr) {
      super(expr.getType());
      this.expr = expr;
   }

   String render() {
      return "(" + this.expr.render() + ")";
   }

   public String toString() {
      return "(" + this.expr + ")";
   }

   Object getValue(State state) {
      return this.expr.getValue(state);
   }

   ExprNode getTree(State state) {
      return this.expr.getTree(state);
   }

   public String getError() {
      return this.expr.getError();
   }

   ExprNode[] getChildren(State state) {
      return null;
   }
}
