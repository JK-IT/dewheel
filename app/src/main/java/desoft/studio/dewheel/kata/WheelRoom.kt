package desoft.studio.dewheel.kata

import android.os.Parcel
import android.os.Parcelable

data class WheelRoom(
    val id: String?,
    var statfrom : Boolean?,
    var statto: Boolean?,
    var msgfrom: String?,
    var msgto: String?, ) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        parcel.readString(),
        parcel.readString()
    ) {}

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeValue(statfrom)
        parcel.writeValue(statto)
        parcel.writeString(msgfrom)
        parcel.writeString(msgto)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<WheelRoom> {
        override fun createFromParcel(parcel: Parcel): WheelRoom {
            return WheelRoom(parcel)
        }

        override fun newArray(size: Int): Array<WheelRoom?> {
            return arrayOfNulls(size)
        }
    }

}
