package hu.ppke.itk.plang.prog;

import hu.ppke.itk.plang.gui.ExprNode;

abstract class Accessor {
   static Accessor parseAccessors(Lexer lex, Environment env, Type baseType) {
      return (Accessor)(lex.isKeyword("[") ? Indexer.parseIndexer(lex, env, baseType) : new Assigner(baseType));
   }

   abstract Type getType();

   abstract Object access(State var1, Object var2, Object var3);

   abstract ExprNode getTree(State var1, String var2, ExprNode var3);

   abstract String render();

   abstract String getError();

   private static class Assigner extends Accessor {
      private Type type;

      Assigner(Type type) {
         this.type = type;
      }

      public Object access(State state, Object oldValue, Object newValue) {
         return this.type.copy(newValue);
      }

      ExprNode getTree(State state, String pref, ExprNode subExpr) {
         return subExpr;
      }

      Type getType() {
         return this.type;
      }

      public String render() {
         return "";
      }

      public String toString() {
         return "";
      }

      String getError() {
         return null;
      }
   }
}
