package desoft.studio.dewheel.SubKlass

import androidx.room.TypeConverter
import com.google.gson.Gson
import desoft.studio.dewheel.kata.FireEvent

class Konverters
{
    @TypeConverter
    fun toJsonFireEvent(fvent : FireEvent) : String
    {
        return Gson().toJson(fvent);
    }

    @TypeConverter
    fun toFireEventJson(fevejson : String) : FireEvent
    {
        return Gson().fromJson(fevejson, FireEvent::class.java);
    }
}