package hu.ppke.itk.plang.prog;

import hu.ppke.itk.plang.gui.ExprNode;
import hu.ppke.itk.plang.gui.ProgramLine;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SubProgram extends Program {
   private static Map<Kind, Set<String>> termKeyw = new EnumMap(Kind.class);
   private Kind kind;
   private Signature signature;
   private ErrorType errorType;
   private Environment env;

   static {
      termKeyw.put(SubProgram.Kind.PROC, new HashSet(Arrays.asList("eljaras_vege", "program_vege")));
      termKeyw.put(SubProgram.Kind.FUNC, new HashSet(Arrays.asList("fuggveny_vege", "program_vege")));
   }

   static SubProgram parseSubProgram(Lexer lex, Environment env) {
      Kind kind;
      if (lex.isKeyword("eljaras")) {
         kind = SubProgram.Kind.PROC;
      } else {
         if (!lex.isKeyword("fuggveny")) {
            return null;
         }

         kind = SubProgram.Kind.FUNC;
      }

      lex.next();
      if (!lex.isIdent()) {
         return new SubProgram(kind, SubProgram.ErrorType.NAME);
      } else {
         String name = lex.getString();
         lex.next();
         Environment subEnv = new Environment(env);
         Signature sig = Signature.parseSignature(kind, lex, subEnv);
         Declarations decls = Declarations.parseDecl(lex, subEnv);
         Statements stmts = Statements.parseStmt(lex, subEnv, (Set)termKeyw.get(kind));
         if ((kind != SubProgram.Kind.PROC || lex.isKeyword("eljaras_vege")) && (kind != SubProgram.Kind.FUNC || lex.isKeyword("fuggveny_vege"))) {
            lex.next();
            SubProgram sp = new SubProgram(kind, name, sig, decls, stmts, subEnv, SubProgram.ErrorType.NONE);
            sp.apply(env);
            return sp;
         } else {
            return new SubProgram(kind, name, sig, decls, stmts, SubProgram.ErrorType.TAIL);
         }
      }
   }

   private SubProgram(Kind kind, String name, Signature sig, Declarations decl, Statements stmts, Environment env, ErrorType errType) {
      super(name, decl, stmts, env);
      this.signature = sig;
      this.kind = kind;
      this.env = env;
      this.errorType = errType;
      if (this.errorType == SubProgram.ErrorType.NONE && stmts.hasError()) {
         this.errorType = SubProgram.ErrorType.BODY;
      }

   }

   private SubProgram(Kind kind, ErrorType errorType) {
      this(kind, (String)null, (Signature)null, (Declarations)null, (Statements)null, (Environment)null, errorType);
   }

   private SubProgram(Kind kind, String name, Signature sig, Declarations decls, Statements stmts, ErrorType errorType) {
      this(kind, name, sig, decls, stmts, (Environment)null, errorType);
   }

   public List<State> runProgram(State parent, Object[] param) {
      State state = new State(this.env, parent);

      for(int i = 0; i < this.signature.getParCount(); ++i) {
         ParDecl par = this.signature.getPar(i);
         if (par.getKind() == ParDecl.Kind.INPUT) {
            state.setVar(par.getName(), param[i]);
         }
      }

      List<State> lst = super.runProgram(state);
      State last = (State)lst.get(lst.size() - 1);

      for(int i = 0; i < this.signature.getParCount(); ++i) {
         ParDecl par = this.signature.getPar(i);
         if (par.getKind() == ParDecl.Kind.OUTPUT) {
            param[i] = last.getVar(par.getName());
         }
      }

      for(String stn : parent.getStreamNames()) {
         parent.getStreamState(stn).advanceTo(last.getStreamState(stn).getPtr());
      }

      return lst;
   }

   private String headError() {
      return this.errorType == SubProgram.ErrorType.NAME ? "Hiányzik az alprogram neve." : null;
   }

   protected ProgramLine getHeadLine(int indent) {
      return new SubProgHead(indent);
   }

   private String tailError() {
      return this.errorType == SubProgram.ErrorType.TAIL ? "Hiányzik " + (this.kind == SubProgram.Kind.PROC ? "az ELJÁRÁS_VÉGE" : "a FÜGGVÉNY_VÉGE") + " kulcsszó." : null;
   }

   protected ProgramLine getTailLine(int indent) {
      return new SubProgTail(indent);
   }

   public boolean hasError() {
      return this.errorType != SubProgram.ErrorType.NONE;
   }

   private void apply(Environment env) {
      env.addSubProg(this);
   }

   int getParamCount() {
      return this.signature.getParCount();
   }

   ParDecl getParam(int index) {
      return this.signature.getPar(index);
   }

   static enum Kind {
      PROC,
      FUNC;
   }

   private static enum ErrorType {
      NONE,
      BODY,
      NAME,
      TAIL,
      PARAM,
      RETURN,
      OPAREN;
   }

   private class SubProgHead extends ProgramLine {
      SubProgHead(int indent) {
         super(indent, SubProgram.this.headError());
      }

      protected String render() {
         String kwd = SubProgram.this.kind == SubProgram.Kind.PROC ? "ELJÁRÁS " : "FÜGGVÉNY ";
         return SubProgram.this.errorType == SubProgram.ErrorType.NAME ? this.indent() + kwd + bad("???") : this.indent() + kwd + SubProgram.this.getName() + SubProgram.this.signature.render();
      }

      public ExprNode getExpr(State state) {
         return new ExprNode((String)null, "---", (ExprNode[])null);
      }

      public void setLine(int l) {
      }
   }

   private class SubProgTail extends ProgramLine {
      SubProgTail(int indent) {
         super(indent, SubProgram.this.tailError());
      }

      protected String render() {
         String kwd = SubProgram.this.kind == SubProgram.Kind.PROC ? "ELJÁRÁS_VÉGE" : "FÜGGVÉNY_VÉGE";
         return SubProgram.this.errorType == SubProgram.ErrorType.TAIL ? this.indent() + bad(kwd) : this.indent() + kwd;
      }

      public ExprNode getExpr(State state) {
         return new ExprNode((String)null, state == null ? "---" : "Az alprogram véget ért.", (ExprNode[])null);
      }

      public void setLine(int l) {
         SubProgram.this.setEndLine(l);
      }
   }
}
