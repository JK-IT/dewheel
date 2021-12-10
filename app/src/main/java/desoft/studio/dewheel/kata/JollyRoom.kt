package desoft.studio.dewheel.kata

import android.os.Parcel
import android.os.Parcelable

data class JollyRoom(
    var id : String?, // == jolly jid
    var roomsg: Kmessage?
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readParcelable(Kmessage::class.java.classLoader)
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeParcelable(roomsg, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<JollyRoom> {
        override fun createFromParcel(parcel: Parcel): JollyRoom {
            return JollyRoom(parcel)
        }

        override fun newArray(size: Int): Array<JollyRoom?> {
            return arrayOfNulls(size)
        }
    }

}

data class Kmessage(
    var senderid : String? = null, // gid of sender
    var sendername: String?=null, // name of sender
    var sendermsg: String?=null // the message
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(senderid)
        parcel.writeString(sendername)
        parcel.writeString(sendermsg)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Kmessage> {
        override fun createFromParcel(parcel: Parcel): Kmessage {
            return Kmessage(parcel)
        }

        override fun newArray(size: Int): Array<Kmessage?> {
            return arrayOfNulls(size)
        }
    }

}
