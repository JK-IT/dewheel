package desoft.studio.dewheel.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Kuser::class, Kevent::class], version=7, exportSchema = false)
abstract class Klocalbase : RoomDatabase() {
    abstract fun kablesDao(): KablesDao;

    companion object
    {
        @Volatile
        private var INSTANCE: Klocalbase? = null;

        fun GetdbINS(ctx : Context) : Klocalbase
        {
            return INSTANCE ?: synchronized(this) {
                var temp = Room.databaseBuilder(ctx.applicationContext, Klocalbase::class.java, "Wheel Local Database")
                    .fallbackToDestructiveMigration().build();
                INSTANCE = temp;
                temp;
            }
        }
    }
}