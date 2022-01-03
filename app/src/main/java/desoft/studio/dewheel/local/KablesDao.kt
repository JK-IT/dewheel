package desoft.studio.dewheel.local

import androidx.room.*

@Dao
interface KablesDao {
    @Insert(entity = Kuser::class,onConflict = OnConflictStrategy.REPLACE)
    suspend fun InsertUser(vararg kus: Kuser);

    @Update(entity = Kuser::class, onConflict = OnConflictStrategy.REPLACE)
    suspend fun UpdateUser(vararg kus: Kuser);

    @Query("SELECT * FROM Kuser WHERE fuid = :uid")
    suspend fun FindUser(uid : String): Kuser?;

    @Delete(entity = Kuser::class)
    suspend fun DeleteUser(vararg kus:Kuser);


}