package desoft.studio.dewheel

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.text.SpannableStringBuilder
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import desoft.studio.dewheel.kata.FireUser
import desoft.studio.dewheel.katic.KONSTANT
import java.lang.RuntimeException


class UserFragment : Fragment()
{
	private var TAG : String = "-des- [[== USER FRAGMENT ==]]";
	
	private lateinit var handlerWorker : Handler;
	
	private lateinit var fbauth : FirebaseAuth;
	private lateinit var fbuser: FirebaseUser;
	private lateinit var appcache : SharedPreferences;
	
	private lateinit var gooCLIENT : GoogleSignInClient;
	private var gooACC : GoogleSignInAccount? = null;
	private var gooLauncher : ActivityResultLauncher<Intent> = GooSIGNINresultLAUNCHER();
	
	private lateinit var disnameLayout : TextInputLayout;
	private lateinit var disnameTitle :TextInputEditText;
	private lateinit var goobtn : SignInButton;
	private lateinit var verifiedBtn : Button;
	private lateinit var verifiedImg: ImageView;
	private lateinit var genderDropLayout: TextInputLayout;
	private lateinit var genderDropLine: AutoCompleteTextView;
	private lateinit var sorientDropLayout: TextInputLayout;
	private lateinit var sorientDropLine: AutoCompleteTextView;
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
		handlerWorker = Handler(requireActivity().mainLooper);
		appcache = requireContext().getSharedPreferences(getString(R.string.app_cache_preference), Context.MODE_PRIVATE);
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
		goobtn = v.findViewById(R.id.user_goobtn);
		verifiedBtn = v.findViewById(R.id.user_verified_btn);
		verifiedImg = v.findViewById(R.id.user_verified_img);
		genderDropLayout = v.findViewById(R.id.user_gender_drop_layout);
		genderDropLine = v.findViewById(R.id.user_gender_drop_line);
		
