package hu.ppke.itk.plang.prog;

class AndExpression extends BinOpExpression {
   AndExpression(Expression left, Expression right) {
      super(left, right, BinaryOperator.AND, (Type)BasicType.BOOLEAN);
   }

   Object getValue(State state) {
      Object lval = this.left.getValue(state);
      if (lval instanceof BadValue) {
         return lval;
      } else {
         return !(Boolean)lval ? lval : this.right.getValue(state);
      }
   }
}
