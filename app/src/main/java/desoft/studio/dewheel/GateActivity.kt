package desoft.studio.dewheel

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class GateActivity : AppCompatActivity()
{
	private val TAG = "-des- << GATE ACTIVITY >>";
	private lateinit var fbauth : FirebaseAuth;
	private var fbuser : FirebaseUser? = null;
	private lateinit var gooInOptions : GoogleSignInOptions;
	private lateinit var gooInClient : GoogleSignInClient;
	private lateinit var shrepref : SharedPreferences;
	private lateinit var goobtn : SignInButton;
	private lateinit var guestbtn : Button;
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_gate);
		
		shrepref = getSharedPreferences(getString(R.string.app_pref), Context.MODE_PRIVATE);
		
		gooInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
			.requestIdToken(getString(R.string.default_web_client_id))
			.requestId().requestEmail().build();
		gooInClient = GoogleSignIn.getClient(this, gooInOptions);
		
		fbauth = FirebaseAuth.getInstance();
		fbuser = fbauth.currentUser;
		
		goobtn = findViewById(R.id.gate_google_btn);
		guestbtn = findViewById(R.id.gate_guest_btn);
		
		StartAuthen();
		SetupVIEWfunc();
	}
	
	override fun onStop()
	{
		//todo : clear later, now testing
		//fbauth.signOut();
		super.onStop();
	}
	
	/* *---------------------------------------*/
	/**
	 * fbuser == null --> user not login , data is deleted
	 * -> then just let them login either with google or get new anonymous id if they wiped their data
	 */
	private fun StartAuthen()
	{
		if(fbuser != null) //-> start next activity
		{
			Log.d(TAG, "StartAuthen: ==> user is NOT null, uid ${fbuser?.uid}");
			var inte = Intent(this, MainActivity::class.java);
			//startActivity(inte);
			//finish();
		}
	}
	
	private fun SetupVIEWfunc()
	{
		GuestBtn();
		GooBtn();
	}
	/**
	 * Sign in anonymous - > success then start next activity
	 * failed -> stay here
	 */
	private fun GuestBtn()
	{
		guestbtn.setOnClickListener {
			fbauth.signInAnonymously().addOnSuccessListener {
				fbuser = it.user;
				Log.d(TAG, "GuestBtn: ==> success guest login - uid [${fbuser?.uid}");
				//start main activity
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
	 * Google sign in button
	 */
	private fun GooBtn()
	{
		if(fbuser != null && (fbuser?.isAnonymous == true))
		{
			// link in with google account
			
		}
	}
}