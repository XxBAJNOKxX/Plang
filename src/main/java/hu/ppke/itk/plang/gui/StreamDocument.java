package hu.ppke.itk.plang.gui;

import java.awt.Color;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import hu.ppke.itk.plang.gui.theme.Theme;
import hu.ppke.itk.plang.prog.StreamData;
import hu.ppke.itk.plang.prog.StreamKind;
import hu.ppke.itk.plang.prog.StreamState;

/**
 * A be- és kimeneti csatornák tartalmát tároló dokumentum.
 *
 * A már feldolgozott (illetve még ki nem írt) rész halványan, az éppen
 * beolvasott/kiírt szakasz pedig kiemelve jelenik meg – a színek most a
 * témából származnak, így sötét háttéren is olvashatók maradnak.
 */
public class StreamDocument extends DefaultStyledDocument {

   private static final long serialVersionUID = 1L;

   private StreamKind kind;
   private int length;
   private int start;

   private static SimpleAttributeSet normal = new SimpleAttributeSet();
   private static SimpleAttributeSet current = new SimpleAttributeSet();
   private static SimpleAttributeSet hidden = new SimpleAttributeSet();

   static {
      refreshStyles();
   }

   /** A téma váltása után újraszámolja a stílusok színeit. */
   public static void refreshStyles() {
      Theme.Palette p = Theme.p();
      StyleConstants.setForeground(normal, p.editorFg);
      StyleConstants.setBold(normal, false);
      StyleConstants.setBackground(normal, p.editorBg);

      StyleConstants.setBold(current, true);
      StyleConstants.setForeground(current, p.editorFg);
      StyleConstants.setBackground(current, Theme.isDark()
                                            ? new Color(0x26, 0x4F, 0x78)
                                            : new Color(0xAD, 0xD6, 0xFF));

      StyleConstants.setForeground(hidden, p.gutterFg);
      StyleConstants.setBold(hidden, false);
      StyleConstants.setBackground(hidden, p.editorBg);
   }

   public StreamDocument() {
      this.kind = StreamKind.INPUT;
      this.length = 0;
      this.start = 0;
   }

   public static SimpleAttributeSet getNormalAttr() {
      return normal;
   }

   public String getText() {
      try {
         return this.getText(0, this.getLength());
      } catch (BadLocationException e) {
         e.printStackTrace();
         return null;
      }
   }

   public void setStream(StreamData stream) {
      try {
         this.remove(0, this.getLength());
         if (stream != null) {
            this.insertString(0, stream.getSection(0, stream.getLength()), (AttributeSet) null);
            this.kind = stream.getKind();
         }
      } catch (BadLocationException e) {
         throw new RuntimeException("Stream index error", e);
      }
   }

   public void setState(StreamState state) {
      if (state == null) {
         this.setCharacterAttributes(0, this.getLength(), normal, true);
      } else {
         this.length = state.getLastSec();
         this.start = state.getPtr() - this.length;
         this.setCharacterAttributes(0, this.start,
            this.kind == StreamKind.INPUT ? hidden : normal, true);
         this.setCharacterAttributes(this.start, this.length, current, true);
         this.setCharacterAttributes(this.start + this.length,
            this.getLength() - this.start - this.length,
            this.kind == StreamKind.OUTPUT ? hidden : normal, true);
      }
   }

   /** Újraszínezi a dokumentumot az aktuális témával. */
   public void reapplyTheme() {
      refreshStyles();
      setCharacterAttributes(0, getLength(), normal, true);
   }
}
