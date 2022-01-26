package desoft.studio.dewheel.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import desoft.studio.dewheel.SubKlass.Konverters

@Database(entities = [Kuser::class, Kevent::class, Ksaved::class], version=10, exportSchema = true)
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
        
        private var mig9_10 = object: Migration(9, 10){
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS temp_kevent (id INTEGER PRIMARY KEY NOT NULL, event_name TEXT NOT NULL, event_description TEXT DEFAULT NULL, event_time TEXT DEFAULT NULL, event_time_inmilli INTEGER DEFAULT NULL, event_location TEXT NOT NULL, event_lati REAL NOT NULL, event_longi REAL NOT NULL, event_locality TEXT DEFAULT NULL, event_sub_locality TEXT DEFAULT NULL, event_admin1 TEXT DEFAULT NULL, event_zip_code TEXT DEFAULT NULL, event_country TEXT DEFAULT NULL)");
                database.execSQL("INSERT INTO temp_kevent(id, event_name, event_description, event_time, event_time_inmilli, event_location, event_lati, event_longi, event_locality, event_sub_locality, event_admin1, event_zip_code, event_country) SELECT id, event_name, event_description, event_time, event_time_inmilli, event_location, event_lati, event_longi, event_locality, event_sub_locality, event_admin1, event_zip_code, event_country FROM Kevent");
                database.execSQL("DROP TABLE Kevent");
                database.execSQL("ALTER TABLE temp_kevent RENAME TO Kevent");
            }
        }

        fun GetdbINS(ctx : Context) : Klocalbase
        {
            return INSTANCE ?: synchronized(this) {
                var temp = Room.databaseBuilder(ctx.applicationContext, Klocalbase::class.java, "Wheel Local Database")
                    .addMigrations(mig7_8, mig8_9, mig9_10)
                    .fallbackToDestructiveMigration().build();
                INSTANCE = temp;
                temp;
            }
        }
    }
}