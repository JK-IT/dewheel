package desoft.studio.dewheel

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import desoft.studio.dewheel.DataKenter.WedaKontrol
import desoft.studio.dewheel.katic.KONSTANT
import desoft.studio.dewheel.local.Kuser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DELETEGateActivity : AppCompatActivity()
{
	private val TAG = "-des- <<++ GATE ACTIVITY ++>>";

	private val wedaKontrol : WedaKontrol by viewModels {WedaKontrol.DataWheelKontrolFactory((application as Wapplication).repo)};
	private lateinit var fbauth : FirebaseAuth;
	private val fstore  = Firebase.firestore;
	private var fbuser : FirebaseUser? = null;

	private lateinit var connmana : ConnectivityManager;
	private lateinit var gooInOptions : GoogleSignInOptions;
	private lateinit var gooInClient : GoogleSignInClient;
	private var gooInAccount : GoogleSignInAccount? = null;
	private var gooLauncher : ActivityResultLauncher<Intent> = KF_GOO_LAUNCHER_CB();
	private lateinit var appCache : SharedPreferences;
	private var goobtn : SignInButton? = null;
	private var guestbtn : Button?=null;
	
	private var botsheet : BottomSheetDialog?=null;

	/**
	* *				onCreate
	 * . get connection manager
	 * . check connection
	 * 		-> yes : get sharepreferences
	 * 					. setup google signin client, firebase instance
	 * 					.start authentication process, setup view
	 * 		-> no: popup dialog saying user is offline, ask them to reconnect
	*/
	@SuppressLint("MissingPermission")
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(R.layout.delete_activity_gate);
		
		connmana = getSystemService(ConnectivityManager::class.java);
		appCache = getSharedPreferences(getString(R.string.app_cache_preference), Context.MODE_PRIVATE);
		KF_SETUP_VIEW();
		if(connmana.activeNetwork != null) {
			Log.d(TAG, "onCreate: default net is actvie");
			gooInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
				.requestIdToken(getString(R.string.default_web_client_id))
				.requestId().requestEmail().build();
			gooInClient = GoogleSignIn.getClient(this, gooInOptions);
			
			fbauth = FirebaseAuth.getInstance();
			fbuser = fbauth.currentUser;
			KF_QUICK_CHECK_USER();
		}
		else {
			KF_NO_INTERNET_DIALOG(this);
		}
	}
/**
* *					onNewIntent
 * . activity launch mode : singletask
 * . this callback will be called when the activity is relaunched
*/
	override fun onNewIntent(intent: Intent?)
	{
		super.onNewIntent(intent);
		Log.d(TAG, "onNewIntent: Gate Activity is RELAUNCHED");
	}
