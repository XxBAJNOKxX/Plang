package hu.ppke.itk.plang.prog;

import hu.ppke.itk.plang.gui.ExprNode;
import hu.ppke.itk.plang.gui.ProgramLine;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

class Loop extends Statement {
   private static Set<String> termPre = new HashSet(Arrays.asList("program_vege", "ciklus_vege"));
   private static Set<String> termPost = new HashSet(Arrays.asList("program_vege", "amig"));
   private CheckType checkType;
   private Expression cond;
   private Statements body;
   private ErrorType errorType;
   Statement condStmt;

   static Loop parseLoopStmt(Lexer lex, Environment env) {
      if (lex.isKeyword("amig")) {
         lex.next();
         Expression cond = Expression.parseExpression(lex, env);
         Statements body = Statements.parseStmt(lex, env, termPre);
         if (lex.isKeyword("ciklus_vege")) {
            lex.next();
            return new Loop(Loop.CheckType.BEFORE, cond, body);
         } else {
            return new Loop(Loop.CheckType.BEFORE, cond, body, Loop.ErrorType.TAIL);
         }
      } else {
         Statements body = Statements.parseStmt(lex, env, termPost);
         if (lex.isKeyword("amig")) {
            lex.next();
            Expression cond = Expression.parseExpression(lex, env);
            return new Loop(Loop.CheckType.AFTER, cond, body);
         } else {
            return new Loop(Loop.CheckType.AFTER, (Expression)null, body, Loop.ErrorType.TAIL);
         }
      }
   }

   private Loop(CheckType checkType, Expression cond, Statements body, ErrorType errorType) {
      this.condStmt = new Statement() {
         State execute(State state) {
            Object cval = Loop.this.cond.getValue(state);
            state = state.newState();
            if (cval instanceof BadValue) {
               state.setError(cval.toString());
            } else if ((Boolean)cval) {
               state.setStatement(Loop.this.body.getFirst());
            } else {
               state.setStatement(this.getNext());
            }

            return state;
         }

         List<ProgramLine> getLines(int indent) {
            return null;
         }

         boolean hasError() {
            return Loop.this.hasError();
         }
      };
      this.checkType = checkType;
      this.errorType = errorType;
      this.cond = cond;
      this.body = body;
      if (errorType == Loop.ErrorType.NONE) {
         if (this.cond != null && this.cond.getError() != null) {
            this.errorType = Loop.ErrorType.COND;
         } else if (cond.getType() != BasicType.BOOLEAN) {
            errorType = Loop.ErrorType.BOOL;
         } else if (this.body.hasError()) {
            this.errorType = Loop.ErrorType.BODY;
         }
      }

   }

   public Loop(CheckType checkType, Expression cond, Statements body) {
      this(checkType, cond, body, Loop.ErrorType.NONE);
   }

   List<ProgramLine> getLines(int indent) {
      LinkedList<ProgramLine> lines = new LinkedList();
      lines.add(new LoopHead(indent));
      lines.addAll(this.body.getLines(indent + 1));
      lines.add(new LoopTail(indent));
      return lines;
   }

   void setNext(Statement next) {
      super.setNext(next);
      this.condStmt.setNext(next);
      this.body.getLast().setNext(this.condStmt);
   }

   State execute(State state) {
      return this.checkType == Loop.CheckType.BEFORE ? this.condStmt.execute(state) : this.body.getFirst().execute(state);
   }

   private String getHeadError() {
      if (this.errorType == Loop.ErrorType.BOOL) {
         return "A ciklus feltételének logikai típusúnak kell lennie.";
      } else {
         return this.checkType == Loop.CheckType.BEFORE && this.errorType == Loop.ErrorType.COND ? this.cond.getError() : null;
      }
   }

   private String getTailError() {
      if (this.errorType == Loop.ErrorType.BOOL) {
         return "A ciklus feltételének logikai típusúnak kell lennie.";
      } else if (this.checkType == Loop.CheckType.AFTER && this.errorType == Loop.ErrorType.COND) {
         return this.cond.getError();
      } else if (this.errorType == Loop.ErrorType.TAIL) {
         return this.checkType == Loop.CheckType.BEFORE ? "Hiányzik a CIKLUS_VÉGE kulcsszó." : "Hiányzik az AMÍG kulcsszó.";
      } else {
         return null;
      }
   }

   public int getLineIndex() {
      return this.checkType == Loop.CheckType.AFTER ? this.body.getFirst().getLineIndex() : super.getLineIndex();
   }

   boolean hasError() {
      return this.errorType != Loop.ErrorType.NONE;
   }

   private static enum CheckType {
      BEFORE,
      AFTER;
   }

   private static enum ErrorType {
      NONE,
      TAIL,
      COND,
      BOOL,
      BODY;
   }

   private final class LoopHead extends ProgramLine {
      LoopHead(int indent) {
         super(indent, Loop.this.getHeadError());
      }

      protected String render() {
         if (Loop.this.checkType == Loop.CheckType.BEFORE) {
            return Loop.this.errorType == Loop.ErrorType.BOOL ? this.indent() + "CIKLUS AMÍG " + bad(Loop.this.cond.render()) : this.indent() + "CIKLUS AMÍG " + Loop.this.cond.render();
         } else {
            return this.indent() + "CIKLUS";
         }
      }

      public void setLine(int l) {
         if (Loop.this.checkType == Loop.CheckType.BEFORE) {
            Loop.this.setLineIndex(l);
            Loop.this.condStmt.setLineIndex(l);
         }

      }

      public ExprNode getExpr(State state) {
         return Loop.this.checkType == Loop.CheckType.BEFORE ? Loop.this.cond.getTree(state) : ExprNode.EMPTY;
      }
   }

   private final class LoopTail extends ProgramLine {
      LoopTail(int indent) {
         super(indent, Loop.this.getTailError());
      }

      protected String render() {
         if (Loop.this.checkType == Loop.CheckType.BEFORE) {
            return Loop.this.errorType == Loop.ErrorType.TAIL ? this.indent() + bad("CIKLUS_VÉGE") : this.indent() + "CIKLUS_VÉGE";
         } else if (Loop.this.errorType == Loop.ErrorType.TAIL) {
            return this.indent() + bad("AMÍG ???");
         } else {
            return Loop.this.errorType == Loop.ErrorType.BOOL ? this.indent() + "AMÍG " + bad(Loop.this.cond.render()) : this.indent() + "AMÍG " + Loop.this.cond.render();
         }
      }

      public void setLine(int l) {
         if (Loop.this.checkType == Loop.CheckType.AFTER) {
            Loop.this.condStmt.setLineIndex(l);
         }

      }

      public ExprNode getExpr(State state) {
         return Loop.this.checkType == Loop.CheckType.AFTER && Loop.this.cond != null ? Loop.this.cond.getTree(state) : ExprNode.EMPTY;
      }
   }
}
