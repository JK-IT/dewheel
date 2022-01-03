package desoft.studio.dewheel.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

/**
 * *            Kuser Table
* firebase = fuid, fb name, provider id, gmail
 * app = username, gender, sorientation, favorite things
*/
@Entity
data class Kuser (
    @PrimaryKey
    @ColumnInfo(name = "fuid") val fuid: String,
    @ColumnInfo(name = "google_email") val google_email:String?=null,
    @ColumnInfo(name = "google_id") val google_id:String?=null,
    @ColumnInfo(name="local_username") var local_username:String?=null,
    @ColumnInfo(name = "remote_username") var remote_username:String?=null,
    @ColumnInfo(name = "user_gender") var user_gender:String?=null,
    @ColumnInfo(name = "user_sorientation") var user_sorientation:String?=null,
    @ColumnInfo(name = "user_trails") var user_trails:String?=null,
    )
/**
* *         full text search enable for Kuser table
*/
@Fts4(contentEntity = Kuser::class)
@Entity
data class KuserFts(
    @ColumnInfo(name = "fuid") val fuid: String,
    @ColumnInfo(name = "google_id") val google_id: String?,
    @ColumnInfo(name = "google_email") val google_email: String?,
)