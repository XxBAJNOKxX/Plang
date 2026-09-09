package hu.ppke.itk.plang.prog;

enum BinaryOperator {
   PLUS("+"),
   MINUS("-"),
   STAR("*"),
   SLASH("/"),
   CIRCFLX("^"),
   AT("@"),
   LESS("<") {
      String render(Expression a, Expression b) {
         return a.render() + " &lt; " + b.render();
      }
   },
   EQUALS("="),
   GREATER(">"),
   SLASHEQ("/="),
   LESSEQ("<=") {
      String render(Expression a, Expression b) {
         return a.render() + " &lt;= " + b.render();
      }
   },
   GREATEQ(">="),
   AND("ÉS"),
   OR("VAGY"),
   DIV("DIV"),
   MOD("MOD"),
   BRACKET("[ ]") {
      String render(String a, String b) {
         return a + "[" + b + "]";
      }
   };

   private final String op;

   private BinaryOperator(String op) {
      this.op = op;
   }

   String render(Expression a, Expression b) {
      return this.render(a.render(), b.render());
   }

   String render(String a, String b) {
      return a + " " + this.op + " " + b;
   }

   public String toString() {
      return this.op;
   }

   // $FF: synthetic method
   BinaryOperator(String var3, BinaryOperator var4) {
      this(var3);
   }
}
