import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;

import hu.ppke.itk.plang.gui.*;
import hu.ppke.itk.plang.gui.theme.Theme;

public class Screenshot {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        System.setProperty("file.encoding", "UTF-8");
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {}

        for (int mode : new int[]{Theme.DARK, Theme.LIGHT}) {
            Theme.setMode(mode);
            String name = mode == Theme.DARK ? "dark" : "light";
            System.out.println("Generating " + name);

            Workbench wb = new Workbench(null);
            wb.setSize(1440, 900);
            wb.setPreferredSize(new Dimension(1440, 900));
            doLayoutRec(wb);

            // osztópanelek kézi beállítása (invokeLater helyett)
            try {
                JSplitPane mainSplit = (JSplitPane) getField(wb, "mainSplit");
                JSplitPane centerSplit = (JSplitPane) getField(wb, "centerSplit");
                JSplitPane rightSplit = (JSplitPane) getField(wb, "rightSplit");
                JSplitPane inspectSplit = (JSplitPane) getField(wb, "inspectSplit");
                JSplitPane consoleSplit = (JSplitPane) getField(wb, "consoleSplit");
                mainSplit.setDividerLocation(260);
                centerSplit.setDividerLocation(500);
                rightSplit.setDividerLocation(900);
                inspectSplit.setDividerLocation(300);
                consoleSplit.setDividerLocation(400);
            } catch (Exception e) {}

            // betöltünk egy példát, hogy legyen tartalom
            File ex = new File("examples/osszeadas.plang");
            if (!ex.exists()) ex = new File("/home/user/Plang/examples/osszeadas.plang");
            if (ex.exists()) {
                wb.openFile(ex);
                // értelmezzük
                try {
                    Action parse = (Action) getField(wb, "parseAction");
                    parse.actionPerformed(null);
                } catch (Exception e) {}
            }

            doLayoutRec(wb);
            // második layout, hogy a dividers érvényesüljenek
            doLayoutRec(wb);

            int w = 1440;
            int h = 900;
            JMenuBar mb = wb.getMenuBar();
            int mbH = 24;
            if (mb != null) {
                mb.setSize(w, mbH);
                doLayoutRec(mb);
                mbH = mb.getPreferredSize().height;
                if (mbH < 20) mbH = 24;
            }

            BufferedImage img = new BufferedImage(w, h + mbH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = img.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            // háttér
            g2.setColor(Theme.p().editorBg);
            g2.fillRect(0, 0, w, h + mbH);

            if (mb != null) {
                Graphics2D gMenu = (Graphics2D) g2.create(0, 0, w, mbH);
                mb.printAll(gMenu);
                gMenu.dispose();
            }

            Graphics2D gBody = (Graphics2D) g2.create(0, mbH, w, h);
            wb.printAll(gBody);
            gBody.dispose();
            g2.dispose();

            File out = new File("docs/screenshot-" + name + ".png");
            ImageIO.write(img, "png", out);
            System.out.println("Saved " + out.getAbsolutePath());
        }
    }

    static Object getField(Object obj, String name) throws Exception {
        Class<?> c = obj.getClass();
        while (c != null) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(obj);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    static void doLayoutRec(Component c) {
        try { c.doLayout(); } catch (Exception e) {}
        if (c instanceof Container) {
            Container cont = (Container) c;
            for (Component child : cont.getComponents()) {
                doLayoutRec(child);
            }
        }
        if (c instanceof JComponent) {
            try { ((JComponent) c).revalidate(); } catch (Exception e) {}
        }
    }
}
