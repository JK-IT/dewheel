package desoft.studio.dewheel.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import desoft.studio.dewheel.SubKlass.Konverters

@Database(entities = [Kuser::class, Kevent::class, Ksaved::class], version=9, exportSchema = true)
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

        private var mig8_9 = object: Migration(8,9){
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE Ksaved ADD COLUMN saved_area TEXT DEFAULT NULL");
                database.execSQL("ALTER TABLE Ksaved ADD COLUMN saved_admin1 TEXT DEFAULT NULL");
            }
        }

        fun GetdbINS(ctx : Context) : Klocalbase
        {
            return INSTANCE ?: synchronized(this) {
                var temp = Room.databaseBuilder(ctx.applicationContext, Klocalbase::class.java, "Wheel Local Database")
                    .addMigrations(mig7_8, mig8_9)
                    .fallbackToDestructiveMigration().build();
                INSTANCE = temp;
                temp;
            }
        }
    }
}