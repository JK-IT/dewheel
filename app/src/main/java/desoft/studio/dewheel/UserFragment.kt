package desoft.studio.dewheel

import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser


class UserFragment : Fragment()
{
	private var TAG : String = "-des- [[== USER FRAGMENT ==]]";
	
	private lateinit var fbauth : FirebaseAuth;
	private lateinit var fbuser: FirebaseUser;
	private lateinit var appcache : SharedPreferences;
	
	private lateinit var disnameTitle :EditText;
	private lateinit var verifiedBtn : Button;
	private lateinit var verifiedImg: ImageView;
	/**
	 * Retrieve current fb user and getting information
	 */
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState);
		fbauth = FirebaseAuth.getInstance();
		fbuser = fbauth.currentUser!!;
		FilloutUSERinfo();
	}
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
	                          savedInstanceState: Bundle?): View?
	{
		// Inflate the layout for this fragment
		var v = inflater.inflate(R.layout.frag_user, container, false);
		
		return v;
	}
	
	/* * ================================================*/
	private fun FilloutUSERinfo()
	{
		if(fbuser.isAnonymous) {
		
		}
		
	}
	
}