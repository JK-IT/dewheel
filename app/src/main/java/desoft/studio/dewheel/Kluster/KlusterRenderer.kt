package desoft.studio.dewheel.Kluster

import android.content.Context
import android.view.LayoutInflater
import android.widget.ImageView
import com.google.android.gms.maps.GoogleMap
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.google.maps.android.ui.IconGenerator
import desoft.studio.dewheel.R

class KlusterRenderer<T : ClusterItem>(var ctx : Context, var inmap : GoogleMap, var clusman : ClusterManager<T>) : DefaultClusterRenderer<T>(ctx, inmap, clusman)
{
    private val iconGenerator = IconGenerator(ctx.applicationContext);
    private val markerimgview = ImageView(ctx);
    private var imgv : ImageView? = null;

    init {
        //markerimgview.layoutParams = ViewGroup.LayoutParams(50, 50);
        var vi = LayoutInflater.from(ctx).inflate(R.layout.design_cluster_view, null);
        imgv = vi.findViewById<ImageView>(R.id.design_cluster_imgview);
        iconGenerator.setContentView(vi);

    }

    /*override fun onBeforeClusterItemRendered(item: T, markerOptions: MarkerOptions) {
        //markerimgview.setImageResource(R.mipmap.ic_celebration);
        imgv?.setImageResource(R.mipmap.ic_celebration);
        var icobm = iconGenerator.makeIcon();
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icobm));
    }*/

    override fun shouldRenderAsCluster(cluster: Cluster<T>): Boolean {
        return cluster.size >=3;
    }
}