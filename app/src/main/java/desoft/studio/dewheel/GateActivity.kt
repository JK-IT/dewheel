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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

class GateActivity : AppCompatActivity()
{
	private val TAG = "-des- <<++ GATE ACTIVITY ++>>";
	
	private lateinit var connmana : ConnectivityManager;
	private lateinit var fbauth : FirebaseAuth;
	private var fbuser : FirebaseUser? = null;
	private lateinit var gooInOptions : GoogleSignInOptions;
	private lateinit var gooInClient : GoogleSignInClient;
	private var gooInAccount : GoogleSignInAccount? = null;
	private var gooLauncher : ActivityResultLauncher<Intent> = KF_GOOlauncherCB();
	private lateinit var shrepref : SharedPreferences;
	private lateinit var goobtn : SignInButton;
	private lateinit var guestbtn : Button;
	private var noCONNECTION : Boolean = false;
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_gate);
		
		connmana = getSystemService(ConnectivityManager::class.java);
		if(CheckConnection())
		{
			shrepref = getSharedPreferences(getString(R.string.app_pref), Context.MODE_PRIVATE);
			
			gooInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
				.requestIdToken(getString(R.string.default_web_client_id))
				.requestId().requestEmail().build();
			gooInClient = GoogleSignIn.getClient(this, gooInOptions);
			
			fbauth = FirebaseAuth.getInstance();
			
			goobtn = findViewById(R.id.gate_google_btn);
			guestbtn = findViewById(R.id.gate_guest_btn);
			
			StartAuthen();
			SetupVIEWfunc();
		}
	}
	
	override fun onDestroy()
	{
		super.onDestroy();
	}
	
	/* *---------------------------------------*/
	/**
	 * Check for connection
	 * FAIL => SHOWING DIALOG FRAGMENT
	 * SUCCESS => RETURN;
	 */
	private fun CheckConnection() : Boolean
	{
		if(connmana.isDefaultNetworkActive == false || connmana.activeNetwork == null)
		{
			noCONNECTION = true;
			ShowingNOCONNECTIONdialog(this);
			return false;
		} else
		{
			return true;
		}
	}
	
	/**
	 * fbuser == null --> user not login or data is deleted
	 * -> then just let them login either with google or get new anonymous id if they wiped their data
	 *
	 * Flow : user == null, stay here, else ->> start next activity
	 */
	private fun StartAuthen()
	{
		fbuser = fbauth.currentUser;
		if(fbuser != null) //-> start next activity
		{
			Log.d(TAG, "StartAuthen: ==> user is NOT null, uid ${fbuser?.uid}");
			StartMain();
		} else {
			Log.w(TAG, "StartAuthen: == user is null");
		}
	}
	
	private fun SetupVIEWfunc()
	{
		GuestBtn();
		GooBtn();
	}
	/**
	 * if user== null,
	 * Sign in anonymous - > success then start next activity
	 * failed -> stay here
	 */
	private fun GuestBtn()
	{
		guestbtn.setOnClickListener {
			if(fbuser != null)  return@setOnClickListener;
			fbauth.signInAnonymously().addOnSuccessListener {
				fbuser = it.user;
				Log.d(TAG, "GuestBtn: ==> success guest login - uid [${fbuser?.uid}");
				StartMain();
			}
				.addOnFailureListener {
				Log.e(TAG, "GuestBtn: failed to sign in as anonymous");
				var snbar = Snackbar.make(window.decorView.rootView, "Opps!! Cannot connect to server, please restart the application", Snackbar.LENGTH_INDEFINITE);
				snbar.setAction("OK", object : View.OnClickListener{
					override fun onClick(p0: View?)
					{
						finish();
						var inte = intent; // get self intent
						inte.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP;
						startActivity(inte);
					}
				}).show();
			}
		}
	}
	
	/**
	 * GOOGLE SIGN IN LAUNCHER
	 */
	private fun KF_GOOlauncherCB() : ActivityResultLauncher<Intent>
	{
		return registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
			var tsk = GoogleSignIn.getSignedInAccountFromIntent(it.data);
			try {
				gooInAccount = tsk.getResult(ApiException::class.java)!!;
				Log.d(TAG, "KF_GOOlauncherCB: == Successfully getting google sign in account");
				LinkORsign(gooInAccount!!);
			}catch (e: ApiException	){
				Log.e(TAG, "KF_GOOlauncherCB: == FAILED TO LOGIN AS GOOGLE", RuntimeException(e.message));
			}
		}
	}
	
	/**
	 * Link with google account
	 * NOTE: if google account already link to an anonymous, try to link it with another one will failed
	 * * should we delete this anonymous for safety ????? or just disable it
	 *
	 */
	private fun LinkWITHgoogle(gacc : GoogleSignInAccount)
	{
		var cred = GoogleAuthProvider.getCredential(gacc.idToken, null);
		fbuser?.linkWithCredential(cred)
			?.addOnSuccessListener {
				Log.d(TAG, "LinkWITHgoogle: == success link to google account");
				fbuser = it.user;
			}?.addOnFailureListener {
				Log.e(TAG, "LinkWITHgoogle: == failed to link with google", it);
			}
	}
	
	/**
	 * SIGN IN GOOGLE, will creade user on database with unique uid, which similar to anonymous uid
	 */
	private fun SigninWITHgoogle(gacc : GoogleSignInAccount)
	{
		var cred = GoogleAuthProvider.getCredential(gacc.idToken, null);
		fbauth.signInWithCredential(cred)
			.addOnCompleteListener {
				if(it.isSuccessful){
					fbuser = it.result.user;
					Log.d(TAG, "SigninWITHgoogle: == success signin , size of fbuser ${fbuser?.providerData?.size}");
					StartMain();
				} else
				{
					Log.e(TAG, "SigninWITHgoogle: == failed to sign in with google ",it.exception );
				}
			}
	}
	
	/**
	 * if user == null, log in as guest then link google account
	 * -> which will throw error if google already link to another anonymous
	 * solution :: ask user to log out and log in as google instead
	 * or just use sign in instead of link
	 *
	 * if user== null and isAnonymous - > just delete from server the anonymous and sign in with google
	 */
	private fun LinkORsign(gacc : GoogleSignInAccount)
	{
		if(fbuser != null && (fbuser?.isAnonymous== true))
		{
			fbuser?.delete()?.addOnCompleteListener {
				if(it.isSuccessful)
				{
					Log.d(TAG, "LinkORsign: == success delete temp account");
					SigninWITHgoogle(gacc);
				} else {
					Log.e(TAG, "LinkORsign: Failed to delete anonyID", it.exception);
				}
			}
		}
		else {
			SigninWITHgoogle(gacc);
		}
	}
	/**
	 * Google sign in button
	 * user!=null and anonymous --> google btn will link goo account to this anonymous
	 * else then this is google account, they already login , no need to ask for last signin
	 * user==null then signin as google or anonymous
	 * if signin as google, try to get last google signin account 
	 */
	private fun GooBtn()
	{
		goobtn.setOnClickListener {
			var inte = gooInClient.signInIntent;
			gooLauncher.launch(inte);
		}
	}
	
	/**
	 * Going Forward to next
	 */
	private fun StartMain()
	{
		var inte = Intent(this, MainActivity::class.java);
		//inte.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK;
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