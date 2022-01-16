package desoft.studio.dewheel.local

import androidx.room.*

@Dao
interface KablesDao {

    // +    USER DAO --------->>-------->>--------->>*** -->>----------->>>>
    @Insert(entity = Kuser::class,onConflict = OnConflictStrategy.REPLACE)
    suspend fun InsertUser(vararg kus: Kuser);

    @Update(entity = Kuser::class, onConflict = OnConflictStrategy.REPLACE)
    suspend fun UpdateUser(vararg kus: Kuser);

    @Query("SELECT * FROM Kuser WHERE fuid = :uid")
    suspend fun FindUser(uid : String): Kuser?;

    @Query("DELETE FROM Kuser")
    suspend fun DeleteAllUser();

    @Delete(entity = Kuser::class)
    suspend fun DeleteUser(vararg kus:Kuser);

    // +    EVENT DAO --------->>-------->>--------->>*** -->>----------->>>>
    @Insert(entity = Kevent::class, onConflict = OnConflictStrategy.REPLACE)
    suspend fun InsertEvent(evnt: Kevent) : Long;

    @Update(entity = Kevent::class, onConflict = OnConflictStrategy.REPLACE)
    suspend fun UpdateEvent(evnt: Kevent);

    @Query("SELECT * FROM Kevent")
    suspend fun GetAllEvents(): List<Kevent>;

    @Query("SELECT * FROM Kevent WHERE id = :ineid")
    suspend fun GetEvent(ineid : Long) : Kevent;

    @Query("DELETE FROM Kevent")
    suspend fun DeleteAllEvent();

    @Delete(entity = Kevent::class)
    suspend fun DeleteEvent(evnt: Kevent);

    // +    SAVED EVENT DAO --------->>-------->>--------->>*** -->>----------->>>>
    @Insert(entity = Ksaved::class, onConflict = OnConflictStrategy.REPLACE)
    suspend fun AddSavedEvnt(ksaved: Ksaved);

    @Query("SELECT * FROM Ksaved WHERE saved_id = :inid")
    suspend fun FindSaved(inid : String): Ksaved;

    @Query("SELECT * FROM Ksaved")
    suspend fun GetAllSaved(): List<Ksaved>;

    @Query("DELETE FROM Ksaved WHERE saved_id = :inid")
    suspend fun DeleteWithId(inid: String);

    @Delete(entity = Ksaved::class)
    suspend fun DeleteSaved(ksaved: Ksaved);
}