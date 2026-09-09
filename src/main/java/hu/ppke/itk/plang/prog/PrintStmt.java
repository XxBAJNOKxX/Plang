package hu.ppke.itk.plang.prog;

import hu.ppke.itk.plang.gui.ExprNode;
import hu.ppke.itk.plang.gui.ProgramLine;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

class PrintStmt extends Statement {
   private String file;
   private Vector<Expression> exprs;
   private ErrorType errorType;

   public static PrintStmt parsePrintStmt(Lexer lex, Environment env) {
      String file = null;
      if (lex.isIdent()) {
         file = lex.getString();
         lex.next();
      }

      if (!lex.isKeyword(":")) {
         return new PrintStmt(file, (List)null, PrintStmt.ErrorType.COLON);
      } else {
         lex.next();
         ErrorType err = PrintStmt.ErrorType.NONE;
         LinkedList<Expression> exprs = new LinkedList();
         exprs.add(Expression.parseExpression(lex, env));
         if (((Expression)exprs.getLast()).getError() != null) {
            err = PrintStmt.ErrorType.EXPR;
         }

         while(lex.isKeyword(",")) {
            lex.next();
            exprs.add(Expression.parseExpression(lex, env));
            if (((Expression)exprs.getLast()).getError() != null) {
               err = PrintStmt.ErrorType.EXPR;
            }
         }

         if (file != null && env.getVarType(file) != FileType.OUTPUT) {
            return new PrintStmt(file, exprs, PrintStmt.ErrorType.TYPE);
         } else {
            return new PrintStmt(file, exprs, err);
         }
      }
   }

   private PrintStmt(String file, List<Expression> exprs, ErrorType errorType) {
      this.file = file;
      this.exprs = exprs == null ? new Vector(0) : new Vector(exprs);
      this.errorType = errorType;
   }

   List<ProgramLine> getLines(int indent) {
      return oneLine(new PrintLine(indent));
   }

   private String getError() {
      if (this.errorType == PrintStmt.ErrorType.COLON) {
         return "Hiányzik a kettőspont az utasításból.";
      } else if (this.errorType == PrintStmt.ErrorType.TYPE) {
         return "Csak KIFÁJL-ba lehet adatokat kiírni.";
      } else {
         if (this.errorType == PrintStmt.ErrorType.EXPR) {
            for(Expression e : this.exprs) {
               String err = e.getError();
               if (err != null) {
                  return err;
               }
            }
         }

         return null;
      }
   }

   State execute(State state) {
      String name = this.file == null ? "KIMENET" : (String)state.getVar(this.file);
      if (name.equals("<--LEZART-->")) {
         return state.newError("Nincs megnyitva a fájl.");
      } else {
         StreamData strm = state.getStream(name);
         StreamState sst = state.getStreamState(name);

         for(Expression e : this.exprs) {
            Object val = e.getValue(state);
            if (val instanceof BadValue) {
               return state.newError(val.toString());
            }

            e.getType().printData(strm, sst, val);
         }

         state = state.newState();
         state.setStatement(this.getNext());
         return state;
      }
   }

   boolean hasError() {
      return this.errorType != PrintStmt.ErrorType.NONE;
   }

   private static enum ErrorType {
      NONE,
      COLON,
      EXPR,
      TYPE;
   }

   private class PrintLine extends ProgramLine {
      PrintLine(int ind) {
         super(ind, PrintStmt.this.getError());
      }

      protected String render() {
         String str = this.indent() + "KI";
         if (PrintStmt.this.file != null) {
            str = str + " " + (PrintStmt.this.errorType == PrintStmt.ErrorType.TYPE ? bad(PrintStmt.this.file) : PrintStmt.this.file);
         }

         if (PrintStmt.this.errorType == PrintStmt.ErrorType.COLON) {
            return str + bad(":");
         } else {
            str = str + ": ";
            String comma = "";

            for(Expression e : PrintStmt.this.exprs) {
               str = str + comma + e.render();
               comma = ", ";
            }

            return str;
         }
      }

      public ExprNode getExpr(State state) {
         ExprNode[] chld = new ExprNode[PrintStmt.this.exprs.size()];
         int i = 0;

         for(Expression e : PrintStmt.this.exprs) {
            chld[i++] = e.getTree(state);
         }

         return new ExprNode((String)null, "KI:", chld);
      }

      public void setLine(int l) {
         PrintStmt.this.setLineIndex(l);
      }
   }
}
