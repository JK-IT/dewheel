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
import android.view.inputmethod.EditorInfo
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
	private lateinit var disnameTitle :TextInputEditText;
	private lateinit var verifiedBtn : Button;
	private lateinit var verifiedImg: ImageView;
	private lateinit var gender: EditText;
	private lateinit var genderbtn : Button;
	private lateinit var sorient: EditText;
	private lateinit var sorientbtn : Button;
	private lateinit var favor: TextInputEditText;
	private lateinit var favorbtn: Button;
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
			.requestProfile()
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
		genderbtn = v.findViewById(R.id.user_gender_upbtn);
		sorient = v.findViewById(R.id.user_orientation_edt);
		sorientbtn = v.findViewById(R.id.user_sex_orient_upbtn);
		favor = v.findViewById(R.id.user_favor_edt);
		favorbtn = v.findViewById(R.id.user_favor_upbtn);
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
		} else
		{
			verifiedBtn.isEnabled = false;
			verifiedImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.confirm));
			val disname = appcache.getString(KONSTANT.username, "");
			if(disname.isNullOrBlank()) // from appcache
			{
				Log.e(TAG, "FilloutUSERinfo: == CACHE USERNAME IS NULL");
			}
			Log.d(TAG, "FilloutUSERinfo: == NAME FROM CACHE ${disname}");
			disnameTitle.setText(DefaultUSERname(disname!!));
		}
	}
	private fun SetupVIEWfunc()
	{
		//_title layout
		disnameLayout.setEndIconOnClickListener {
			//Log.d(TAG, "SetupVIEWfunc: uppo");
			disnameTitle.isEnabled = !(disnameTitle.isEnabled);
			if(disnameTitle.isEnabled)
			{
				disnameTitle.requestFocus() ;
			}
			if(disnameTitle.text.toString().isBlank())
			{
				disnameTitle.text = SpannableStringBuilder("Set A Name");
			} else
			{
				var inname = disnameTitle.text.toString().contains("Set A Name");
				if( ! inname)
				{
					appcache.edit().putString(KONSTANT.username, disnameTitle.text.toString()).apply();
				}
			}
			
		}
		//_title input
		disnameTitle.setOnEditorActionListener { textView, i, keyEvent ->
			if(i == EditorInfo.IME_ACTION_DONE){
				disnameTitle.clearFocus();
				disnameTitle.isEnabled = false;
				if(disnameTitle.text.toString().isBlank())
				{
					disnameTitle.text = SpannableStringBuilder("Set A Name");
				} else
				{
					appcache.edit().putString(KONSTANT.username, disnameTitle.text.toString()).apply();
				}
				Log.d(TAG, "SetupVIEWfunc: New text value ${disnameTitle.text}");
			}
			false;
		}
		// _verified button
		verifiedBtn.setOnClickListener {
			var goointe = gooCLIENT.signInIntent;
			gooLauncher.launch(goointe);
		}
		//_ gender button
		genderbtn.setOnClickListener {
			var value = gender.text.toString();
			if( ! value.isBlank())
			{
				appcache.edit().putString(KONSTANT.gender, value).apply();
				gender.isEnabled = false;
			}
		}
		//_ sex orientation
		sorientbtn.setOnClickListener {
			var valu = sorient.text.toString();
			if( ! valu.isBlank())
			{
				appcache.edit().putString(KONSTANT.sexori, valu).apply();
			}
		}
		//_ favor things
		favorbtn.setOnClickListener {
			var valu = favor.text.toString();
			if( ! valu.isBlank())
			{
				appcache.edit().putString(KONSTANT.favor, valu).apply();
			}
		}
		//_logout button
		logoutbtn.setOnClickListener {
			fbauth.signOut();
			(requireContext() as MainActivity).GoBACKtoGATE();
		}
		//_delete account button
		delebtn.setOnClickListener {
			fbuser.delete().addOnCompleteListener {
				if(it.isSuccessful){
					Toast.makeText(requireContext(), "Successful Delete Your Account", Toast.LENGTH_SHORT).show();
					(requireContext() as MainActivity).GoBACKtoGATE();
				}else
				{
					Log.e(TAG, "SetupVIEWfunc: == Failed to delete from server");
					Toast.makeText(requireContext(), "Server Error, please try again later", Toast.LENGTH_SHORT).show();
				}
			}
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
		Log.d(TAG, "UpdateUIwithGOO: == gOOGLE account name ${param.displayName}");
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
							Log.d(TAG, "FbAUTHgoogle: == FB NOW CONNECT TO GOOGLE");
							fbuser = fbauth.currentUser!!;
							UpdateUIwithGOO(gooACC!!);
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
		var usinfo = fbuser.providerData.get(1);
		Log.d(TAG, "SavingTOcache: ${fbuser.providerData.size}");
		Log.d(TAG, "SavingTOcache: ${usinfo.providerId}");
		Log.d(TAG, "SavingTOcache: ${usinfo?.displayName}");
		editor.apply {
			putString(KONSTANT.username, usinfo?.displayName);
			putString(KONSTANT.useruid, fbuser.uid);
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