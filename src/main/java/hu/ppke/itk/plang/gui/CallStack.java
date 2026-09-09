package hu.ppke.itk.plang.gui;

import hu.ppke.itk.plang.prog.MainProgram;
import hu.ppke.itk.plang.prog.State;
import hu.ppke.itk.plang.prog.StreamData;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractListModel;

final class CallStack extends AbstractListModel {
   private final StateList stateList;
   private int maxSteps;
   private LinkedList<StackEntry> stack;

   CallStack(StateList stateList, int maxSteps) {
      this.stateList = stateList;
      this.maxSteps = maxSteps;
      this.stack = new LinkedList();
   }

   State runProgram(MainProgram prog, Map<String, String> input, Map<String, StreamData> output) {
      int s = this.stack.size();
      this.stack.clear();
      this.fireIntervalRemoved(this, 0, s);
      if (prog == null) {
         this.stateList.setStates((List)null);
         return null;
      } else {
         List<State> states = prog.runProgram(input, this.maxSteps);
         this.stack.add(new StackEntry("FŐPROGRAM", states));
         this.fireIntervalAdded(this, 0, 1);
         this.stateList.setStates(states);
         State last = (State)states.get(states.size() - 1);

         for(String stream : last.getStreamNames()) {
            output.put(stream, last.getStream(stream));
         }

         return last;
      }
   }

   void enter(String expr, List<State> subProg) {
      this.stack.add(new StackEntry(expr, subProg));
      this.stateList.setStates(subProg);
      this.fireIntervalAdded(this, this.stack.size() - 1, this.stack.size());
   }

   void leave() {
      if (this.stack.size() > 1) {
         this.stack.removeLast();
         this.stateList.setStates(((StackEntry)this.stack.getLast()).states);
         this.fireIntervalRemoved(this, this.stack.size(), this.stack.size() + 1);
      }

   }

   public int getSize() {
      return this.stack.size();
   }

   public Object getElementAt(int index) {
      return ((StackEntry)this.stack.get(index)).expr;
   }

   void setMaxSteps(int steps) {
      this.maxSteps = steps;
   }

   int getMaxSteps() {
      return this.maxSteps;
   }

   private class StackEntry {
      String expr;
      List<State> states;

      StackEntry(String e, List<State> s) {
         this.expr = e;
         this.states = s;
      }
   }
}
