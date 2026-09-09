package hu.ppke.itk.plang.prog;

import hu.ppke.itk.plang.gui.ExprNode;
import hu.ppke.itk.plang.gui.ProgramLine;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

class UnparsedLines {
   private Vector<String> lines;

   private UnparsedLines(List<String> lines) {
      this.lines = new Vector(lines);
   }

   static UnparsedLines skip(Lexer lex, String limit) {
      LinkedList<String> lines = new LinkedList();

      while(!lex.isEof() && (limit == null || !lex.isKeyword(limit))) {
         lines.add(lex.skip());
      }

      return new UnparsedLines(lines);
   }

   List<ProgramLine> getLines(int indent) {
      LinkedList<ProgramLine> l = new LinkedList();

      for(int i = 0; i < this.lines.size(); ++i) {
         l.add(new UnparsedLine(indent, i));
      }

      return l;
   }

   private final class UnparsedLine extends ProgramLine {
      int line;

      UnparsedLine(int ind, int l) {
         super(ind);
         this.line = l;
      }

      protected String render() {
         return this.indent() + (String)UnparsedLines.this.lines.elementAt(this.line);
      }

      public ExprNode getExpr(State state) {
         return new ExprNode((String)null, "---", (ExprNode[])null);
      }

      public void setLine(int l) {
      }
   }
}
