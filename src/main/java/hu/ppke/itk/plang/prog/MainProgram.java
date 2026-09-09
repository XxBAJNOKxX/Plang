package hu.ppke.itk.plang.prog;

import hu.ppke.itk.plang.gui.ExprNode;
import hu.ppke.itk.plang.gui.ProgramLine;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainProgram extends Program {
   private ErrorType errorType;
   private List<String> comments;
   private static Set<String> termKeyw = new HashSet(Arrays.asList("program_vege"));

   public static MainProgram parseMainProgram(Lexer lex) {
      List<String> comments = new LinkedList();

      while(lex.isKeyword("**")) {
         lex.next();
         comments.add(lex.skip());
      }

      if (!lex.isKeyword("program")) {
         return new MainProgram(MainProgram.ErrorType.HEAD, comments, UnparsedLines.skip(lex, "program_vege"));
      } else {
         lex.next();
         if (!lex.isIdent()) {
            return new MainProgram(MainProgram.ErrorType.NAME, comments, UnparsedLines.skip(lex, "program_vege"));
         } else {
            String name = lex.getString();
            lex.next();
            Environment env = new Environment();
            Declarations decls = Declarations.parseDecl(lex, env);
            Statements stmts = Statements.parseStmt(lex, env, termKeyw);
            return !lex.isKeyword("program_vege") ? new MainProgram(name, comments, decls, stmts, MainProgram.ErrorType.TAIL) : new MainProgram(name, comments, decls, stmts, env);
         }
      }
   }

   private MainProgram(ErrorType errType, List<String> comments, UnparsedLines unparsed) {
      this((String)null, comments, (Declarations)null, (Statements)null, (Environment)null, errType, unparsed);
   }

   private MainProgram(String name, List<String> comments, Declarations decl, Statements stmts, ErrorType errType) {
      this(name, comments, decl, stmts, (Environment)null, errType, (UnparsedLines)null);
   }

   private MainProgram(String name, List<String> comments, Declarations decl, Statements stmts, Environment env) {
      this(name, comments, decl, stmts, env, MainProgram.ErrorType.NONE, (UnparsedLines)null);
   }

   private MainProgram(String name, List<String> comments, Declarations decl, Statements stmts, Environment env, ErrorType errType, UnparsedLines unparsed) {
      super(name, decl, stmts, env, unparsed);
      if (comments != null) {
         this.comments = new ArrayList(comments);
      }

      this.errorType = errType;
      if (this.errorType == MainProgram.ErrorType.NONE && stmts.hasError()) {
         this.errorType = MainProgram.ErrorType.BODY;
      }

   }

   public List<State> runProgram(Map<String, String> input, int maxSteps) {
      State state = new State(this.getEnv(), (long)maxSteps);

      for(String stream : input.keySet()) {
         state.getStream(stream).addContent((String)input.get(stream));
      }

      return this.runProgram(state);
   }

   protected List<ProgramLine> getPrefixLines(int indent) {
      LinkedList<ProgramLine> lst = new LinkedList();

      for(String s : this.comments) {
         lst.add(new CommentLine(s));
      }

      return lst;
   }

   protected ProgramLine getHeadLine(int indent) {
      return new ProgramHead();
   }

   protected ProgramLine getTailLine(int indent) {
      return new ProgramTail();
   }

   public boolean hasError() {
      return this.errorType != MainProgram.ErrorType.NONE;
   }

   private String errStr(boolean need) {
      if (!need) {
         return null;
      } else if (this.errorType == MainProgram.ErrorType.HEAD) {
         return "A programnak a PROGRAM kulcsszóval kell kezdődnie.";
      } else if (this.errorType == MainProgram.ErrorType.NAME) {
         return "Hiányzik a program neve (egy azonosító).";
      } else {
         return this.errorType == MainProgram.ErrorType.TAIL ? "A programot a PROGRAM_VÉGE kulcsszóval kell lezárni." : null;
      }
   }

   private static enum ErrorType {
      NONE,
      HEAD,
      NAME,
      TAIL,
      BODY;
   }

   private class CommentLine extends ProgramLine {
      private String str;

      CommentLine(String str) {
         super(0);
         this.str = str;
      }

      protected String render() {
         return "** " + this.str;
      }

      public ExprNode getExpr(State state) {
         return new ExprNode((String)null, "---", (ExprNode[])null);
      }

      public void setLine(int l) {
      }
   }

   private class ProgramHead extends ProgramLine {
      ProgramHead() {
         super(0, MainProgram.this.errStr(MainProgram.this.errorType == MainProgram.ErrorType.HEAD || MainProgram.this.errorType == MainProgram.ErrorType.NAME));
      }

      protected String render() {
         if (MainProgram.this.errorType == MainProgram.ErrorType.HEAD) {
            return bad("PROGRAM");
         } else {
            return MainProgram.this.errorType == MainProgram.ErrorType.NAME ? "PROGRAM " + bad("???") : "PROGRAM " + MainProgram.this.getName();
         }
      }

      public ExprNode getExpr(State state) {
         return new ExprNode((String)null, "---", (ExprNode[])null);
      }

      public void setLine(int l) {
      }
   }

   private class ProgramTail extends ProgramLine {
      ProgramTail() {
         super(0, MainProgram.this.errStr(MainProgram.this.errorType == MainProgram.ErrorType.TAIL));
      }

      protected String render() {
         return MainProgram.this.errorType == MainProgram.ErrorType.TAIL ? bad("PROGRAM_VÉGE") : "PROGRAM_VÉGE";
      }

      public ExprNode getExpr(State state) {
         return new ExprNode((String)null, state == null ? "---" : "A program véget ért.", (ExprNode[])null);
      }

      public void setLine(int l) {
         MainProgram.this.setEndLine(l);
      }
   }
}
