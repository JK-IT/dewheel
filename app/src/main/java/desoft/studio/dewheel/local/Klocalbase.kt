package desoft.studio.dewheel.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import desoft.studio.dewheel.SubKlass.Konverters

@Database(entities = [Kuser::class, Kevent::class, Ksaved::class], version=8, exportSchema = true)
@TypeConverters(Konverters::class)
abstract class Klocalbase : RoomDatabase() {
    abstract fun kablesDao(): KablesDao;

    companion object
    {
        @Volatile
        private var INSTANCE: Klocalbase? = null;

        private var mig7_8 = object : Migration(7, 8){
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS Ksaved (saved_id TEXT PRIMARY KEY NOT NULL, fire_evnt TEXT)");
            }
        }

        fun GetdbINS(ctx : Context) : Klocalbase
        {
            return INSTANCE ?: synchronized(this) {
                var temp = Room.databaseBuilder(ctx.applicationContext, Klocalbase::class.java, "Wheel Local Database")
                    .addMigrations(mig7_8)
                    .fallbackToDestructiveMigration().build();
                INSTANCE = temp;
                temp;
            }
        }
    }
}