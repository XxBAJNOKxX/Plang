package hu.ppke.itk.plang.prog;

import hu.ppke.itk.plang.gui.ExprNode;
import hu.ppke.itk.plang.gui.ProgramLine;
import java.util.List;

class CommentStatement extends Statement {
   private String text;

   public static CommentStatement parseComment(Lexer lex, Environment env) {
      return new CommentStatement(lex.skip());
   }

   private CommentStatement(String text) {
      this.text = text;
   }

   List<ProgramLine> getLines(int indent) {
      return oneLine(new CommentLine(indent));
   }

   private String getError() {
      return null;
   }

   State execute(State state) {
      state.setStatement(this.getNext());
      return state;
   }

   boolean hasError() {
      return false;
   }

   private class CommentLine extends ProgramLine {
      CommentLine(int ind) {
         super(ind, CommentStatement.this.getError());
      }

      protected String render() {
         return this.indent() + "** " + CommentStatement.this.text;
      }

      public ExprNode getExpr(State state) {
         return ExprNode.EMPTY;
      }

      public void setLine(int l) {
         CommentStatement.this.setLineIndex(l);
      }
   }
}
