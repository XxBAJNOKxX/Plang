package hu.ppke.itk.plang.prog;

import hu.ppke.itk.plang.gui.ProgramLine;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

class Signature {
   private Vector<ParDecl> params;
   private Type returns;
   private Error error;
   private int errorInd;

   public static Signature parseSignature(SubProgram.Kind kind, Lexer lex, Environment subEnv) {
      if (!lex.isKeyword("(")) {
         return new Signature(kind, (LinkedList)null, (Type)null, Signature.Error.OPAREN);
      } else {
         lex.next();
         LinkedList<ParDecl> param = new LinkedList();
         ParDecl.Kind parKind = ParDecl.Kind.INPUT;
         boolean hasMore = !lex.isKeyword(")");

         while(hasMore) {
            if (lex.isKeyword("be")) {
               parKind = ParDecl.Kind.INPUT;
               lex.next();
            } else if (lex.isKeyword("ki")) {
               parKind = ParDecl.Kind.OUTPUT;
               lex.next();
            }

            param.addAll(ParDecl.parseParDecls(parKind, lex, subEnv));
            if (lex.isKeyword(",")) {
               lex.next();
            } else {
               hasMore = false;
            }
         }

         if (!lex.isKeyword(")")) {
            return new Signature(kind, param, (Type)null, Signature.Error.CPAREN);
         } else {
            lex.next();
            Type ret = null;
            if (kind == SubProgram.Kind.FUNC) {
               if (!lex.isKeyword(":")) {
                  return new Signature(kind, param, (Type)null, Signature.Error.NORET);
               }

               lex.next();
               ret = Type.parseType(lex, subEnv);
               if (ret == null) {
                  return new Signature(kind, param, (Type)null, Signature.Error.RETURN);
               }
            }

            return new Signature(kind, param, ret, Signature.Error.NONE);
         }
      }
   }

   private Signature(SubProgram.Kind kind, LinkedList<ParDecl> params, Type returns, Error error) {
      this.params = new Vector(params);
      this.returns = returns;
      this.error = error;
      if (error == Signature.Error.NONE) {
         this.errorInd = 0;

         for(ParDecl par : params) {
            if (kind == SubProgram.Kind.FUNC && par.getKind() == ParDecl.Kind.OUTPUT) {
               error = Signature.Error.OUTPAR;
               break;
            }

            if (par.getError() != null) {
               error = Signature.Error.PARAM;
               break;
            }

            ++this.errorInd;
         }
      }

   }

   public String render() {
      if (this.error == Signature.Error.OPAREN) {
         return ProgramLine.bad("(");
      } else {
         String s = "(";
         ParDecl.Kind pk = ParDecl.Kind.INPUT;
         Iterator<ParDecl> par = this.params.iterator();

         ParDecl next;
         for(ParDecl prev = par.hasNext() ? (ParDecl)par.next() : null; prev != null; prev = next) {
            next = par.hasNext() ? (ParDecl)par.next() : null;
            if (prev.getKind() != pk) {
               s = s + (this.error == Signature.Error.OUTPAR ? ProgramLine.bad(prev.getKind().toString()) : prev.getKind()) + " ";
               pk = prev.getKind();
            }

            if (next != null && prev.getKind() == next.getKind() && prev.getType() == next.getType()) {
               s = s + prev.renderName() + ", ";
            } else {
               s = s + prev.renderName() + ": " + prev.renderType();
               if (next != null) {
                  s = s + ", ";
               }
            }
         }

         if (this.error == Signature.Error.CPAREN) {
            s = s + ProgramLine.bad(")");
         } else {
            s = s + ")";
         }

         if (this.error == Signature.Error.NORET) {
            s = s + ProgramLine.bad(": ???");
         } else if (this.error == Signature.Error.RETURN) {
            s = s + ": " + ProgramLine.bad("???");
         } else if (this.returns != null) {
            s = s + ": " + this.returns.render();
         }

         return s;
      }
   }

   public String getError() {
      return this.error == Signature.Error.PARAM ? ((ParDecl)this.params.get(this.errorInd)).getError() : this.error.msg();
   }

   int getParCount() {
      return this.params.size();
   }

   ParDecl getPar(int index) {
      return (ParDecl)this.params.get(index);
   }

   private static enum Error {
      NONE((String)null),
      OPAREN("Hiányzik a paraméterlista nyitó zárójele."),
      CPAREN("Hiányzik a paraméterlista záró zárójele."),
      PARAM((String)null),
      OUTPAR("Függvénynek nem lehet kimenő paramétere"),
      NORET("Hiányzik az eredmény típusa."),
      RETURN("Hibás az eredmény típusa.");

      String msg;

      private Error(String msg) {
         this.msg = msg;
      }

      String msg() {
         return this.msg;
      }
   }
}
