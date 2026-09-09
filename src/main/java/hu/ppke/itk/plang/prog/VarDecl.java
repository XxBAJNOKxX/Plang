package hu.ppke.itk.plang.prog;

import hu.ppke.itk.plang.gui.ExprNode;
import hu.ppke.itk.plang.gui.ProgramLine;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

class VarDecl {
   private Vector<String> names;
   private Type type;
   private boolean last;
   private ErrorType errorType;
   private int errIndex;

   static VarDecl parseVarDecl(Lexer lex, Environment env) {
      LinkedList<String> names = new LinkedList();
      if (!lex.isIdent()) {
         return new VarDecl(names, (Type)null, VarDecl.ErrorType.NAME);
      } else {
         names.add(lex.getString());
         lex.next();

         while(lex.isKeyword(",")) {
            lex.next();
            if (!lex.isIdent()) {
               return new VarDecl(names, (Type)null, VarDecl.ErrorType.NAME);
            }

            names.add(lex.getString());
            lex.next();
         }

         if (!lex.isKeyword(":")) {
            return new VarDecl(names, (Type)null, VarDecl.ErrorType.COLON);
         } else {
            lex.next();
            Type type = Type.parseType(lex, env);
            VarDecl vd = new VarDecl(names, type, VarDecl.ErrorType.NONE);
            vd.apply(env);
            return vd;
         }
      }
   }

   void apply(Environment env) {
      if (this.errorType == VarDecl.ErrorType.NONE) {
         for(int i = 0; i < this.names.size(); ++i) {
            if (env.hasVar((String)this.names.elementAt(i))) {
               this.errorType = VarDecl.ErrorType.DUP_NAME;
               this.errIndex = i;
            } else {
               env.addVar((String)this.names.elementAt(i), this.type);
            }
         }

      }
   }

   public void setLast(boolean l) {
      this.last = l;
   }

   public VarDecl(List<String> names, Type type, ErrorType errorType) {
      this.errorType = VarDecl.ErrorType.NONE;
      this.errIndex = -1;
      this.names = new Vector(names);
      this.type = type;
      this.errorType = errorType;
      if (errorType == VarDecl.ErrorType.NONE && type == null) {
         this.errorType = VarDecl.ErrorType.TYPE;
      }

      this.last = true;
   }

   public ProgramLine getLine(int indent) {
      return new DeclLine(indent);
   }

   private String getError() {
      if (this.errorType == VarDecl.ErrorType.NAME) {
         return "Hiányzik a következő változó azonosítója.";
      } else if (this.errorType == VarDecl.ErrorType.COLON) {
         return "Hiányzik a változók után a kettőspont.";
      } else if (this.errorType == VarDecl.ErrorType.TYPE) {
         return "Hibás a típus megadása.";
      } else {
         return this.errorType == VarDecl.ErrorType.DUP_NAME ? "Ilyen nevű változó már létezik." : null;
      }
   }

   private static enum ErrorType {
      NONE,
      NAME,
      COLON,
      TYPE,
      DUP_NAME;
   }

   final class DeclLine extends ProgramLine {
      DeclLine(int ind) {
         super(ind, VarDecl.this.getError());
      }

      protected String render() {
         Iterator<String> i = VarDecl.this.names.iterator();
         if (!i.hasNext()) {
            return ProgramLine.bad("???");
         } else {
            int ind = 0;

            String str;
            for(str = this.indent() + (ind == VarDecl.this.errIndex ? ProgramLine.bad((String)i.next()) : (String)i.next()); i.hasNext(); str = str + ", " + (ind == VarDecl.this.errIndex ? ProgramLine.bad((String)i.next()) : (String)i.next())) {
               ++ind;
            }

            if (VarDecl.this.errorType == VarDecl.ErrorType.NAME) {
               return str + ", " + ProgramLine.bad("???");
            } else if (VarDecl.this.errorType == VarDecl.ErrorType.COLON) {
               return ProgramLine.bad(":");
            } else if (VarDecl.this.errorType == VarDecl.ErrorType.TYPE) {
               return str + ": " + ProgramLine.bad("???");
            } else {
               return str + ": " + VarDecl.this.type.render() + (VarDecl.this.last ? "" : ",");
            }
         }
      }

      public ExprNode getExpr(State state) {
         return ExprNode.EMPTY;
      }

      public void setLine(int l) {
      }
   }
}
