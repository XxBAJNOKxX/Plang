package hu.ppke.itk.plang.prog;

import hu.ppke.itk.plang.gui.ExprNode;
import hu.ppke.itk.plang.gui.ProgramLine;

class BinOpExpression extends Expression {
   BinaryOperator op;
   Expression left;
   Expression right;
   Type leftType;
   Type rightType;
   ErrorType errorType;

   static BinOpExpression createBinOpExpr(Expression left, Expression right, BinaryOperator op) {
      if (left.getError() != null) {
         return new BinOpExpression(left, right, op, BinOpExpression.ErrorType.LEFT);
      } else if (right.getError() != null) {
         return new BinOpExpression(left, right, op, BinOpExpression.ErrorType.RIGHT);
      } else {
         if (left.getType() == BasicType.BOOLEAN && right.getType() == BasicType.BOOLEAN) {
            if (op == BinaryOperator.AND) {
               return new AndExpression(left, right);
            }

            if (op == BinaryOperator.OR) {
               return new OrExpression(left, right);
            }
         }

         Type retType = left.getType().operatorType(op, right.getType());
         return retType == null ? new BinOpExpression(left, right, op, BinOpExpression.ErrorType.OPER) : new BinOpExpression(left, right, op, retType);
      }
   }

   BinOpExpression(Expression left, Expression right, BinaryOperator op, Type type) {
      super(type);
      this.errorType = BinOpExpression.ErrorType.NONE;
      this.left = left;
      this.right = right;
      this.leftType = left.getType();
      this.rightType = right.getType();
      this.op = op;
   }

   BinOpExpression(Expression left, Expression right, BinaryOperator op, ErrorType errType) {
      super((Type)null);
      this.errorType = BinOpExpression.ErrorType.NONE;
      this.left = left;
      this.right = right;
      this.op = op;
      this.errorType = errType;
   }

   String render() {
      return this.errorType == BinOpExpression.ErrorType.OPER ? ProgramLine.bad(this.op.render(this.left, this.right)) : this.op.render(this.left, this.right);
   }

   public String toString() {
      return this.op.render(this.left.toString(), this.right.toString());
   }

   Object getValue(State state) {
      Object lval = this.left.getValue(state);
      if (lval instanceof BadValue) {
         return lval;
      } else {
         Object rval = this.right.getValue(state);
         return rval instanceof BadValue ? rval : this.leftType.apply(this.op, this.rightType, lval, rval);
      }
   }

   ExprNode[] getChildren(State state) {
      return new ExprNode[]{this.left.getTree(state), this.right.getTree(state)};
   }

   public String getError() {
      if (this.errorType == BinOpExpression.ErrorType.LEFT) {
         return this.left.getError();
      } else if (this.errorType == BinOpExpression.ErrorType.RIGHT) {
         return this.right.getError();
      } else {
         return this.errorType == BinOpExpression.ErrorType.OPER ? this.left.getType() + " és " + this.right.getType() + " típusok között " + "nincs " + this.op + " művelet értelmezve." : null;
      }
   }

   private static enum ErrorType {
      NONE,
      LEFT,
      RIGHT,
      OPER;
   }
}
