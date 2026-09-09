package hu.ppke.itk.plang.prog;

import hu.ppke.itk.plang.gui.ExprNode;
import hu.ppke.itk.plang.gui.ProgramLine;

class SubstringExpression extends Expression {
   private ErrorType errorType;
   private Expression base;
   private Expression begin;
   private Expression end;

   static SubstringExpression createSubstringExpr(Expression base, Expression begin, Expression end) {
      if (base.getError() != null) {
         return new SubstringExpression(base, begin, end, SubstringExpression.ErrorType.BASE);
      } else if (begin.getError() != null) {
         return new SubstringExpression(base, begin, end, SubstringExpression.ErrorType.BEGIN);
      } else if (end.getError() != null) {
         return new SubstringExpression(base, begin, end, SubstringExpression.ErrorType.END);
      } else if (base.getType() != BasicType.STRING) {
         return new SubstringExpression(base, begin, end, SubstringExpression.ErrorType.BASETYPE);
      } else if (begin.getType() != BasicType.INTEGER) {
         return new SubstringExpression(base, begin, end, SubstringExpression.ErrorType.BEGINTYPE);
      } else {
         return end.getType() != BasicType.INTEGER ? new SubstringExpression(base, begin, end, SubstringExpression.ErrorType.ENDTYPE) : new SubstringExpression(base, begin, end, SubstringExpression.ErrorType.NONE);
      }
   }

   private SubstringExpression(Expression base, Expression begin, Expression end, ErrorType errorType) {
      super(BasicType.STRING);
      this.base = base;
      this.begin = begin;
      this.end = end;
      this.errorType = errorType;
   }

   public String getError() {
      if (this.errorType == SubstringExpression.ErrorType.BASE) {
         return this.base.getError();
      } else if (this.errorType == SubstringExpression.ErrorType.BEGIN) {
         return this.begin.getError();
      } else if (this.errorType == SubstringExpression.ErrorType.END) {
         return this.end.getError();
      } else if (this.errorType == SubstringExpression.ErrorType.BASETYPE) {
         return "A [ : ] művelet csak a STRING típuson értelmezett.";
      } else {
         return this.errorType != SubstringExpression.ErrorType.BEGINTYPE && this.errorType != SubstringExpression.ErrorType.ENDTYPE ? null : "A [ : ] művelet paraméterei csak INTEGER típusúak lehetnek.";
      }
   }

   String render() {
      if (this.errorType == SubstringExpression.ErrorType.BASETYPE) {
         return ProgramLine.bad(this.base.render()) + "[" + this.begin.render() + ":" + this.end.render() + "]";
      } else if (this.errorType == SubstringExpression.ErrorType.BEGINTYPE) {
         return this.base.render() + "[" + ProgramLine.bad(this.begin.render()) + ":" + this.end.render() + "]";
      } else {
         return this.errorType == SubstringExpression.ErrorType.ENDTYPE ? this.base.render() + "[" + this.begin.render() + ":" + ProgramLine.bad(this.end.render()) + "]" : this.base.render() + "[" + this.begin.render() + ":" + this.end.render() + "]";
      }
   }

   public String toString() {
      return this.base + "[" + this.begin + ":" + this.end + "]";
   }

   Object getValue(State state) {
      Object baseval = this.base.getValue(state);
      if (baseval instanceof BadValue) {
         return baseval;
      } else {
         Object bval = this.begin.getValue(state);
         if (bval instanceof BadValue) {
            return bval;
         } else {
            Object eval = this.end.getValue(state);
            if (eval instanceof BadValue) {
               return eval;
            } else {
               int b = (Integer)bval;
               int e = (Integer)eval;
               if (b == e) {
                  return "";
               } else {
                  String s = (String)baseval;
                  if (b >= 0 && b < s.length()) {
                     if (e < b) {
                        return new BadValue("A kezdő index nagyobb végindexnél");
                     } else {
                        return e > s.length() ? new BadValue("Hibás végindex") : ((String)baseval).substring((Integer)bval, (Integer)eval);
                     }
                  } else {
                     return new BadValue("Hibás kezdő index");
                  }
               }
            }
         }
      }
   }

   ExprNode[] getChildren(State state) {
      return new ExprNode[]{this.base.getTree(state), this.begin.getTree(state), this.end.getTree(state)};
   }

   private static enum ErrorType {
      NONE,
      BASE,
      BEGIN,
      END,
      BASETYPE,
      BEGINTYPE,
      ENDTYPE;
   }
}
