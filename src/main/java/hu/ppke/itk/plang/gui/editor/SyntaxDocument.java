package hu.ppke.itk.plang.gui.editor;

import java.awt.Color;
import java.util.List;

import javax.swing.event.DocumentEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import hu.ppke.itk.plang.gui.theme.Theme;

/**
 * Szintaxist kiemelő dokumentum a PLanG forráskódhoz.
 *
 * A kiemelés soronként történik: minden szerkesztés után csak az érintett
 * sorokat színezzük újra, így nagy programoknál sem lassul be a gépelés.
 */
public class SyntaxDocument extends DefaultStyledDocument {

   private static final long serialVersionUID = 1L;

   private SimpleAttributeSet[] styles = new SimpleAttributeSet[11];
   private boolean highlighting;

   public SyntaxDocument() {
      buildStyles();
   }

   /** A téma váltásakor újraépíti a stílusokat és újraszínez mindent. */
   public void refreshTheme() {
      buildStyles();
      highlightAll();
   }

   private void buildStyles() {
      Theme.Palette p = Theme.p();
      styles[PlangSyntax.PLAIN] = style(p.editorFg, false, false);
      styles[PlangSyntax.KW] = style(p.synKeyword, false, false);
      styles[PlangSyntax.CTRL] = style(p.synControl, false, false);
      styles[PlangSyntax.TYPE_T] = style(p.synType, false, false);
      styles[PlangSyntax.STRING] = style(p.synString, false, false);
      styles[PlangSyntax.NUMBER] = style(p.synNumber, false, false);
      styles[PlangSyntax.COMMENT] = style(p.synComment, false, true);
      styles[PlangSyntax.FUNC] = style(p.synFunction, false, false);
      styles[PlangSyntax.IDENT] = style(p.synVariable, false, false);
      styles[PlangSyntax.OPERATOR] = style(p.synOperator, false, false);
      styles[PlangSyntax.CONST] = style(p.synConstant, false, false);
   }

   private SimpleAttributeSet style(Color fg, boolean bold, boolean italic) {
      SimpleAttributeSet a = new SimpleAttributeSet();
      StyleConstants.setForeground(a, fg);
      StyleConstants.setBold(a, bold);
      StyleConstants.setItalic(a, italic);
      return a;
   }

   public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
      super.insertString(offs, str, a);
      highlightAround(offs, str == null ? 0 : str.length());
   }

   public void remove(int offs, int len) throws BadLocationException {
      super.remove(offs, len);
      highlightAround(offs, 0);
   }

   protected void fireInsertUpdate(DocumentEvent e) {
      super.fireInsertUpdate(e);
   }

   /** A teljes dokumentum újraszínezése. */
   public void highlightAll() {
      highlightRange(0, getLength());
   }

   private void highlightAround(int offset, int length) {
      Element root = getDefaultRootElement();
      int startLine = root.getElementIndex(Math.max(0, offset));
      int endLine = root.getElementIndex(Math.min(getLength(), offset + length));
      Element se = root.getElement(Math.max(0, startLine));
      Element ee = root.getElement(Math.max(0, endLine));
      if (se == null || ee == null) {
         return;
      }
      highlightRange(se.getStartOffset(), ee.getEndOffset());
   }

   private void highlightRange(int from, int to) {
      if (highlighting) {
         return;
      }
      highlighting = true;
      try {
         Element root = getDefaultRootElement();
         int first = root.getElementIndex(from);
         int last = root.getElementIndex(Math.max(from, Math.min(to, getLength())));

         for (int i = first; i <= last && i < root.getElementCount(); i++) {
            Element line = root.getElement(i);
            int s = line.getStartOffset();
            int e = Math.min(line.getEndOffset(), getLength());
            if (e <= s) {
               continue;
            }
            String text = getText(s, e - s);
            setCharacterAttributes(s, e - s, styles[PlangSyntax.PLAIN], true);
            List<PlangSyntax.Token> toks = PlangSyntax.tokenize(text);
            for (int t = 0; t < toks.size(); t++) {
               PlangSyntax.Token tk = toks.get(t);
               MutableAttributeSet st = styles[tk.kind];
               if (st != null) {
                  setCharacterAttributes(s + tk.start, tk.end - tk.start, st, true);
               }
            }
         }
      } catch (BadLocationException ex) {
         // a szerkesztés közbeni versenyhelyzet nem végzetes
      } finally {
         highlighting = false;
      }
   }
}
