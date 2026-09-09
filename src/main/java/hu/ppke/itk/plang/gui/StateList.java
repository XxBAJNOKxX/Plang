package hu.ppke.itk.plang.gui;

import hu.ppke.itk.plang.prog.BadValue;
import hu.ppke.itk.plang.prog.State;
import hu.ppke.itk.plang.prog.StreamState;
import hu.ppke.itk.plang.prog.Type;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import javax.swing.table.AbstractTableModel;

public class StateList extends AbstractTableModel {
   private Vector<State> states;
   private Vector<String> names;
   private Vector<Type> types;

   public int getColumnCount() {
      return this.names != null ? this.names.size() + 1 : 0;
   }

   public String getColumnName(int col) {
      if (col == 0) {
         return "LÉPÉS";
      } else {
         return this.names != null ? (String)this.names.elementAt(col - 1) : "";
      }
   }

   public int getRowCount() {
      return this.states != null ? this.states.size() : 0;
   }

   public Object getValueAt(int rowIndex, int columnIndex) {
      if (this.states != null) {
         if (columnIndex == 0) {
            return rowIndex + 1;
         } else {
            Object val = ((State)this.states.elementAt(rowIndex)).getVar((String)this.names.elementAt(columnIndex - 1));
            if (val == null) {
               return "<html><font color=\"blue\">???";
            } else {
               return val instanceof BadValue ? "<html><font color=\"red\">###" : ((Type)this.types.get(columnIndex - 1)).render(val);
            }
         }
      } else {
         return null;
      }
   }

   void setStates(List<State> stl) {
      if (this.states != null) {
         int l = this.states.size();
         this.states = null;
         this.names = null;
         this.types = null;
         this.fireTableRowsDeleted(0, l - 1);
         this.fireTableStructureChanged();
      }

      if (stl != null) {
         this.states = new Vector(stl);
         SortedSet<String> ns = new TreeSet(((State)this.states.get(0)).getVarNames());
         this.names = new Vector(ns);
         this.types = new Vector(this.names.size());

         for(String var : this.names) {
            this.types.add(((State)this.states.get(0)).getVarType(var));
         }

         this.fireTableRowsInserted(0, this.states.size() - 1);
         this.fireTableStructureChanged();
      }

   }

   public StreamState getStreamState(String name, int row) {
      return ((State)this.states.elementAt(row)).getStreamState(name);
   }

   public State getState(int row) {
      return row >= 0 && row < this.states.size() ? (State)this.states.elementAt(row) : null;
   }
}
