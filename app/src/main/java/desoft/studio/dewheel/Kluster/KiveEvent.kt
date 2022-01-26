package desoft.studio.dewheel.Kluster

import android.location.Geocoder
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import desoft.studio.dewheel.kata.FireEvent

class KiveEvent(val andgeo : Geocoder, var fevnt : FireEvent) : ClusterItem
{
    override fun getPosition(): LatLng {
        var reslst = andgeo.getFromLocationName(fevnt.location.toString(),1);
        return LatLng(reslst[0].latitude, reslst[0].longitude);
    }

    override fun getTitle(): String? {
        return fevnt.name.toString();
    }

    override fun getSnippet(): String? {
        return fevnt.about.toString();
    }

}