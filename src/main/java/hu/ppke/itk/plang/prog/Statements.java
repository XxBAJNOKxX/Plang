package hu.ppke.itk.plang.prog;

import hu.ppke.itk.plang.gui.ExprNode;
import hu.ppke.itk.plang.gui.ProgramLine;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

class Statements {
   private Vector<Statement> stmts;
   private ErrorType errorType;

   static Statements parseStmt(Lexer lex, Environment env, Set<String> termKeyw) {
      LinkedList<Statement> stmts = new LinkedList();

      do {
         if (lex.isKeyword(",")) {
            lex.next();
         }

         if (lex.isIdent()) {
            String ident = lex.getString();
            lex.next();
            if (lex.isKeyword("(")) {
               stmts.add(CallStatement.parseCallStmt(ident, lex, env));
            } else {
               AssignableExpr lhs = AssignableExpr.parseAssignable(ident, lex, env);
               if (!lex.isKeyword(":=")) {
                  stmts.add(new UnparsedStatement(lhs.toString(), "Az értékadó utasítás := jele hiányzik.", lex));
               } else {
                  lex.next();
                  Expression expr = Expression.parseExpression(lex, env);
                  stmts.add(new Assignment(lhs, expr, env));
               }
            }
         } else if (lex.isKeyword("ha")) {
            lex.next();
            stmts.add(Conditional.parseCondStmt(lex, env));
         } else if (lex.isKeyword("ciklus")) {
            lex.next();
            stmts.add(Loop.parseLoopStmt(lex, env));
         } else if (lex.isKeyword("ki")) {
            lex.next();
            stmts.add(PrintStmt.parsePrintStmt(lex, env));
         } else if (lex.isKeyword("be")) {
            lex.next();
            stmts.add(ReadStmt.parseReadStmt(lex, env));
         } else if (lex.isKeyword("megnyit")) {
            lex.next();
            stmts.add(OpenStatement.parseOpenStmt(lex, env));
         } else if (lex.isKeyword("lezar")) {
            lex.next();
            stmts.add(CloseStatement.parseCloseStmt(lex, env));
         } else if (lex.isKeyword("**")) {
            lex.next();
            stmts.add(CommentStatement.parseComment(lex, env));
         } else {
            if (lex.isKeyword(termKeyw)) {
               break;
            }

            stmts.add(new UnparsedStatement("", "Ebben a sorban nincs felismerhető utasítás.", lex));
         }
      } while(!lex.isEof());

      return new Statements(stmts);
   }

   private Statements(List<Statement> stmts) {
      this.errorType = Statements.ErrorType.NONE;
      this.stmts = new Vector(stmts);
      if (stmts.isEmpty()) {
         this.errorType = Statements.ErrorType.EMPTY;
      } else {
         if (((Statement)this.stmts.firstElement()).hasError()) {
            this.errorType = Statements.ErrorType.STMT;
         } else {
            for(int i = 1; i < this.stmts.size(); ++i) {
               if (((Statement)this.stmts.get(i)).hasError()) {
                  this.errorType = Statements.ErrorType.STMT;
                  break;
               }

               ((Statement)this.stmts.get(i - 1)).setNext((Statement)this.stmts.get(i));
            }
         }

         if (this.errorType == Statements.ErrorType.NONE) {
            ((Statement)this.stmts.elementAt(stmts.size() - 1)).setNext((Statement)null);
         }
      }

   }

   boolean hasError() {
      return this.errorType != Statements.ErrorType.NONE;
   }

   public List<ProgramLine> getLines(int indent) {
      if (this.errorType == Statements.ErrorType.EMPTY) {
         Vector<ProgramLine> v = new Vector(1);
         v.add(new ProgramLine(indent, "Erről a helyről hiányoznak az utasítások.") {
            protected String render() {
               return this.indent() + bad("???");
            }

            public void setLine(int l) {
            }

            public ExprNode getExpr(State state) {
               return null;
            }
         });
         return v;
      } else {
         LinkedList<ProgramLine> l = new LinkedList();

         for(Statement s : this.stmts) {
            l.addAll(s.getLines(indent));
         }

         return l;
      }
   }

   public Statement getFirst() {
      return (Statement)this.stmts.firstElement();
   }

   public Statement getLast() {
      return (Statement)this.stmts.lastElement();
   }

   private static class UnparsedStatement extends Statement {
      String parsed;
      String rest;
      String error;

      UnparsedStatement(String line, String error, Lexer lex) {
         this.parsed = line;
         this.error = error;
         this.rest = lex.skip();
      }

      List<ProgramLine> getLines(int indent) {
         return oneLine(new ProgramLine(indent, this.error) {
            protected String render() {
               return this.indent() + bad(UnparsedStatement.this.parsed + " " + UnparsedStatement.this.rest);
            }

            public ExprNode getExpr(State state) {
               return new ExprNode((String)null, UnparsedStatement.this.parsed + " " + UnparsedStatement.this.rest, (ExprNode[])null);
            }

            public void setLine(int l) {
            }
         });
      }

      State execute(State state) {
         return state;
      }

      boolean hasError() {
         return true;
      }
   }

   private static enum ErrorType {
      NONE,
      EMPTY,
      STMT;
   }
}
