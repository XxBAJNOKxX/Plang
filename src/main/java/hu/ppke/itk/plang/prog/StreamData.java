package hu.ppke.itk.plang.prog;

public class StreamData {
   private StringBuffer content;
   private StreamKind kind;

   public StreamData(StreamKind kind) {
      this.kind = kind;
      this.content = new StringBuffer();
   }

   void addContent(String data) {
      this.content.append(data);
   }

   public StreamKind getKind() {
      return this.kind;
   }

   public void append(StreamState st, String str) {
      if (st.getPtr() < this.content.length()) {
         st.advance(this.content.length() - st.getPtr());
      }

      this.content.append(str);
      st.advance(str.length());
   }

   public String getSection(int start, int end) {
      return this.content.substring(start, end);
   }

   public int getLength() {
      return this.content.length();
   }

   private void skipWS(StreamState sst) {
      while(true) {
         if (sst.getPtr() < this.content.length()) {
            char c = this.content.charAt(sst.getPtr());
            if (c == ' ' || c == '\n' || c == '\t') {
               sst.advance(1);
               continue;
            }
         }

         return;
      }
   }

   private String getNumber(StreamState sst) throws DataError {
      if (sst.getPtr() < this.content.length() && this.content.charAt(sst.getPtr()) >= '0' && this.content.charAt(sst.getPtr()) <= '9') {
         String num = "";

         while(sst.getPtr() < this.content.length()) {
            char c = this.content.charAt(sst.getPtr());
            if (c < '0' || c > '9') {
               break;
            }

            num = num + c;
            sst.advance(1);
         }

         return num;
      } else {
         throw new DataError();
      }
   }

   int readInteger(StreamState sst) throws DataError {
      String num = "";
      this.skipWS(sst);
      if (sst.getPtr() < this.content.length() && this.content.charAt(sst.getPtr()) == '-') {
         num = "-";
         sst.advance(1);
      }

      num = num + this.getNumber(sst);
      return Integer.valueOf(num);
   }

   double readReal(StreamState sst) throws DataError {
      String num = "";
      this.skipWS(sst);
      if (sst.getPtr() < this.content.length() && this.content.charAt(sst.getPtr()) == '-') {
         num = "-";
         sst.advance(1);
      }

      num = num + this.getNumber(sst);
      if (sst.getPtr() < this.content.length() && this.content.charAt(sst.getPtr()) == '.') {
         num = num + ".";
         sst.advance(1);
         num = num + this.getNumber(sst);
      }

      return Double.valueOf(num);
   }

   String readString(StreamState sst) throws DataError {
      if (sst.getPtr() >= this.content.length()) {
         throw new DataError();
      } else {
         StringBuffer s = new StringBuffer("");

         while(sst.getPtr() < this.content.length()) {
            char c = this.content.charAt(sst.getPtr());
            sst.advance(1);
            if (c == '\n') {
               break;
            }

            s.append(c);
         }

         return s.toString();
      }
   }

   Object readChar(StreamState sst) throws DataError {
      if (sst.getPtr() >= this.content.length()) {
         throw new DataError();
      } else {
         char c = this.content.charAt(sst.getPtr());
         sst.advance(1);
         return c;
      }
   }

   boolean readBoolean(StreamState sst) throws DataError {
      this.skipWS(sst);
      if (sst.getPtr() >= this.content.length()) {
         throw new DataError();
      } else {
         char c = this.content.charAt(sst.getPtr());
         switch (c) {
            case 'H':
            case 'N':
            case 'h':
            case 'n':
               sst.advance(1);
               return false;
            case 'I':
            case 'Y':
            case 'i':
            case 'y':
               sst.advance(1);
               return true;
            default:
               throw new DataError();
         }
      }
   }

   public static class DataError extends Exception {
   }
}
