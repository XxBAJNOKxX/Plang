package hu.ppke.itk.plang.prog;

import hu.ppke.itk.plang.gui.ExprNode;
import hu.ppke.itk.plang.gui.ProgramLine;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

class Conditional extends Statement {
   private static Set<String> termIf = new HashSet(Arrays.asList("program_vege", "ha_vege", "kulonben"));
   private static Set<String> termElse = new HashSet(Arrays.asList("program_vege", "ha_vege"));
   private Expression cond;
   private Statements thenBody;
   private Statements elseBody;
   private ErrorType errType;

   static Conditional parseCondStmt(Lexer lex, Environment env) {
      Expression cond = Expression.parseExpression(lex, env);
      if (!lex.isKeyword("akkor")) {
         return new Conditional(cond, (Statements)null, (Statements)null, Conditional.ErrorType.HEAD);
      } else {
         lex.next();
         Statements thenBody = null;
         Statements elseBody = null;
         thenBody = Statements.parseStmt(lex, env, termIf);
         if (lex.isKeyword("kulonben")) {
            lex.next();
            elseBody = Statements.parseStmt(lex, env, termElse);
         }

         if (!lex.isKeyword("ha_vege")) {
            return new Conditional(cond, thenBody, elseBody, Conditional.ErrorType.TAIL);
         } else {
            lex.next();
            return new Conditional(cond, thenBody, elseBody);
         }
      }
   }

   private Conditional(Expression cond, Statements thenBody, Statements elseBody, ErrorType errorType) {
      this.cond = cond;
      this.thenBody = thenBody;
      this.elseBody = elseBody;
      this.errType = errorType;
      if (this.errType == Conditional.ErrorType.NONE) {
         if (cond != null && cond.getError() == null) {
            if (cond.getType() != BasicType.BOOLEAN) {
               this.errType = Conditional.ErrorType.BOOL;
            } else if (thenBody.hasError() || this.elseBody != null && this.elseBody.hasError()) {
               this.errType = Conditional.ErrorType.BODY;
            }
         } else {
            this.errType = Conditional.ErrorType.COND;
         }
      }

   }

   private Conditional(Expression cond, Statements thenBody, Statements elseBody) {
      this(cond, thenBody, elseBody, Conditional.ErrorType.NONE);
   }

   List<ProgramLine> getLines(int indent) {
      LinkedList<ProgramLine> lines = new LinkedList();
      lines.add(new CondHead(indent));
      if (this.thenBody != null) {
         lines.addAll(this.thenBody.getLines(indent + 1));
      }

      if (this.elseBody != null) {
         lines.add(new CondTail(indent, false));
         lines.addAll(this.elseBody.getLines(indent + 1));
      }

      lines.add(new CondTail(indent, true));
      return lines;
   }

   State execute(State state) {
      Object cval = this.cond.getValue(state);
      state = state.newState();
      if (cval instanceof BadValue) {
         state.setError(cval.toString());
      } else if ((Boolean)cval) {
         state.setStatement(this.thenBody.getFirst());
      } else if (this.elseBody != null) {
         state.setStatement(this.elseBody.getFirst());
      } else {
         state.setStatement(this.getNext());
      }

      return state;
   }

   void setNext(Statement next) {
      super.setNext(next);
      this.thenBody.getLast().setNext(next);
      if (this.elseBody != null) {
         this.elseBody.getLast().setNext(next);
      }

   }

   private String getHeadError() {
      if (this.errType == Conditional.ErrorType.HEAD) {
         return "Hiányzik az AKKOR kulcsszó.";
      } else if (this.errType == Conditional.ErrorType.COND) {
         return this.cond.getError();
      } else {
         return this.errType == Conditional.ErrorType.BOOL ? "A feltétel nem logikai típusú kifejezés." : null;
      }
   }

   private String getTailError() {
      return this.errType == Conditional.ErrorType.TAIL ? "Hiányzik a HA_VÉGE kulcsszó." : null;
   }

   boolean hasError() {
      return this.errType != Conditional.ErrorType.NONE;
   }

   private static enum ErrorType {
      NONE,
      HEAD,
      TAIL,
      BOOL,
      BODY,
      COND;
   }

   private class CondHead extends ProgramLine {
      CondHead(int indent) {
         super(indent, Conditional.this.getHeadError());
      }

      protected String render() {
         if (Conditional.this.errType == Conditional.ErrorType.HEAD) {
            return this.indent() + "HA " + Conditional.this.cond.render() + " " + bad("AKKOR");
         } else {
            return Conditional.this.errType == Conditional.ErrorType.BOOL ? this.indent() + "HA " + bad(Conditional.this.cond.render()) + " AKKOR" : this.indent() + "HA " + Conditional.this.cond.render() + " AKKOR";
         }
      }

      public ExprNode getExpr(State state) {
         return Conditional.this.cond.getTree(state);
      }

      public void setLine(int l) {
         Conditional.this.setLineIndex(l);
      }
   }

   private class CondTail extends ProgramLine {
      private boolean last;

      CondTail(int indent, boolean last) {
         super(indent, last ? Conditional.this.getTailError() : null);
         this.last = last;
      }

      protected String render() {
         return this.last && Conditional.this.errType == Conditional.ErrorType.TAIL ? this.indent() + bad("HA_VÉGE") : this.indent() + (this.last ? "HA_VÉGE" : "KÜLÖNBEN");
      }

      public ExprNode getExpr(State state) {
         return ExprNode.EMPTY;
      }

      public void setLine(int l) {
      }
   }
}
