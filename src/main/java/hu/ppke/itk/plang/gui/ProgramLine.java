package hu.ppke.itk.plang.gui;

import hu.ppke.itk.plang.prog.State;

public abstract class ProgramLine {
   public static final ProgramLine emptyLine = new ProgramLine(0) {
      protected String render() {
         return " ";
      }

      public ExprNode getExpr(State state) {
         return null;
      }

      public void setLine(int l) {
      }
   };
   private int indent;
   private String error;

   protected ProgramLine(int indent) {
      this.indent = indent;
   }

   protected ProgramLine(String error) {
      this.indent = 0;
      this.error = error;
   }

   protected ProgramLine(int indent, String error) {
      this.indent = indent;
      this.error = error;
   }

   public final int getIndent() {
      return this.indent;
   }

   protected final String indent() {
      String sp = "&nbsp;&nbsp;";
      String r = "";

      for(int i = 0; i < this.indent; ++i) {
         r = r + sp;
      }

      return r;
   }

   public static final String bad(String s) {
      return "<font color=\"red\">" + s + "</font>";
   }

   protected abstract String render();

   public abstract void setLine(int var1);

   public abstract ExprNode getExpr(State var1);

   public final String toString() {
      return this.render();
   }

   boolean hasError() {
      return this.error != null;
   }

   String getError() {
      return this.error;
   }
}
