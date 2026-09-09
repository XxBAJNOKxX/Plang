package hu.ppke.itk.plang.prog;

import hu.ppke.itk.plang.gui.ProgramLine;
import java.util.List;
import java.util.Vector;

abstract class Statement {
   private Statement next;
   private int line;

   Statement getNext() {
      return this.next;
   }

   void setNext(Statement next) {
      this.next = next;
   }

   abstract List<ProgramLine> getLines(int var1);

   protected static final List<ProgramLine> oneLine(ProgramLine l) {
      Vector<ProgramLine> v = new Vector(1);
      v.add(l);
      return v;
   }

   abstract State execute(State var1);

   abstract boolean hasError();

   public void setLineIndex(int line) {
      this.line = line;
   }

   public int getLineIndex() {
      return this.line;
   }
}
