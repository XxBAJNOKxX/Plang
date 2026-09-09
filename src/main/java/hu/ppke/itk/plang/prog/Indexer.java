package hu.ppke.itk.plang.prog;

import hu.ppke.itk.plang.gui.ExprNode;
import hu.ppke.itk.plang.gui.ProgramLine;

class Indexer extends Accessor {
   private Expression index;
   private Accessor next;
   private Type baseType;
   private ErrorType errorType;

   static Indexer parseIndexer(Lexer lex, Environment env, Type baseType) {
      lex.next();
      Expression index = Expression.parseExpression(lex, env);
      if (!lex.isKeyword("]")) {
         return new Indexer(baseType, index, (Accessor)null, Indexer.ErrorType.BRACKET);
      } else {
         lex.next();
         Type parType = baseType == null ? null : baseType.operatorType(BinaryOperator.BRACKET, index.getType());
         return new Indexer(baseType, index, Accessor.parseAccessors(lex, env, parType));
      }
   }

   private Indexer(Type baseType, Expression index, Accessor next, ErrorType errorType) {
      this.index = index;
      this.next = next;
      this.baseType = baseType;
      this.errorType = errorType;
      if (errorType == Indexer.ErrorType.NONE) {
         if (baseType == null) {
            this.errorType = Indexer.ErrorType.BASE;
         } else if (index.getError() != null) {
            this.errorType = Indexer.ErrorType.INDEX;
         } else if (!baseType.hasAccessor(BinaryOperator.BRACKET, index.getType())) {
            this.errorType = Indexer.ErrorType.TYPE;
         } else if (next.getError() != null) {
            this.errorType = Indexer.ErrorType.NEXT;
         }
      }

   }

   private Indexer(Type baseType, Expression index, Accessor next) {
      this(baseType, index, next, Indexer.ErrorType.NONE);
   }

   Object access(State state, Object baseValue, Object newValue) {
      if (baseValue instanceof EmptyValue) {
         return new BadValue("Az alapkifejezés nem kapott kezdőértéket.");
      } else {
         Object ind = this.index.getValue(state);
         if (ind instanceof BadValue) {
            return ind;
         } else {
            Object oldValue = this.baseType.apply(BinaryOperator.BRACKET, this.index.getType(), baseValue, ind);
            if (oldValue instanceof BadValue && !(oldValue instanceof EmptyValue)) {
               return oldValue;
            } else {
               newValue = this.next.access(state, oldValue, newValue);
               return this.baseType.access(BinaryOperator.BRACKET, baseValue, ind, newValue);
            }
         }
      }
   }

   String render() {
      if (this.errorType == Indexer.ErrorType.BRACKET) {
         return "[" + this.index.render() + ProgramLine.bad(" ??? ");
      } else {
         return this.errorType == Indexer.ErrorType.TYPE ? ProgramLine.bad("[" + this.index.render() + "]") + this.next.render() : "[" + this.index.render() + "]" + this.next.render();
      }
   }

   public String toString() {
      return "[" + this.index + "]" + this.next;
   }

   ExprNode getTree(State state, String pref, ExprNode subExpr) {
      String s = pref + "[" + this.index.render() + "]";
      return this.next.getTree(state, s, new ExprNode(s, new ExprNode[]{subExpr, this.index.getTree(state)}, (String)null));
   }

   String getError() {
      if (this.errorType == Indexer.ErrorType.BRACKET) {
         return "Hiányzik a záró szögletes zárójel.";
      } else if (this.errorType == Indexer.ErrorType.INDEX) {
         return this.index.getError();
      } else if (this.errorType == Indexer.ErrorType.TYPE) {
         return this.baseType.render() + " típusnak " + this.index.getType().render() + " típusú \"[ ]\" operátoron keresztül nem lehet értéket adni.";
      } else {
         return this.errorType == Indexer.ErrorType.NEXT ? this.next.getError() : null;
      }
   }

   Type getType() {
      return this.next == null ? null : this.next.getType();
   }

   private static enum ErrorType {
      NONE,
      BRACKET,
      TYPE,
      NEXT,
      BASE,
      INDEX;
   }
}
