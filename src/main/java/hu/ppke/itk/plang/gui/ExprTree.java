package hu.ppke.itk.plang.gui;

import java.util.LinkedList;
import java.util.List;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public class ExprTree implements TreeModel {
   ExprNode root = new ExprNode((String)null, "---", (ExprNode[])null);
   private List<TreeModelListener> listeners = new LinkedList();

   public void setRoot(ExprNode expr) {
      ExprNode oldRoot = this.root;
      if (expr != null) {
         this.root = expr;
      } else {
         this.root = ExprNode.EMPTY;
      }

      this.fireChange(oldRoot);
   }

   public Object getRoot() {
      return this.root;
   }

   public Object getChild(Object obj, int index) {
      return ((ExprNode)obj).getChild(index);
   }

   public int getChildCount(Object obj) {
      return ((ExprNode)obj).childNumber();
   }

   public boolean isLeaf(Object obj) {
      return ((ExprNode)obj).isLeaf();
   }

   public void valueForPathChanged(TreePath arg0, Object arg1) {
   }

   public int getIndexOfChild(Object obj, Object child) {
      ExprNode parent = (ExprNode)obj;

      for(int i = 0; i < parent.childNumber(); ++i) {
         if (parent.getChild(i) == child) {
            return i;
         }
      }

      return -1;
   }

   public void addTreeModelListener(TreeModelListener lst) {
      this.listeners.add(lst);
   }

   public void removeTreeModelListener(TreeModelListener lst) {
      this.listeners.remove(lst);
   }

   private void fireChange(ExprNode oldRoot) {
      TreeModelEvent ev = new TreeModelEvent(this, new Object[]{oldRoot});

      for(TreeModelListener l : this.listeners) {
         l.treeStructureChanged(ev);
      }

   }
}
