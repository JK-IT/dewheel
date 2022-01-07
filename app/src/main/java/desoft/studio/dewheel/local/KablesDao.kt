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

    @Delete(entity = Kevent::class)
    suspend fun DeleteEvent(evnt: Kevent);
}