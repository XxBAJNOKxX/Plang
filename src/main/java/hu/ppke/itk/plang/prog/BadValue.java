package hu.ppke.itk.plang.prog;

public class BadValue {
   private String message;

   BadValue(String msg) {
      this.message = msg;
   }

   public String toString() {
      return this.message;
   }
}
