package desoft.studio.dewheel

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
import androidx.core.content.edit
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
import desoft.studio.dewheel.Kontrol.DataControl
import desoft.studio.dewheel.Kontrol.WedaKontrol
import desoft.studio.dewheel.katic.KONSTANT
import desoft.studio.dewheel.local.Kuser

class GateActivity : AppCompatActivity()
{
	private val TAG = "-des- <<++ GATE ACTIVITY ++>>";

	private val dataKontrol : DataControl by viewModels{DataControl.DataFactory(application)};
	private val wedaKontrol : WedaKontrol by viewModels {WedaKontrol.DataWheelKontrolFactory((application as Wapplication).repo)};

	private lateinit var connmana : ConnectivityManager;
	private lateinit var fbauth : FirebaseAuth;
	private var fbuser : FirebaseUser? = null;
	private lateinit var gooInOptions : GoogleSignInOptions;
	private lateinit var gooInClient : GoogleSignInClient;
	private var gooInAccount : GoogleSignInAccount? = null;
	private var gooLauncher : ActivityResultLauncher<Intent> = KF_GOO_LAUNCHER_CB();
	private lateinit var appCache : SharedPreferences;
	private var goobtn : SignInButton? = null;
	private var guestbtn : Button?=null;

	/**
	* *				onCreate
	 * . get connection manager
	 * . check connection
	 * 		-> yes : get sharepreferences
	 * 					. setup google signin client, firebase instance
	 * 					.start authentication process, setup view
	 * 		-> no: popup dialog saying user is offline, ask them to reconnect
	*/
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_gate);
		
		connmana = getSystemService(ConnectivityManager::class.java);
		if(KF_CHECK_ONLINE())
		{
			appCache = getSharedPreferences(getString(R.string.app_pref), Context.MODE_PRIVATE);
			
			gooInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
				.requestIdToken(getString(R.string.default_web_client_id))
				.requestId().requestEmail().build();
			gooInClient = GoogleSignIn.getClient(this, gooInOptions);
			
			fbauth = FirebaseAuth.getInstance();

			goobtn = findViewById(R.id.gate_google_btn);
			guestbtn = findViewById(R.id.gate_guest_btn);

			KF_SETUP_VIEW();
			KF_VERIFY_USER();
		}
	}
	// + --------->>-------->>--------->>*** -->>----------->>>>
	/**
	 * *		KF_CHECK_ONLINE
	 * Check for connection
	 * FAIL => SHOWING DIALOG FRAGMENT
	 * SUCCESS => RETURN;
	 */
	private fun KF_CHECK_ONLINE() : Boolean
	{
		//Log.i(TAG, "CheckConnection: Active network null ?${connmana.activeNetwork == null} and curr net active ?${connmana.isDefaultNetworkActive}");
		if(connmana.activeNetwork == null)  {
			ShowingNOCONNECTIONdialog(this);
			return false;
		}
		else  {
			return true;
		}
	}
	/**
	 * *		KF_VERIFY_USER
	 * . check share reference for verification
	 * .-> if verifed -> check for current user on firebase server
	 * -> incase user is deleted from server -> enable login interface again
	 */
	private fun KF_VERIFY_USER()
	{
		var verified = appCache.getBoolean(KONSTANT.userverified, false);
		if(verified) {
			fbuser = fbauth.currentUser;
			if(fbuser != null) {
				Log.d(TAG, "StartAuthen: ==> user is NOT null, uid ${fbuser?.uid}");
				KF_START_WHEEL();
			} else {
				Log.w(TAG, "StartAuthen: == user is null");
				KF_ENA_LOGIN_INTERFACE();
			}
		}
		else {
			Log.d(TAG, "KF_VERIFY_USER: user not verified");
			KF_ENA_LOGIN_INTERFACE();
		}
	}
	/**
	* * 	KF_ENA_LOGIN_INTERFACE
	*/
	private fun KF_ENA_LOGIN_INTERFACE()
	{
		goobtn?.isEnabled = true;
		guestbtn?.isEnabled = true;
		appCache.edit {
			putBoolean(KONSTANT.userverified, false);
			putString(KONSTANT.useruid, "");
			commit();
		}
	}
	/**
	* * 	KF_SETUP_VIEW
	*/
	private fun KF_SETUP_VIEW()
	{
		KF_SETUP_GUEST_LOGIN();
		//_ setup google button
		goobtn?.setOnClickListener {
			var inte = gooInClient.signInIntent;
			gooLauncher.launch(inte);
		}
	}
	/**
	 * *				KF_SETUP_GUEST_LOGIN
	 * . Sign in anonymous
	 * . onSUCCESS : save fb user id to appcache & room , then start next activity
	 * . failed : check on connection
	 */
	private fun KF_SETUP_GUEST_LOGIN()
	{
		guestbtn?.setOnClickListener {
			if(fbuser != null)  return@setOnClickListener;
			fbauth.signInAnonymously()
				.addOnSuccessListener {
					Log.d(TAG, "GuestBtn: ==> success guest login - uid [${fbuser?.uid}");
					fbuser = it.user;
					KF_UPDATE_CACHE_ROOM();
					KF_START_WHEEL();
				}
				.addOnFailureListener {
					Log.e(TAG, "GuestBtn: failed to sign in as anonymous");
					goobtn?.isEnabled = false;
					guestbtn?.isEnabled = false;
					var snbar = Snackbar.make(window.decorView.rootView, "Opps!! Cannot connect to server, please restart the application", Snackbar.LENGTH_INDEFINITE);
					snbar.setAction("OK", object : View.OnClickListener{
						override fun onClick(p0: View?)
						{
							finishAndRemoveTask();
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
			}catch (e: ApiException	){
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
	 * . SIGN IN GOOGLE, will creade user on database with unique uid, which similar to anonymous uid
	 */
	private fun KF_SIGNIN_AS_GOOGLE(gacc : GoogleSignInAccount)
	{
		var cred = GoogleAuthProvider.getCredential(gacc.idToken, null);
		fbauth.signInWithCredential(cred)
			.addOnCompleteListener {
				if(it.isSuccessful){
					fbuser = it.result.user;
					Log.d(TAG, "SigninWITHgoogle: == success signin , size of fbuser ${fbuser?.providerData?.size}");
					KF_START_WHEEL();
				} else
				{
					Log.e(TAG, "SigninWITHgoogle: == failed to sign in with google ",it.exception );
				}
			}
	}
	/**
	 * * 			KF_UPDATE_USER_AS_GOOGLE
	 * . this is the function when user login with google
	 * ! case: user login as anonymous, then get out of the app, then going back and log in with google account
	 * -> . so we update the current user with google account.
	 *
	 */
	private fun KF_UPDATE_USER_AS_GOOGLE(gacc : GoogleSignInAccount)
	{
		if(fbuser != null && (fbuser?.isAnonymous== true))
		{
			fbuser?.delete()?.addOnCompleteListener {
				if(it.isSuccessful) {
					Log.d(TAG, "LinkORsign: == success delete temp account");
					KF_SIGNIN_AS_GOOGLE(gacc);
				}
				else {
					Log.e(TAG, "LinkORsign: Failed to delete anonyID", it.exception);
					var snbar = Snackbar.make(window.decorView.rootView, "Failed to connect to server. Please check your connection and try again", Snackbar.LENGTH_LONG);
					snbar.setAction("OK", object : View.OnClickListener{
						override fun onClick(p0: View?)
						{
							snbar.dismiss();
						}
					}).show();
				}
			}
		}
		else {
			KF_SIGNIN_AS_GOOGLE(gacc);
		}
	}
	// + --------->>-------->>--------->>*** -->>----------->>>>

	/**
	* 		*			KF_UPDATE_CACHE_ROOM
	*/
	private fun KF_UPDATE_CACHE_ROOM()
	{
		appCache.edit {
			putBoolean(KONSTANT.userverified, true);
			putString(KONSTANT.useruid, fbuser?.uid);
			commit();
		}
		var kewuser = Kuser(0, fuid = fbuser!!.uid);
		dataKontrol.KF_VM_ADD_USER_TO_ROOM(kewuser);
	}


	/**
	 * *				KF_START_WHEEL
	 */
	private fun KF_START_WHEEL()
	{
		var inte = Intent(this, MainActivity::class.java);
		inte.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK;
		startActivity(inte);
		finish();
	}
	
	
	companion object{
		/**
		 * NO INTERNET CONNECTION BOTTOM SHEET DIALOG
		 */
		fun ShowingNOCONNECTIONdialog(acti: Activity)
		{
			var botshee = BottomSheetDialog(acti);
			botshee.apply {
				setContentView(R.layout.dialog_net_bottom);
				setCancelable(false);
				setCanceledOnTouchOutside(false);
			}
			var okbtn = botshee.findViewById<Button>(R.id.dialog_net_ok_btn);
			okbtn?.setOnClickListener {
				botshee.dismiss();
				acti.finish();
			}
			var beha = botshee.behavior;
			beha.apply {
				isHideable = false;
				isFitToContents = true;
				state = BottomSheetBehavior.STATE_EXPANDED;
			}
			botshee.show();
		}
	}
	
	
}