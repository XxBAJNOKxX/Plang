package hu.ppke.itk.plang.prog;

public abstract class Type {
   static Type parseType(Lexer lex, Environment env) {
      Type t = null;
      if (lex.isKeyword("egesz")) {
         t = BasicType.INTEGER;
      }

      if (lex.isKeyword("valos")) {
         t = BasicType.REAL;
      }

      if (lex.isKeyword("szoveg")) {
         t = BasicType.STRING;
      }

      if (lex.isKeyword("logikai")) {
         t = BasicType.BOOLEAN;
      }

      if (lex.isKeyword("karakter")) {
         t = BasicType.CHARACTER;
      }

      if (t != null) {
         lex.next();
         return (Type)(lex.isKeyword("[") ? ArrayType.parseArrayType(t, lex, env) : t);
      } else if (lex.isKeyword("befajl")) {
         lex.next();
         return FileType.INPUT;
      } else if (lex.isKeyword("kifajl")) {
         lex.next();
         return FileType.OUTPUT;
      } else {
         return null;
      }
   }

   public String render(Object ob) {
      return this.toString(ob);
   }

   public String toString(Object ob) {
      return ob.toString();
   }

   abstract String render();

   abstract Object initVal();

   abstract boolean canCopy(Type var1);

   abstract Object copy(Object var1);

   abstract Type operatorType(UnaryOperator var1);

   abstract Object apply(UnaryOperator var1, Object var2);

   abstract boolean hasAccessor(UnaryOperator var1);

   abstract Object access(UnaryOperator var1, Object var2, Object var3);

   abstract Type operatorType(BinaryOperator var1, Type var2);

   abstract Object apply(BinaryOperator var1, Type var2, Object var3, Object var4);

   abstract boolean hasAccessor(BinaryOperator var1, Type var2);

   abstract Object access(BinaryOperator var1, Object var2, Object var3, Object var4);

   abstract Object readData(StreamData var1, StreamState var2) throws StreamData.DataError;

   void printData(StreamData str, StreamState sst, Object val) {
      str.append(sst, val.toString());
   }
}