		sorientDropLayout = v.findViewById(R.id.user_sexori_drop_layout);
		sorientDropLine = v.findViewById(R.id.user_sexori_drop_line);
		
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
			goobtn.isEnabled = true;
			verifiedBtn.isEnabled = true;
		} else
		{
			goobtn.isEnabled = false;
			val disname = appcache.getString(KONSTANT.username, "");
			if(disname.isNullOrBlank()) // from appcache
			{
				Log.e(TAG, "FilloutUSERinfo: == CACHE USERNAME IS NULL");
			}
			Log.d(TAG, "FilloutUSERinfo: == NAME FROM CACHE ${disname}");
			disnameTitle.setText(DefaultUSERname(disname!!));
		}

		// fill out gender, other info from cache
		val genderpref = appcache.getString(KONSTANT.gender, "");
		if(genderpref?.isNotBlank() == true)
		{
			genderDropLine.text = SpannableStringBuilder( genderpref);
		}
		val sorientpref = appcache.getString(KONSTANT.sexori, "");
		if(sorientpref?.isNotBlank() == true)
		{
			sorientDropLine.text = SpannableStringBuilder(sorientpref);
		}
		val favorpref = appcache.getString(KONSTANT.favor, "");
		if(favorpref?.isNotBlank() == true)
		{
			favor.text = SpannableStringBuilder(favorpref);
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
		//_goobtn
		goobtn.setOnClickListener{
			var goointe = gooCLIENT.signInIntent;
			gooLauncher.launch(goointe);
		}
		// _verified button
		verifiedBtn.setOnClickListener {
			FieldChecked(true);
		}
		//_set up gender list and gender spinner
		var genderlist : ArrayList<String> = arrayListOf(*resources.getStringArray(R.array.gender_list));
		var genderAdapter = ArrayAdapter<String>(requireContext(), R.layout.support_simple_spinner_dropdown_item, genderlist);
		genderDropLine.setAdapter(genderAdapter);
		genderDropLine.setOnItemClickListener { adapview, view, pos, l ->
			val selecitem = adapview?.getItemAtPosition(pos);
			Log.d(TAG, "onItemSelected: == User picked this item $selecitem");
			appcache.edit().putString(KONSTANT.gender, selecitem.toString()).apply();
		}
		
		//_ set up sex list and sex spinner
		var sexlist : ArrayList<String> = arrayListOf(*resources.getStringArray(R.array.sex_list));
		var sexdapter = ArrayAdapter<String>(requireContext(), R.layout.support_simple_spinner_dropdown_item, sexlist);
		sorientDropLine.setAdapter(sexdapter);
		sorientDropLine.setOnItemClickListener { adapterView, view, pos, l ->
			val seitem = adapterView.getItemAtPosition(pos);
			Log.d(TAG, "onItemSelected: == User picked this item $seitem");
			appcache.edit().putString(KONSTANT.sexori, seitem.toString()).apply();
		}

		//_ favor things
		favor.setOnEditorActionListener { textView, i, keyEvent ->
			if(i== EditorInfo.IME_ACTION_DONE)
			{
				favor.clearFocus();
			}
			false;
		}
		/*
		* update cache with favorite
		* do field checked to become verified member if satisfied
		 */
		favorbtn.setOnClickListener {
			var valu = favor.text.toString();
			if( ! valu.isBlank())
			{
				favor.clearFocus();
				var oldfavor = appcache.getString(KONSTANT.favor, "");
				if(oldfavor.isNullOrBlank() || ! oldfavor.contentEquals(valu, true))
				{
					if(appcache.edit().putString(KONSTANT.favor, valu).commit())
					{
						Toast.makeText(requireContext(), "Successfully updating your information", Toast.LENGTH_SHORT).show();
					}
					FieldChecked(true);
				}
			}
		}
		//_logout button
		logoutbtn.setOnClickListener {
			fbauth.signOut();
			(requireContext() as MainActivity).KF_TO_GATE_ACTIVITY();
		}
		//_delete account button
		delebtn.setOnClickListener {
			fbuser.delete().addOnCompleteListener {
				if(it.isSuccessful){
					Toast.makeText(requireContext(), "Successful Delete Your Account", Toast.LENGTH_SHORT).show();
					(requireContext() as MainActivity).KF_TO_GATE_ACTIVITY();
				}else
				{
					Log.e(TAG, "SetupVIEWfunc: == Failed to delete from server");
					Toast.makeText(requireContext(), "Server Error, please try again later", Toast.LENGTH_SHORT).show();
				}
			}
		}
	}
	
	override fun onStart() {
		super.onStart();
		if(appcache.getBoolean(KONSTANT.userexist, false) == true)
		{
			FieldChecked(false, false);
		} else
		{
			FieldChecked(false, true);
		}
	}
	
	//#region GOOGLE SIGN IN LAUNCHER RESULT CALLBACK
	private fun GooSIGNINresultLAUNCHER() : ActivityResultLauncher<Intent>
	{
		return registerForActivityResult(ActivityResultContracts.StartActivityForResult())
		{
			if(it.resultCode == Activity.RESULT_OK){
				Log.d(TAG, "GooSIGNINresultLAUNCHER: == Google sign in return ok");
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
		goobtn.isEnabled = false;
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
							FieldChecked();
							Toast.makeText(requireContext(), "You are now signed in with google", Toast.LENGTH_SHORT).show();
						} else {
							Toast.makeText(requireContext(), "Server Error, please try again later", Toast.LENGTH_LONG).show();
							Log.e(TAG, "FbAUTHgoogle: == Failed to sign in with firebase ");
						}
					}
			}
		}
	}
	
	/**
	 * after sign in , do fields check to let user know they need to fill out form so they can become verifed user.
	 * FIELD CHECKED  will enable the button by itself
	 * IT WILL BE THE KNOT THAT UPDATE USER TO FIRESTORE
	 * @param show : true to show toast what u missed
	 */
	private fun FieldChecked(show: Boolean = false, upload: Boolean = true) : Boolean
	{
		var gender = appcache.getString(KONSTANT.gender, "");
		var sorient = appcache.getString(KONSTANT.sexori, "");
		var favor = appcache.getString(KONSTANT.favor, "");
		if(gender?.isBlank() == true || sorient?.isBlank() == true || favor?.isBlank() == true || fbuser.isAnonymous == true)
		{
			verifiedBtn.isEnabled = true;
			verifiedImg.setColorFilter(ContextCompat.getColor(requireContext(),R.color.grey));
			if(show == true)
			{
				if(gender?.isBlank() == true)
					Toast.makeText(requireContext(), "Please Pick Your Gender", Toast.LENGTH_SHORT).show();
				if(sorient?.isBlank() == true)
					Toast.makeText(requireContext(), "Please Pick Your Interest", Toast.LENGTH_SHORT).show();
				if(favor?.isBlank() == true)
					Toast.makeText(requireContext(), "Please Confirm Your Favorites", Toast.LENGTH_SHORT).show();
				if(fbuser.isAnonymous)
					Toast.makeText(requireContext(), "Please Sign In/Up With Google Account", Toast.LENGTH_SHORT).show();
			}
			return false;
		} else
		{
			verifiedBtn.isEnabled = false;
			verifiedImg.setColorFilter(ContextCompat.getColor(requireContext(),R.color.material_dynamic_secondary30));
			genderDropLayout.isEnabled = false;
			sorientDropLayout.isEnabled = false;
			if(upload)
				KF_UPLOADuserDATA();
			return true;
		}
	}
	
	/**
	 * THIS WILL BE CALLED AFTER SUCCESSFUL SIGIN AS GOOGLE USER
	 */
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
	
	private fun KF_UPLOADuserDATA()
	{
		//viewmodel update user to firestore
		var kid : String = appcache.getString(KONSTANT.usergid, "")!!;
		var fbid: String = appcache.getString(KONSTANT.useruid, "")!!;
		var email : String = appcache.getString(KONSTANT.usergmail, "")!!;
		var app_user_name: String = appcache.getString(KONSTANT.username, "")!!;
		var gender: String = appcache.getString(KONSTANT.gender, "")!!;
		var sorient: String = appcache.getString(KONSTANT.sexori, "")!!;
		var favorite: String = appcache.getString(KONSTANT.favor, "")!!;
		var usr = FireUser(kid, fbid, email, app_user_name, gender, sorient, favorite);
		(requireActivity() as MainActivity).KF_UPuserTOstore(usr);
	}
	
	private fun DefaultUSERname(iname : String?): String{
		if(iname.isNullOrBlank()){
			return "Set A Name";
		}
		return iname;
	}
}