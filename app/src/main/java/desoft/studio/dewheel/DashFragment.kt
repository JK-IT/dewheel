package desoft.studio.dewheel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

/**
 * A simple [Fragment] subclass.
 * Use the [DashFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DashFragment : Fragment() {

    private val TAG = "-des- [[== DASHBOARD FRAGMENT ==]]"

    private var fbuser : FirebaseUser? = null;

    private lateinit var showinfoBtn: ImageView;
    private lateinit var infogroup: LinearLayout;
    private lateinit var verifedBtn: Button;
    private lateinit var editBtn: FrameLayout;

    /**
    * *             onCreate
    */
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState);
        fbuser = FirebaseAuth.getInstance().currentUser;
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

        editBtn = v.findViewById(R.id.dashboard_header_edit_btn);
        infogroup = v.findViewById(R.id.dashboard_header_user_info_grp);

        editBtn.setOnClickListener {
            if(infogroup.isVisible) {
                infogroup.visibility = View.GONE;
            }
            else {
                infogroup.visibility = View.VISIBLE;
            }
        }
/*        showinfoBtn = v.findViewById(R.id.dashboard_header_showinfo_btn);
        showinfoBtn.setOnClickListener {
            showinfoBtn.setImageResource(R.drawable.ic_baseline_arrow_drop_down_24);
            showinfoBtn.setImageResource(R.drawable.ic_baseline_arrow_drop_up_24);
        }*/

        verifedBtn = v.findViewById(R.id.dashboard_header_google_login_btn);
        //verifedBtn.isEnabled = false;
    }

    override fun onStart() {
        super.onStart();
    }

    // + --------->>-------->>--------->>*** -->>----------->>>>

}