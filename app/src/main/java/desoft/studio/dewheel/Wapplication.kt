package desoft.studio.dewheel

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import desoft.studio.dewheel.Kontrol.RepoWheel
import desoft.studio.dewheel.local.Klocalbase

class Wapplication : Application() {

    val localbase by lazy { Klocalbase.GetdbINS(this) };
    // repo will have instance of database to get dao
    val repo by lazy { RepoWheel(localbase) };

    override fun onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}