package hu.ppke.itk.plang.gui;

import hu.ppke.itk.plang.prog.State;
import java.util.List;

public class ExprNode {
   private String result;
   private String error;
   private String expr;
   private ExprNode[] children;
   private List<State> subStates;
   public static final ExprNode EMPTY = new ExprNode((String)null, "---", (ExprNode[])null);

   public ExprNode(String expr, ExprNode[] children, String error) {
      this(expr, (ExprNode[])children, (List)null, error);
   }

   public ExprNode(String expr, ExprNode[] children, List<State> subStates, String error) {
      this.expr = expr;
      this.children = children;
      this.error = error;
      this.subStates = subStates;
   }

   public ExprNode(String result, String expr, ExprNode[] children) {
      this(result, (String)expr, children, (List)null);
   }

   public ExprNode(String result, String expr, ExprNode[] children, List<State> subStates) {
      this(expr, (ExprNode[])children, subStates, (String)null);
      this.result = result;
   }

   public final String toString() {
      return "<html>" + this.expr + (this.result != null ? "<b> = " + this.result + "</b>" : "");
   }

   String getError() {
      return this.error;
   }

   boolean hasError() {
      return this.error != null;
   }

   ExprNode getChild(int index) {
      return this.children[index];
   }

   int childNumber() {
      return this.children != null ? this.children.length : 0;
   }

   boolean isLeaf() {
      return this.children == null;
   }

   List<State> getSubStates() {
      return this.subStates;
   }
}
