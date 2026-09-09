package hu.ppke.itk.plang.prog;

public class StreamState {
   private int ptr;
   private int lastSec;
   private boolean eof;

   StreamState() {
      this(0, 0, false);
   }

   private StreamState(int ptr, int lastSec, boolean eof) {
      this.ptr = ptr;
      this.lastSec = lastSec;
      this.eof = eof;
   }

   public int getPtr() {
      return this.ptr;
   }

   public int getLastSec() {
      return this.lastSec;
   }

   StreamState newSection() {
      return new StreamState(this.ptr, 0, this.eof);
   }

   void advance(int len) {
      this.ptr += len;
      this.lastSec += len;
   }

   void advanceTo(int pos) {
      if (pos > this.ptr) {
         this.lastSec += pos - this.ptr;
         this.ptr = pos;
      }

   }

   public String toString() {
      return "[" + (this.ptr - this.lastSec) + ".." + this.ptr + "]";
   }

   void setEof() {
      this.eof = true;
   }

   boolean isEof() {
      return this.eof;
   }
}
