package desoft.studio.dewheel

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import desoft.studio.dewheel.Kontrol.RepoWheel
import desoft.studio.dewheel.local.Klocalbase
import java.util.concurrent.BlockingQueue
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class Wapplication : Application() {

    val localbase by lazy { Klocalbase.GetdbINS(this) };
    // repo will have instance of database to get dao
    val repo by lazy { RepoWheel(localbase) };

    private val corenum = Runtime.getRuntime().availableProcessors();
    private val workqueue : BlockingQueue<Runnable> = SynchronousQueue<Runnable>();
    private val KEEP_ALIVE_TIME = 1L;
    private val TIME_UNIT = TimeUnit.SECONDS;

    val threadpoolExecutor : ThreadPoolExecutor = ThreadPoolExecutor(corenum, Int.MAX_VALUE, KEEP_ALIVE_TIME, TIME_UNIT, workqueue);

    override fun onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}