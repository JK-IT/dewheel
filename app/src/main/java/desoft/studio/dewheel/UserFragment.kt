package desoft.studio.dewheel

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthCredential
import com.google.firebase.auth.GoogleAuthProvider
import desoft.studio.dewheel.katic.KONSTANT
import java.lang.RuntimeException


class UserFragment : Fragment()
{
	private var TAG : String = "-des- [[== USER FRAGMENT ==]]";
	
	private lateinit var fbauth : FirebaseAuth;
	private lateinit var fbuser: FirebaseUser;
	private lateinit var appcache : SharedPreferences;
	
	private lateinit var gooCLIENT : GoogleSignInClient;
	private var gooACC : GoogleSignInAccount? = null;
	private var gooLauncher : ActivityResultLauncher<Intent> = GooSIGNINresultLAUNCHER();
	
	private lateinit var disnameLayout : TextInputLayout;
	private lateinit var disnameTitle :EditText;
	private lateinit var verifiedBtn : Button;
	private lateinit var verifiedImg: ImageView;
	private lateinit var gender: EditText;
	private lateinit var sorient: EditText;
	private lateinit var favor: TextInputEditText;
	private lateinit var logoutbtn: Button;
	private lateinit var delebtn: Button;
	/**
	 * Retrieve current fb user and getting information
	 */
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState);
		appcache = requireContext().getSharedPreferences(getString(R.string.app_pref), Context.MODE_PRIVATE);
		fbauth = FirebaseAuth.getInstance();
		fbuser = fbauth.currentUser!!;
		
		var gooOPTIONS  = GoogleSignInOptions.Builder().requestEmail().requestId()
			.requestIdToken(getString(R.string.default_web_client_id))
			.build();
		gooCLIENT = GoogleSignIn.getClient(requireContext(), gooOPTIONS);
	}
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
	                          savedInstanceState: Bundle?): View?
	{
		// Inflate the layout for this fragment
		var v = inflater.inflate(R.layout.frag_user, container, false);
		disnameTitle = v.findViewById(R.id.user_disname_edt);
		disnameLayout = v.findViewById(R.id.user_disname_layout);
		verifiedBtn = v.findViewById(R.id.user_verified_btn);
		verifiedImg = v.findViewById(R.id.user_verified_img);
		gender = v.findViewById(R.id.user_gender_edt);
		sorient = v.findViewById(R.id.user_orientation_edt);
		favor = v.findViewById(R.id.user_favor_edt);
		logoutbtn = v.findViewById(R.id.user_logout_btn);
		delebtn = v.findViewById(R.id.user_delete_btn);
		SetupVIEWfunc();
		return v;
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?)
	{
		super.onViewCreated(view, savedInstanceState);
		FilloutUSERinfo();
	}
	/* * ================================================*/
	private fun FilloutUSERinfo()
	{
		if(fbuser.isAnonymous) {
			verifiedImg.setColorFilter(ContextCompat.getColor(requireContext(),R.color.grey));
			verifiedBtn.isEnabled = true;
			
			/*val disname = appcache.getString(KONSTANT.username, "");
			if(disname.isNullOrBlank()) // from appcache
			{
				Log.e(TAG, "FilloutUSERinfo: == Cache not working Error", RuntimeException("Failed to get cache data of already verifed user"));
			}
			Log.d(TAG, "FilloutUSERinfo: == NAME FROM CACHE ${disname}");
			disnameTitle.setText(DefaultUSERname(disname!!));*/
		} else
		{
			verifiedBtn.isEnabled = false;
			verifiedImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.confirm));
			val disname = appcache.getString(KONSTANT.username, "");
			if(disname.isNullOrBlank()) // from appcache
			{
				Log.e(TAG, "FilloutUSERinfo: == Cache not working Error", RuntimeException("Failed to get cache data of already verifed user"));
			}
			Log.d(TAG, "FilloutUSERinfo: == NAME FROM CACHE ${disname}");
			disnameTitle.setText(DefaultUSERname(disname!!));
		}
	}
	private fun SetupVIEWfunc()
	{
		disnameLayout.setEndIconOnClickListener {
			Log.d(TAG, "SetupVIEWfunc: uppo");
		}
		verifiedBtn.setOnClickListener {
			var goointe = gooCLIENT.signInIntent;
			gooLauncher.launch(goointe);
		}
	}
	
	//#region GOOGLE SIGN IN LAUNCHER RESULT CALLBACK
	private fun GooSIGNINresultLAUNCHER() : ActivityResultLauncher<Intent>
	{
		return registerForActivityResult(ActivityResultContracts.StartActivityForResult())
		{
			if(it.resultCode == Activity.RESULT_OK){
				GoogleSignIn.getSignedInAccountFromIntent(it.data)
					.addOnSuccessListener {goores ->
						gooACC = goores;
						UpdateUIwithGOO(gooACC!!);
						FbAUTHgoogle(gooACC!!);
					}
					.addOnFailureListener {
						Log.e(TAG, "GooSIGNINresultLAUNCHER: == FAILED TO LOGIN WITH GOOGLE");
					}
			}
		}
	}
	//#endregion
	private fun UpdateUIwithGOO(param: GoogleSignInAccount)
	{
		//Log.d(TAG, "UpdateUIwithGOO: == parame is null ?? ${param == null}");
		disnameTitle.setText(DefaultUSERname(param.displayName));
	}
	
	/**
	 * if fbuser == guest, delete current user, and sign in with google token
	 */
	private fun FbAUTHgoogle(param : GoogleSignInAccount){
		fbuser.delete().addOnCompleteListener {
			if(it.isSuccessful)
			{
				var goocred = GoogleAuthProvider.getCredential(param.idToken, null);
				fbauth.signInWithCredential(goocred)
					.addOnCompleteListener {authres ->
						if(authres.isSuccessful)
						{
							fbuser = fbauth.currentUser!!;
							SavingTOcache(fbuser);
							verifiedBtn.isEnabled = false;
							verifiedImg.setColorFilter(ContextCompat.getColor(requireContext(),R.color.confirm));
							Toast.makeText(requireContext(), "You are now signed in with google", Toast.LENGTH_SHORT).show();
						} else {
							Toast.makeText(requireContext(), "Server Error, please try again later", Toast.LENGTH_LONG).show();
							Log.e(TAG, "FbAUTHgoogle: == Failed to sign in with firebase ");
						}
					}
			}
		}
	}
	
	private fun SavingTOcache(user : FirebaseUser)
	{
		var editor = appcache.edit();
		var usinfo = fbuser.providerData.get(0);
		Log.d(TAG, "SavingTOcache: ${fbuser.providerData.size}");
		Log.d(TAG, "SavingTOcache: ${usinfo.providerId}");
		Log.d(TAG, "SavingTOcache: ${usinfo?.displayName}");
		editor.apply {
			putString(KONSTANT.username, usinfo?.displayName);
			putString(KONSTANT.useruid, fbuser?.uid);
			putString(KONSTANT.usergid, usinfo?.uid);
			putString(KONSTANT.usergmail, usinfo?.email);
			putString(KONSTANT.fone, usinfo?.phoneNumber);
			putLong(KONSTANT.cache_timestamp, System.currentTimeMillis());
		}
		if(editor.commit())
		{
			Log.d(TAG, "SavingTOcache: == DONE SAVING TO CACHE");
		} else
		{
			Log.e(TAG, "SavingTOcache: == SOMETHING WRONG WITH CACHE", RuntimeException("FAILED TO WRITE TO CACHE"));
		}
	}
	
	private fun DefaultUSERname(iname : String?): String{
		if(iname.isNullOrBlank()){
			return "Set A Name";
		}
		return iname;
	}
}