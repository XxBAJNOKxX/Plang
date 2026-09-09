package hu.ppke.itk.plang.prog;

enum UnaryOperator {
   MINUS("-"),
   SIN("SIN"),
   COS("COS"),
   TAN("TAN"),
   ARCSIN("ARCSIN"),
   ARCCOS("ARCCOS"),
   ARCTAN("ARCTAN"),
   LOG("LOG"),
   NOT("NEM"),
   RND("RND"),
   EXP("EXP"),
   TRUNC("EGÉSZ"),
   ROUND("KEREK"),
   REAL("VALÓS"),
   UPPER("NAGY"),
   LOWER("KIS"),
   ISNUM("SZÁM"),
   ISALPHA("BETŰ"),
   PIPE("| |") {
      String render(String e) {
         return "|" + e + "|";
      }
   };

   protected final String op;

   private UnaryOperator(String op) {
      this.op = op;
   }

   final String render(Expression e) {
      return this.render(e.render());
   }

   String render(String e) {
      return this.op + (this.op.length() > 1 ? " " : "") + e.toString();
   }

   public String toString() {
      return this.op;
   }

   // $FF: synthetic method
   UnaryOperator(String var3, UnaryOperator var4) {
      this(var3);
   }
}
