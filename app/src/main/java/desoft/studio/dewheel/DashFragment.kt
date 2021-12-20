package desoft.studio.dewheel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment

/**
 * A simple [Fragment] subclass.
 * Use the [DashFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DashFragment : Fragment() {

    private val TAG = "-des- [[== DASHBOARD FRAGMENT ==]]"

    private lateinit var showinfoBtn: ImageView;
    private lateinit var infogroup: LinearLayout;

    /**
    * *             onCreate
    */
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState);
    }
    /**
    * *         onCreateView
    */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View?
    {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.frag_dash, container, false)
    }
/**
* *                 onViewCreated
*/
    override fun onViewCreated(v: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(v, savedInstanceState);

        showinfoBtn = v.findViewById(R.id.dashboard_header_showinfo_btn);
        infogroup = v.findViewById(R.id.dashboard_header_user_info_grp);

        showinfoBtn.setOnClickListener {
            if(infogroup.isVisible) {
                infogroup.visibility = View.GONE;
                showinfoBtn.setImageResource(R.drawable.ic_baseline_arrow_drop_down_24);
            }
            else {
                infogroup.visibility = View.VISIBLE;
                showinfoBtn.setImageResource(R.drawable.ic_baseline_arrow_drop_up_24);
            }
        }
    }

}