/**
* *					onStart
*/
	override fun onStart()
	{
		super.onStart();
	}

	// + --------->>-------->>--------->>*** -->>----------->>>>
	/**
	 * *		KF_QUICK_CHECK_USER
	 * . check share reference for verification
	 * . check user from server database
	 * . if user != null -> start the wheel
	 */
	private fun KF_QUICK_CHECK_USER()
	{
		if(fbuser != null ) {
			var done = appCache.edit().putBoolean(KONSTANT.userexist, true).commit();
			if(done) {
				KF_START_WHEEL();
			}
		}
	}
	/**
	* * 						KF_SETUP_VIEW
	*/
	private fun KF_SETUP_VIEW()
	{
		goobtn = findViewById(R.id.gate_google_btn);
		guestbtn = findViewById(R.id.gate_guest_btn);
		
		var exist = appCache.getBoolean(KONSTANT.userexist, false);
		if(exist == true && fbuser != null) {
			Log.i(TAG, "KF_SETUP_VIEW: user is NOT NULL");
			Log.i(TAG, "KF_SETUP_VIEW: ${appCache.getBoolean(KONSTANT.userexist, false)}");
			Log.i(TAG, "KF_SETUP_VIEW: ${fbuser?.uid}");
			guestbtn?.isEnabled = false;
		}
		else {
			Log.i(TAG, " Set up View : user is NULL");
			guestbtn?.isEnabled = true;
		}
		KF_SETUP_GUEST_LOGIN();
		//_ setup google button
		goobtn?.setOnClickListener {
			var inte = gooInClient.signInIntent;
			gooLauncher.launch(inte);
		}
	}
	/**
	 * *				KF_SETUP_GUEST_LOGIN
	 * .
	 */
	private fun KF_SETUP_GUEST_LOGIN()
	{
		guestbtn?.setOnClickListener {
			if(fbuser != null && (appCache.getBoolean(KONSTANT.userexist, false) == true))  return@setOnClickListener;
			fbauth.signInAnonymously()
				.addOnSuccessListener {
					Log.d(TAG, "GuestBtn: ==> success guest login");
					guestbtn?.isEnabled  = false;
					fbuser = it.user;
					var edit = appCache.edit().also {
						it.putBoolean(KONSTANT.userexist, true);
						it.putBoolean(KONSTANT.userauthen, false);
						it.putString(KONSTANT.useruid, fbuser?.uid);
					}
					var done = edit.commit();
					if(done){KF_START_WHEEL();}	//
				}
				.addOnFailureListener {
					Log.e(TAG, "GuestBtn: failed to sign in as anonymous");
					var snbar = Snackbar.make(window.decorView.rootView, "Opps!! Cannot connect to server, please try again", Snackbar.LENGTH_SHORT);
					snbar.setAction("OK", object : View.OnClickListener{
						override fun onClick(p0: View?) {
							snbar.dismiss();
						}
					}).show();
				}
		}
	}
	/**
	 * *				KF_GOO_LAUNCHER_CB
	 * GOOGLE SIGN IN LAUNCHER
	 */
	private fun KF_GOO_LAUNCHER_CB() : ActivityResultLauncher<Intent>
	{
		return registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
			var tsk = GoogleSignIn.getSignedInAccountFromIntent(it.data);
			try {
				gooInAccount = tsk.getResult(ApiException::class.java)!!;
				Log.d(TAG, "KF_GOOlauncherCB: == Successfully getting google sign in account");
				KF_UPDATE_USER_AS_GOOGLE(gooInAccount!!);
			}
			catch (e: ApiException	) {
				Log.e(TAG, "KF_GOOlauncherCB: == FAILED TO LOGIN AS GOOGLE ${e.message}");
				var snbar = Snackbar.make(window.decorView.rootView, "Failed to sign in with google account. Please try again", Snackbar.LENGTH_LONG);
				snbar.setAction("OK", object : View.OnClickListener{
					override fun onClick(p0: View?)
					{
						snbar.dismiss();
					}
				}).show();
			}
		}
	}
	/**
	 * *			KF_SIGNIN_AS_GOOGLE
	 * . get credential from GoogleSignInAccount
	 * . check if it is the same google account
	 * . -> true = > start the wheel, value already in user room database
	 * . -> false = > create a new user in room database
	 * . SIGN IN GOOGLE, will creade user on database with unique uid, which similar to anonymous uid
	 */
	private fun KF_SIGNIN_AS_GOOGLE(gacc : GoogleSignInAccount)
	{
		var cred = GoogleAuthProvider.getCredential(gacc.idToken, null);
		fbauth.signInWithCredential(cred)
			.addOnCompleteListener {
				if(it.isSuccessful)	{
					fbuser = it.result.user;
					Log.d(TAG, "SigninWITHgoogle: == success SIGNING , size of fbuser ${fbuser?.providerData?.size}");
					var goouser = fbuser?.providerData?.get(1);
					var kser = Kuser(fbuser?.uid!!, goouser?.email, goouser?.uid,null, goouser?.displayName);
					lifecycleScope.launch {
						withContext(Dispatchers.IO) {
							var resid = wedaKontrol.VM_ADD_USER_LOCAL(kser);
							Log.d(TAG, "KF_SIGNIN_AS_GOOGLE: ADDING USER TO DATABASE $resid");
						}
						var done =KF_UPDATE_CACHE_GOOGLE(it.result.user!!);
						if(done) {KF_START_WHEEL();}
					}
				}
				else {
					Log.e(TAG, "SigninWITHgoogle: == failed to sign in with google ",it.exception );
					var snbar = Snackbar.make(window.decorView.rootView, "Failed to sign in with google account. Please try again", Snackbar.LENGTH_SHORT);
					snbar.setAction("OK", object : View.OnClickListener{
						override fun onClick(p0: View?)
						{
							snbar.dismiss();
						}
					}).show();
				}
			}
	}
	/**
	 * * 			KF_UPDATE_USER_AS_GOOGLE
	 * . case: user login as anonymous, then get out of the app, then going back and log in with google account
	 * . -> link with existing userid
	 * . case : user directly login as google
	 * . -> sign in as google
	 */
	private fun KF_UPDATE_USER_AS_GOOGLE(gacc : GoogleSignInAccount)
	{
		if(fbuser != null && fbuser?.isAnonymous == true) {
			var cred = GoogleAuthProvider.getCredential(gacc.idToken, null);
			try {
				fbuser?.linkWithCredential(cred)
					?.addOnSuccessListener {
						Log.d(TAG, "SigninWITHgoogle: == success LINKING, size of fbuser ${it.user?.providerData?.size}");
						fbuser = it.user;
						var goouser = fbuser?.providerData?.get(1);
						var kser = Kuser(fbuser?.uid!!, goouser?.email, goouser?.uid, null, goouser?.displayName);
						lifecycleScope.launch {
							withContext(Dispatchers.IO) {
								var resid = wedaKontrol.VM_ADD_USER_LOCAL(kser);
								Log.d(TAG, "KF_SIGNIN_AS_GOOGLE: ADDING USER TO DATABASE $resid");
							}
							var done =KF_UPDATE_CACHE_GOOGLE(it.user!!);
							if(done) {KF_START_WHEEL();}
						}
					}
					?.addOnFailureListener {
						Log.w(TAG, "KF_UPDATE_USER_AS_GOOGLE: FAILED TO LINK USER ${it.message}");
						var snbar = Snackbar.make(window.decorView.rootView, "Opps!! Cannot connect to server, please try again", Snackbar.LENGTH_SHORT);
						snbar.setAction("OK", object : View.OnClickListener{
							override fun onClick(p0: View?)
							{
								snbar.dismiss();
							}
						}).show();
					}
			}
			catch (exc : Exception) {
				Log.w(TAG, "KF_UPDATE_USER_AS_GOOGLE: ${exc.message}");
				var snbar = Snackbar.make(window.decorView.rootView, "Opps!! Cannot connect to server, please try again", Snackbar.LENGTH_SHORT);
				snbar.setAction("OK", object : View.OnClickListener{
					override fun onClick(p0: View?) {
						snbar.dismiss();
					}
				}).show();
			}
		}
		else {
			KF_SIGNIN_AS_GOOGLE(gacc);
		}
	}
	// + --------->>-------->>--------->>*** -->>----------->>>>

	/**
	* 		*			KF_UPDATE_CACHE_GOOGLE
	*/
	private fun KF_UPDATE_CACHE_GOOGLE(usr : FirebaseUser) : Boolean
	{
		var gooprovider = usr.providerData[1];
		var edit = appCache.edit().also {
			it.putBoolean(KONSTANT.userexist, true);
			it.putBoolean(KONSTANT.userauthen, true);
			it.putString(KONSTANT.useruid, usr?.uid);
			it.putString(KONSTANT.usergid, gooprovider.uid);
			it.putString(KONSTANT.usergmail, gooprovider.email);
			it.putString(KONSTANT.username, gooprovider.displayName);
		}
		return edit.commit();
	}

	/**
	 * *				KF_START_WHEEL
	 */
	private fun KF_START_WHEEL()
	{
		var inte = Intent(this, DELETEMainActivity::class.java);
		startActivity(inte);
	}
	
	/**
	 * *NO INTERNET CONNECTION BOTTOM SHEET DIALOG
	 */
	private fun KF_NO_INTERNET_DIALOG(acti: Activity)
	{
		botsheet = BottomSheetDialog(acti);
		botsheet?.apply {
			setContentView(R.layout.dialog_net_bottom);
			setCancelable(false);
			setCanceledOnTouchOutside(false);
		}
		var okbtn = botsheet?.findViewById<Button>(R.id.dialog_net_ok_btn);
		okbtn?.setOnClickListener {
			botsheet?.dismiss();
			finishAndRemoveTask();
			startActivity(Intent(this, DELETEGateActivity::class.java));
		}
		var beha = botsheet?.behavior;
		beha?.apply {
			isHideable = false;
			isFitToContents = true;
			state = BottomSheetBehavior.STATE_EXPANDED;
		}
		botsheet?.show();
	}
	companion object	{
	
	}
	
	
}