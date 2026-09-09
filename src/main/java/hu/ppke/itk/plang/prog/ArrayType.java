package hu.ppke.itk.plang.prog;

import hu.ppke.itk.plang.gui.ProgramLine;
import java.util.Vector;

class ArrayType extends Type {
   private Type baseType;
   private int bound;
   private ErrorType errorType;

   static ArrayType parseArrayType(Type baseType, Lexer lex, Environment env) {
      lex.next();
      if (lex.getType() == Lexer.Token.NUMBER && lex.getNumber() == (double)((int)lex.getNumber())) {
         int bound = (int)lex.getNumber();
         lex.next();
         if (lex.isKeyword("]")) {
            lex.next();
            return new ArrayType((Type)(lex.isKeyword("[") ? parseArrayType(baseType, lex, env) : baseType), bound, ArrayType.ErrorType.NONE);
         } else {
            return new ArrayType(baseType, bound, ArrayType.ErrorType.BRACKET);
         }
      } else {
         return new ArrayType(baseType, 0, ArrayType.ErrorType.SIZE);
      }
   }

   private ArrayType(Type baseType, int bound, ErrorType errorType) {
      this.baseType = baseType;
      this.bound = bound;
      this.errorType = errorType;
   }

   String render() {
      return this.render("");
   }

   public String toString(Object obj) {
      Vector<Object> v = (Vector)obj;
      String img = "[";
      String sep = "";

      for(Object val : v) {
         if (val == null) {
            img = img + sep + "??";
         } else if (val instanceof BadValue) {
            img = img + sep + "##";
         } else {
            img = img + sep + this.baseType.render(val);
         }

         sep = ", ";
      }

      return img + "]";
   }

   private String render(String bounds) {
      if (this.errorType == ArrayType.ErrorType.SIZE) {
         bounds = bounds + "[" + ProgramLine.bad("???");
      } else if (this.errorType == ArrayType.ErrorType.BRACKET) {
         bounds = bounds + "[" + this.bound + ProgramLine.bad("]");
      } else {
         bounds = bounds + "[" + this.bound + "]";
      }

      return this.baseType instanceof ArrayType ? ((ArrayType)this.baseType).render(bounds) : this.baseType.render() + bounds;
   }

   public String toString() {
      return this.toString("");
   }

   private String toString(String bounds) {
      return this.baseType instanceof ArrayType ? ((ArrayType)this.baseType).render("[" + this.bound + "]" + bounds) : this.baseType.render() + "[" + this.bound + "]" + bounds;
   }

   boolean canCopy(Type type) {
      if (type instanceof ArrayType) {
         ArrayType at = (ArrayType)type;
         return this.bound == at.bound && this.baseType.canCopy(at.baseType);
      } else {
         return false;
      }
   }

   Object copy(Object val) {
      Vector<Object> v = (Vector)val;
      Vector<Object> nv = new Vector(this.bound);

      for(Object ob : v) {
         nv.add(this.baseType.copy(ob));
      }

      return nv;
   }

   Object initVal() {
      Vector<Object> v = new Vector(this.bound);

      for(int i = 0; i < this.bound; ++i) {
         v.add(this.baseType.initVal());
      }

      return v;
   }

   Type operatorType(UnaryOperator op) {
      return op == UnaryOperator.PIPE ? BasicType.INTEGER : null;
   }

   Object apply(UnaryOperator op, Object val) {
      return op == UnaryOperator.PIPE ? ((Vector)val).size() : null;
   }

   Type operatorType(BinaryOperator op, Type rhs) {
      return op == BinaryOperator.BRACKET && rhs == BasicType.INTEGER ? this.baseType : null;
   }

   Object apply(BinaryOperator op, Type rhs, Object left, Object right) {
      if (op == BinaryOperator.BRACKET && rhs == BasicType.INTEGER) {
         if (left instanceof BadValue) {
            return left;
         } else if (right instanceof BadValue) {
            return right;
         } else {
            Vector<Object> l = (Vector)left;
            Integer r = (Integer)right;
            if (r >= 0 && r < l.size()) {
               Object val = ((Vector)left).get((Integer)right);
               return val == null ? new EmptyValue("A tömb " + r + ". eleme nem kapott kezdőértéket.") : val;
            } else {
               return new BadValue("Hibás tömbindex");
            }
         }
      } else {
         return null;
      }
   }

   Object readData(StreamData strm, StreamState sst) throws StreamData.DataError {
      return new BadValue("Tömb típusú adatot nem lehet beolvasni.");
   }

   boolean hasAccessor(UnaryOperator op) {
      return false;
   }

   Object access(UnaryOperator op, Object oldVal, Object newVal) {
      return null;
   }

   boolean hasAccessor(BinaryOperator op, Type rhs) {
      return op == BinaryOperator.BRACKET && rhs == BasicType.INTEGER;
   }

   Object access(BinaryOperator op, Object oldVal, Object rhs, Object newVal) {
      Vector<Object> v = (Vector)((Vector)oldVal).clone();
      v.set((Integer)rhs, newVal);
      return v;
   }

   private static enum ErrorType {
      NONE,
      BRACKET,
      SIZE;
   }
}
