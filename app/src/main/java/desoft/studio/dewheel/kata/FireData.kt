package desoft.studio.dewheel.kata

import android.os.Parcel
import android.os.Parcelable

/**
 * upload event on database, what u need
 * -----------------------------------------------------------
 * places picture  ===      [[[[[[----]]]]]]]                   |
 *           ++++   lll   +++  llll          aaaaaa             |
 *      kkk    ,,,..  kkkkk  ...,,, kkk >>>><<<<<               |
 * name: keyle == app_user_name                                 |
 * prompt: let's hangout at this beautiful restaurant           |
 * time and date : milli-second format                          |
 * location: -> this will be dynamic                            |
 * user id on firestore, to get userinfo                        |
 * (click on see button will read data of user from server )    |
 * see user-info: {gender, sexual orientation, favorite things} |
 * Chat -> will open chat activity
 */

/**
 * FIRESTORE DATABASE : USERS
 * 	-kid	-fbid	-email	-username	-gender 	-sorient	-favorite
 * fone number, email , address will be secret, only u give it to the other
 * id: gmail_gid
 * fbid: uid
 * app_user_name
 * gender
 * sexual orientation
 * favorite
 */
data class FireUser(
    var fid: String? = null,
    var gid : String? = null,
    var email : String? = null,
    var app_user_name: String? = null,
    var gender: String?=null,
    var sorient: String?=null,
    var about: String?=null
)

data class FireEvent(
    var name:String?=null,
    var hostname:String?=null,
    var hostid:String?=null, //-> fbid
    var about: String?=null,
    var time:String?=null,
    var evntimilli : Long?=null,
    var timestamp: Long?=null,
    var location:String?=null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readString()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(hostname)
        parcel.writeString(hostid)
        parcel.writeString(about)
        parcel.writeString(time)
        parcel.writeValue(evntimilli)
        parcel.writeValue(timestamp)
        parcel.writeString(location)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<FireEvent> {
        override fun createFromParcel(parcel: Parcel): FireEvent {
            return FireEvent(parcel)
        }

        override fun newArray(size: Int): Array<FireEvent?> {
            return arrayOfNulls(size)
        }
    }

}

data class BriefFireEvent(
    var id : String? = null,
    var fev : FireEvent? = null,
    var saved: Boolean? =null
)