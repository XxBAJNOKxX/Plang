package hu.ppke.itk.plang.gui;

import hu.ppke.itk.plang.prog.MainProgram;
import java.util.Vector;
import javax.swing.AbstractListModel;

public final class ProgramList extends AbstractListModel {
   Vector<ProgramLine> lines;
   private MainProgram program = null;
   private static final long serialVersionUID = -1585949598809115419L;

   public int getSize() {
      return this.lines == null ? 0 : this.lines.size();
   }

   public ProgramLine getElementAt(int index) {
      return (ProgramLine)this.lines.elementAt(index);
   }

   public void setProgram(MainProgram program) {
      if (this.lines != null && this.lines.size() > 0) {
         this.fireIntervalRemoved(this, 0, this.lines.size() - 1);
      }

      if (program != null) {
         this.lines = new Vector(program.getLines());
         int l = 0;

         for(ProgramLine line : this.lines) {
            line.setLine(l++);
         }

         if (this.lines.size() > 0) {
            this.fireIntervalAdded(this, 0, this.lines.size() - 1);
         }

         this.program = program;
      }

   }

   public MainProgram getProgram() {
      return this.program;
   }
}
