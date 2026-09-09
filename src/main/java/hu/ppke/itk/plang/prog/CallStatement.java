package hu.ppke.itk.plang.prog;

import hu.ppke.itk.plang.gui.ExprNode;
import hu.ppke.itk.plang.gui.ProgramLine;
import java.util.LinkedList;
import java.util.List;

class CallStatement extends Statement {
   private Error error;
   private int errorInd;
   private String name;
   private List<Param> params;
   private SubProgram subProg;

   static CallStatement parseCallStmt(String ident, Lexer lex, Environment env) {
      lex.next();
      SubProgram sp = env.getSubProg(ident);
      LinkedList<Param> params = new LinkedList();
      boolean hasMore = !lex.isKeyword(")");
      int errInd = -1;
      Error err = CallStatement.Error.NONE;

      while(hasMore) {
         ParDecl par = sp != null && sp.getParamCount() > params.size() ? sp.getParam(params.size()) : null;
         if (par != null && par.getKind() != ParDecl.Kind.INPUT) {
            AssignableExpr e = AssignableExpr.parseAssignable(lex, env);
            if (errInd < 0 && e.getError() != null) {
               err = CallStatement.Error.PARAM;
               errInd = params.size();
            } else if (errInd < 0 && !e.getType().canCopy(par.getType())) {
               err = CallStatement.Error.TYPE;
               errInd = -1;
            }

            params.add(new Param(e));
         } else {
            Expression e = Expression.parseExpression(lex, env);
            if (errInd < 0 && e.getError() != null) {
               err = CallStatement.Error.PARAM;
               errInd = params.size();
            } else if (par != null && errInd < 0 && !par.getType().canCopy(e.getType())) {
               err = CallStatement.Error.TYPE;
               errInd = params.size();
            }

            params.add(new Param(e));
         }

         if (lex.isKeyword(",")) {
            lex.next();
         } else {
            hasMore = false;
         }
      }

      if (!lex.isKeyword(")")) {
         return new CallStatement(ident, params, CallStatement.Error.PAREN);
      } else {
         lex.next();
         if (sp == null) {
            return new CallStatement(ident, params, CallStatement.Error.EXIST);
         } else if (params.size() != sp.getParamCount()) {
            return new CallStatement(ident, params, CallStatement.Error.COUNT);
         } else if (errInd >= 0) {
            return new CallStatement(ident, params, sp, err, errInd);
         } else {
            return new CallStatement(ident, params, sp);
         }
      }
   }

   private CallStatement(String name, List<Param> params, SubProgram subProg, Error error, int errInd) {
      this.name = name;
      this.subProg = subProg;
      this.params = params;
      this.error = error;
      this.errorInd = errInd;
   }

   private CallStatement(String name, List<Param> params, Error error) {
      this(name, params, (SubProgram)null, error, -1);
   }

   private CallStatement(String name, List<Param> params, SubProgram subProg) {
      this(name, params, subProg, CallStatement.Error.NONE, -1);
   }

   private String getError() {
      if (this.error == CallStatement.Error.PARAM) {
         return ((Param)this.params.get(this.errorInd)).getError();
      } else {
         return this.error == CallStatement.Error.TYPE ? ((Param)this.params.get(this.errorInd)).getType().toString() + " típusú " + (this.errorInd + 1) + ". paraméter nem felel meg a várt " + this.subProg.getParam(this.errorInd).getType() + " típusnak." : this.error.msg();
      }
   }

   List<ProgramLine> getLines(int indent) {
      return oneLine(new ProgramLine(indent, this.getError()) {
         protected String render() {
            String s = this.indent();
            if (CallStatement.this.error == CallStatement.Error.EXIST) {
               s = s + bad(CallStatement.this.name) + "(";
            } else {
               s = s + CallStatement.this.name + "(";
            }

            String comma = "";

            for(Param p : CallStatement.this.params) {
               s = s + comma + p.render();
               comma = ", ";
            }

            if (CallStatement.this.error == CallStatement.Error.PAREN) {
               s = s + bad(")");
            } else {
               s = s + ")";
            }

            return s;
         }

         public void setLine(int l) {
            CallStatement.this.setLineIndex(l);
         }

         public ExprNode getExpr(State state) {
            return state == null ? new ExprNode(CallStatement.this.name + "()", (ExprNode[])null, (String)null) : new ExprNode(CallStatement.this.name + "()", (ExprNode[])null, state.getSubStates(0), (String)null);
         }
      });
   }

   State execute(State state) {
      Object[] parVal = new Object[this.params.size()];
      int pos = 0;

      for(Param p : this.params) {
         if (p.input != null) {
            parVal[pos] = p.input.getValue(state);
         }

         ++pos;
      }

      List<State> subStates = this.subProg.runProgram(state, parVal);
      state.addSubStates(0, subStates);
      state = state.newState();
      pos = 0;

      for(Param p : this.params) {
         if (p.output != null) {
            p.output.assign(state, parVal[pos]);
         }

         ++pos;
      }

      state.setStatement(this.getNext());
      return state;
   }

   boolean hasError() {
      return this.error != CallStatement.Error.NONE;
   }

   private static class Param {
      Expression input;
      AssignableExpr output;

      Param(Expression e) {
         this.input = e;
      }

      Param(AssignableExpr e) {
         this.output = e;
      }

      String render() {
         return this.input != null ? this.input.render() : this.output.render();
      }

      String getError() {
         return this.input != null ? this.input.getError() : this.output.getError();
      }

      Type getType() {
         return this.input != null ? this.input.getType() : this.output.getType();
      }
   }

   private static enum Error {
      NONE((String)null),
      PAREN("Hiányzik a paraméterlista záró zárójele."),
      EXIST("Nem létezik ilyen nevű eljárás."),
      COUNT("A paraméterek száma nem megfelelő."),
      TYPE((String)null),
      PARAM((String)null);

      private String msg;

      private Error(String msg) {
         this.msg = msg;
      }

      String msg() {
         return this.msg;
      }
   }
}
