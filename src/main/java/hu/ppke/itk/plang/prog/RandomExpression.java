package hu.ppke.itk.plang.prog;

import hu.ppke.itk.plang.gui.ExprNode;
import hu.ppke.itk.plang.gui.ProgramLine;

class RandomExpression extends Expression {
   private ErrorType errorType;
   private Expression bound;
   private int rndID;
   private static int maxRndID = 1;

   static RandomExpression createRandomExpr(Expression bound) {
      if (bound.getError() != null) {
         return new RandomExpression(bound, RandomExpression.ErrorType.ARG);
      } else {
         return bound.getType() != BasicType.INTEGER ? new RandomExpression(bound, RandomExpression.ErrorType.TYPE) : new RandomExpression(bound, RandomExpression.ErrorType.NONE);
      }
   }

   private RandomExpression(Expression bound, ErrorType errorType) {
      super(BasicType.INTEGER);
      this.bound = bound;
      this.errorType = errorType;
      this.rndID = maxRndID++;
   }

   public String getError() {
      if (this.errorType == RandomExpression.ErrorType.ARG) {
         return this.bound.getError();
      } else {
         return this.errorType == RandomExpression.ErrorType.TYPE ? "A paraméter nem EGÉSZ típusú." : null;
      }
   }

   String render() {
      return this.errorType != RandomExpression.ErrorType.NONE ? ProgramLine.bad("RND " + this.bound.render()) : "RND " + this.bound.render();
   }

   public String toString() {
      return "RND " + this.bound;
   }

   Object getValue(State state) {
      Object bval = this.bound.getValue(state);
      return bval instanceof BadValue ? bval : (int)((double)(Integer)bval * state.getRandom(this.rndID));
   }

   ExprNode[] getChildren(State state) {
      return new ExprNode[]{this.bound.getTree(state)};
   }

   private static enum ErrorType {
      NONE,
      ARG,
      TYPE;
   }
}
