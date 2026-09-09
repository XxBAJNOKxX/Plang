package hu.ppke.itk.plang.prog;

import hu.ppke.itk.plang.gui.ExprNode;
import hu.ppke.itk.plang.gui.ProgramLine;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

class ReadStmt extends Statement {
   private final String file;
   private Vector<AssignableExpr> exprs;
   private ErrorType errorType;

   public static ReadStmt parseReadStmt(Lexer lex, Environment env) {
      String file = null;
      if (lex.isIdent()) {
         file = lex.getString();
         lex.next();
      }

      if (!lex.isKeyword(":")) {
         return new ReadStmt(file, (LinkedList)null, ReadStmt.ErrorType.COLON);
      } else {
         lex.next();
         ErrorType err = ReadStmt.ErrorType.NONE;
         LinkedList<AssignableExpr> exprs = new LinkedList();
         exprs.add(AssignableExpr.parseAssignable(lex, env));
         if (((AssignableExpr)exprs.getLast()).getError() != null) {
            err = ReadStmt.ErrorType.EXPR;
         }

         while(lex.isKeyword(",")) {
            lex.next();
            exprs.add(AssignableExpr.parseAssignable(lex, env));
            if (((AssignableExpr)exprs.getLast()).getError() != null) {
               err = ReadStmt.ErrorType.EXPR;
            }
         }

         if (file != null && env.getVarType(file) != FileType.INPUT) {
            return new ReadStmt(file, exprs, ReadStmt.ErrorType.TYPE);
         } else {
            return new ReadStmt(file, exprs, err);
         }
      }
   }

   private ReadStmt(String file, LinkedList<AssignableExpr> exprs, ErrorType errorType) {
      this.file = file;
      this.exprs = exprs == null ? new Vector(0) : new Vector(exprs);
      this.errorType = errorType;
   }

   List<ProgramLine> getLines(int indent) {
      return oneLine(new ReadLine(indent));
   }

   private String getError() {
      if (this.errorType == ReadStmt.ErrorType.COLON) {
         return "Hiányzik a kettőspont az utasításból.";
      } else if (this.errorType == ReadStmt.ErrorType.TYPE) {
         return "Csak BEFÁJL-ból lehet adatokat beolvasni.";
      } else {
         if (this.errorType == ReadStmt.ErrorType.EXPR) {
            for(AssignableExpr e : this.exprs) {
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
      String name = this.file == null ? "BEMENET" : (String)state.getVar(this.file);
      if (name.equals("<--LEZART-->")) {
         return state.newError("Nincs megnyitva a fájl.");
      } else {
         StreamData strm = state.getStream(name);
         StreamState sst = state.getStreamState(name);
         LinkedList<Object> values = new LinkedList();

         for(AssignableExpr e : this.exprs) {
            try {
               if (!sst.isEof()) {
                  values.add(e.getType().readData(strm, sst));
               }
            } catch (StreamData.DataError var9) {
               if (this.file == null) {
                  values.add(new BadValue("Nem sikerült a beolvasás."));
                  break;
               }

               sst.setEof();
            }
         }

         state = state.newState();

         for(AssignableExpr e : this.exprs) {
            if (values.isEmpty()) {
               e.assign(state, (Object)null);
            } else {
               Object val = values.remove();
               e.assign(state, val);
               if (val instanceof BadValue) {
                  state.setError("Nem sikerült a beolvasás.");
                  return state;
               }
            }
         }

         state.setStatement(this.getNext());
         return state;
      }
   }

   boolean hasError() {
      return this.errorType != ReadStmt.ErrorType.NONE;
   }

   private static enum ErrorType {
      NONE,
      COLON,
      EXPR,
      TYPE;
   }

   private class ReadLine extends ProgramLine {
      ReadLine(int ind) {
         super(ind, ReadStmt.this.getError());
      }

      protected String render() {
         String str = this.indent() + "BE";
         if (ReadStmt.this.file != null) {
            str = str + " " + (ReadStmt.this.errorType == ReadStmt.ErrorType.TYPE ? bad(ReadStmt.this.file) : ReadStmt.this.file);
         }

         if (ReadStmt.this.errorType == ReadStmt.ErrorType.COLON) {
            return str + bad(":");
         } else {
            str = str + ": ";
            String comma = "";

            for(AssignableExpr e : ReadStmt.this.exprs) {
               str = str + comma + e.render();
               comma = ", ";
            }

            return str;
         }
      }

      public ExprNode getExpr(State state) {
         ExprNode[] chld = new ExprNode[ReadStmt.this.exprs.size()];
         int i = 0;

         for(AssignableExpr e : ReadStmt.this.exprs) {
            chld[i++] = e.getTree(state);
         }

         return new ExprNode((String)null, "BE:", chld);
      }

      public void setLine(int l) {
         ReadStmt.this.setLineIndex(l);
      }
   }
}
