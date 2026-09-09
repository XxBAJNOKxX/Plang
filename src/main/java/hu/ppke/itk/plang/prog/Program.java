package hu.ppke.itk.plang.prog;

import hu.ppke.itk.plang.gui.ProgramLine;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public abstract class Program {
   private String name;
   private Declarations decls;
   private Statements stmts;
   private Environment env;
   private UnparsedLines unparsed;
   private Statement dummyLast;

   protected Program(String name, Declarations decls, Statements stmts, Environment env, UnparsedLines errLines) {
      this.dummyLast = new DummyStatement((DummyStatement)null);
      this.name = name;
      this.decls = decls;
      this.stmts = stmts;
      this.env = env;
      this.unparsed = errLines;
   }

   protected Program(String name, Declarations decls, Statements stmts, Environment env) {
      this(name, decls, stmts, env, (UnparsedLines)null);
   }

   protected Program(UnparsedLines errLines) {
      this((String)null, (Declarations)null, (Statements)null, (Environment)null, errLines);
   }

   protected String getName() {
      return this.name;
   }

   protected Environment getEnv() {
      return this.env;
   }

   protected abstract ProgramLine getHeadLine(int var1);

   protected abstract ProgramLine getTailLine(int var1);

   protected List<ProgramLine> getPrefixLines(int indent) {
      return new LinkedList();
   }

   public List<ProgramLine> getLines() {
      return this.getLines(0);
   }

   public List<ProgramLine> getLines(int indent) {
      LinkedList<ProgramLine> l = new LinkedList();
      l.addAll(this.getPrefixLines(indent));
      l.add(this.getHeadLine(indent));
      if (this.decls != null) {
         l.addAll(this.decls.getLines(indent + 1));
      }

      if (this.stmts != null) {
         l.addAll(this.stmts.getLines(indent + 1));
      }

      if (this.unparsed != null) {
         l.addAll(this.unparsed.getLines(indent + 1));
      }

      l.add(this.getTailLine(indent));
      return l;
   }

   public Set<String> getStreams(StreamKind kind) {
      return this.env.getStreams(kind);
   }

   public abstract boolean hasError();

   protected List<State> runProgram(State state) {
      state.setStatement(this.stmts.getFirst());
      LinkedList<State> states = new LinkedList();
      states.add(state);

      for(State var3 = state.execNext(); var3 != null; var3 = var3.execNext()) {
         states.add(var3);
      }

      ((State)states.getLast()).setStatement(this.dummyLast);
      return states;
   }

   protected void setEndLine(int l) {
      this.dummyLast.setLineIndex(l);
   }

   public Type getVarType(String var) {
      return this.env.getVarType(var);
   }

   private class DummyStatement extends Statement {
      private DummyStatement() {
      }

      List<ProgramLine> getLines(int indent) {
         return null;
      }

      State execute(State state) {
         return null;
      }

      boolean hasError() {
         return false;
      }

      // $FF: synthetic method
      DummyStatement(DummyStatement var2) {
         this();
      }
   }
}
