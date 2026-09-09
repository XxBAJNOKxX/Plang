package hu.ppke.itk.plang.prog;

import hu.ppke.itk.plang.gui.ExprNode;
import hu.ppke.itk.plang.gui.ProgramLine;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

class Declarations {
   Vector<SubProgram> subProgs;
   Vector<VarDecl> varDecls;

   private Declarations(List<SubProgram> subProgs, List<VarDecl> varDecls) {
      this.subProgs = new Vector(subProgs);
      this.varDecls = new Vector(varDecls);
   }

   static Declarations parseDecl(Lexer lex, Environment env) {
      LinkedList<SubProgram> subProgs = new LinkedList();

      while(lex.isKeyword("eljaras") || lex.isKeyword("fuggveny")) {
         subProgs.add(SubProgram.parseSubProgram(lex, env));
      }

      LinkedList<VarDecl> varDecls = new LinkedList();
      if (lex.isKeyword("valtozok")) {
         lex.next();
         if (lex.isKeyword(":")) {
            lex.next();
         }

         varDecls.add(VarDecl.parseVarDecl(lex, env));

         while(lex.isKeyword(",")) {
            ((VarDecl)varDecls.getLast()).setLast(false);
            lex.next();
            varDecls.add(VarDecl.parseVarDecl(lex, env));
         }
      }

      return new Declarations(subProgs, varDecls);
   }

   List<ProgramLine> getLines(int indent) {
      LinkedList<ProgramLine> l = new LinkedList();

      for(SubProgram sp : this.subProgs) {
         l.addAll(sp.getLines(indent));
         l.add(ProgramLine.emptyLine);
      }

      if (this.varDecls.isEmpty()) {
         return l;
      } else {
         l.add(new VarDeclHead(indent));

         for(int i = 0; i < this.varDecls.size(); ++i) {
            l.add(((VarDecl)this.varDecls.elementAt(i)).getLine(indent + 1));
         }

         l.add(ProgramLine.emptyLine);
         return l;
      }
   }

   private final class VarDeclHead extends ProgramLine {
      VarDeclHead(int ind) {
         super(ind);
      }

      protected String render() {
         return this.indent() + "VÁLTOZÓK:";
      }

      public ExprNode getExpr(State state) {
         return new ExprNode((String)null, "---", (ExprNode[])null);
      }

      public void setLine(int l) {
      }
   }
}
