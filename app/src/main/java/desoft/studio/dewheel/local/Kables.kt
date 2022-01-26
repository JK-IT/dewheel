package desoft.studio.dewheel.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey
import desoft.studio.dewheel.kata.FireEvent

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

@Entity
data class Kevent(
    @PrimaryKey(autoGenerate = true) val id : Long,
    @ColumnInfo(name = "event_name") val name:String,
    @ColumnInfo(name = "event_description") val description:String?=null,
    /*@ColumnInfo(name = "event_type") val type:Int,*/
    @ColumnInfo(name = "event_time") var time:String?=null,
    @ColumnInfo(name = "event_time_inmilli") var timeInMilli:Long?=null,
    @ColumnInfo(name = "event_location") val location:String,
    @ColumnInfo(name = "event_lati") val lati:Double,
    @ColumnInfo(name = "event_longi") val longi:Double,
    @ColumnInfo(name = "event_locality") val locality:String?,
    @ColumnInfo(name = "event_sub_locality") val sublocality:String?,
    @ColumnInfo(name = "event_admin1") val admin1:String?,
    @ColumnInfo(name = "event_zip_code") val zipCode:String?,
    @ColumnInfo(name = "event_country") val country:String?,
)

@Entity
data class Ksaved(
    @PrimaryKey @ColumnInfo(name="saved_id") val id: String,
    @ColumnInfo(name="saved_area") var area : String?=null,
    @ColumnInfo(name="saved_admin1") var admin1:String?=null,
    @ColumnInfo(name="fire_evnt") val firevnt : FireEvent?=null
)