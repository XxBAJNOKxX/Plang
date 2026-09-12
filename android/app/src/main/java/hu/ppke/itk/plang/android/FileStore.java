package hu.ppke.itk.plang.android;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Fájlműveletek a Storage Access Frameworkön keresztül – az asztali
 * ISO-8859-2 betöltés/mentés tükörmása tartalmi URI-kkal.
 */
public final class FileStore {

    public static final Charset CHARSET = Charset.forName("ISO-8859-2");

    private FileStore() {}

    /**
     * A fájlnév kiterjesztésének biztosítása (az ensurePlangExtension portja).
     */
    public static String ensurePlangExtension(String displayName) {
        if (displayName == null) {
            return "nevtelen.plang";
        }
        int dot = displayName.lastIndexOf('.');
        if (dot < 0) {
            return displayName + ".plang";
        }
        if (dot == displayName.length() - 1) {
            return displayName + "plang";
        }
        return displayName;
    }

    /**
     * Betölti a megadott tartalmi URI-t ISO-8859-2 kódolással.
     */
    public static String load(Context ctx, Uri uri) throws IOException {
        ContentResolver cr = ctx.getContentResolver();
        InputStream in = cr.openInputStream(uri);
        if (in == null) {
            throw new IOException("Nem nyitható meg: " + uri);
        }
        BufferedReader rd = new BufferedReader(new InputStreamReader(in, CHARSET));
        StringBuilder sb = new StringBuilder();
        try {
            for (String line = rd.readLine(); line != null; line = rd.readLine()) {
                sb.append(line).append("\n");
            }
        } finally {
            rd.close();
        }
        return sb.toString();
    }

    /**
     * A program mentése ISO-8859-2 kódolással.
     */
    public static void save(Context ctx, Uri uri, String text) throws IOException {
        ContentResolver cr = ctx.getContentResolver();
        OutputStream out = cr.openOutputStream(uri, "wt");
        if (out == null) {
            throw new IOException("Nem írható: " + uri);
        }
        Writer wr = new OutputStreamWriter(out, CHARSET);
        try {
            wr.write(text);
            wr.flush();
        } finally {
            wr.close();
        }
    }

    /** Emberi olvasatra való fájlnév egy tartalmi URI-ból. */
    public static String fileName(Context ctx, Uri uri) {
        String name = uri.getLastPathSegment();
        if (name != null && name.contains("/")) {
            name = name.substring(name.lastIndexOf('/') + 1);
        }
        return name == null ? "névtelen.plang" : name;
    }

    /* ---- legutóbbi fájlok ---- */

    public static List<Uri> getRecent(Context ctx) {
        List<Uri> out = new ArrayList<Uri>();
        for (String s : AppPrefs.getRecentFiles()) {
            try {
                Uri uri = Uri.parse(s);
                if (uri != null) {
                    out.add(uri);
                }
            } catch (Exception e) {
                // sérült bejegyzés: kihagyjuk
            }
        }
        return out;
    }

    public static void addRecent(Context ctx, Uri uri) {
        if (uri == null) {
            return;
        }
        List<String> list = AppPrefs.getRecentFiles();
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            if (it.next().equals(uri.toString())) {
                it.remove();
            }
        }
        list.add(0, uri.toString());
        while (list.size() > 8) {
            list.remove(list.size() - 1);
        }
        AppPrefs.setRecentFiles(list);
        // tartalom-megőrzési engedély kérése, hogy újraindítás után is olvasható legyen
        try {
            ctx.getContentResolver().takePersistableUriPermission(uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (Exception e) {
            // nem minden szolgáltató támogatja
        }
    }

    public static void clearRecent() {
        AppPrefs.setRecentFiles(new ArrayList<String>());
    }
}
