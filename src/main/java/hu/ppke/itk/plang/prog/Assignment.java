package hu.ppke.itk.plang.prog;

import hu.ppke.itk.plang.gui.ExprNode;
import hu.ppke.itk.plang.gui.ProgramLine;
import java.util.List;

class Assignment extends Statement {
   private AssignableExpr leftSide;
   private Expression expr;
   private String error;
   private ErrType errType;

   Assignment(AssignableExpr lhs, Expression expr, Environment env) {
      this.errType = Assignment.ErrType.NONE;
      this.leftSide = lhs;
      this.expr = expr;
      if (this.leftSide.getError() != null) {
         this.error = this.leftSide.getError();
         this.errType = Assignment.ErrType.LHS;
      } else if (expr.getError() != null) {
         this.error = expr.getError();
         this.errType = Assignment.ErrType.EXPR;
      } else if (!this.leftSide.getType().canCopy(expr.getType())) {
         this.error = this.leftSide.getType().render() + " és " + expr.getType() + " típusok között nem végezhető el az értékadás.";
         this.errType = Assignment.ErrType.TYPE;
      }

   }

   List<ProgramLine> getLines(int indent) {
      return oneLine(new AssignmentLine(indent));
   }

   State execute(State state) {
      Object rhs = this.expr.getValue(state);
      state = state.newState();
      this.leftSide.assign(state, rhs);
      if (state.getError() == null && rhs instanceof BadValue) {
         state.setError(rhs.toString());
      } else {
         state.setStatement(this.getNext());
      }

      return state;
   }

   boolean hasError() {
      return this.errType != Assignment.ErrType.NONE;
   }

   static enum ErrType {
      NONE,
      LHS,
      EXPR,
      TYPE;
   }

   private class AssignmentLine extends ProgramLine {
      AssignmentLine(int indent) {
         super(indent, Assignment.this.error);
      }

      protected String render() {
         return Assignment.this.errType == Assignment.ErrType.TYPE ? this.indent() + Assignment.this.leftSide.render() + bad(" := ") + Assignment.this.expr.render() : this.indent() + Assignment.this.leftSide.render() + " := " + Assignment.this.expr.render();
      }

      public ExprNode getExpr(State state) {
         return new ExprNode((String)null, Assignment.this.leftSide.render() + " := " + Assignment.this.expr.render(), new ExprNode[]{Assignment.this.leftSide.getTree(state), Assignment.this.expr.getTree(state)});
      }

      public void setLine(int l) {
         Assignment.this.setLineIndex(l);
      }
   }
}
