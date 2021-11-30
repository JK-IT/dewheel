package desoft.studio.dewheel.kata

import android.os.Parcel
import android.os.Parcelable

/**
* Database : EVENTS
 * 	-JID	-KID	-NAME	-ADDR	-TIME
 * 	Kid is gid
*/

data class WheelJolly(
	var jid: String? = null,
	var creator: String? = null,
	var kid:String? = null,
	var name: String? = null,
	var addr: String? = null,
	var area: String? = null,
	var time: Long? = null,
) : Parcelable {
	constructor(parcel: Parcel) : this(
		parcel.readString(),
		parcel.readString(),
		parcel.readString(),
		parcel.readString(),
		parcel.readString(),
		parcel.readString(),
		parcel.readValue(Long::class.java.classLoader) as? Long
	) {}

	override fun writeToParcel(parcel: Parcel, flags: Int) {
		parcel.writeString(jid)
		parcel.writeString(creator)
		parcel.writeString(kid)
		parcel.writeString(name)
		parcel.writeString(addr)
		parcel.writeString(area)
		parcel.writeValue(time)
	}

	override fun describeContents(): Int {
		return 0
	}

	companion object CREATOR : Parcelable.Creator<WheelJolly> {
		override fun createFromParcel(parcel: Parcel): WheelJolly {
			return WheelJolly(parcel)
		}

		override fun newArray(size: Int): Array<WheelJolly?> {
			return arrayOfNulls(size)
		}
	}

}
