package hu.ppke.itk.plang.android;

import android.app.Application;

/**
 * Alkalmazás-osztály: a beállítások inicializálása és a mentett téma
 * betöltése már az első nézet létrehozása előtt.
 */
public class PlangApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppPrefs.init(this);
        Theme.setMode(AppPrefs.getThemeMode());
    }
}
