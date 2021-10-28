package desoft.studio.dewheel

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import desoft.studio.dewheel.katic.KONSTANT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.RuntimeException

class MainActivity : AppCompatActivity()
{
	private val TAG = "-des- <<++ MAIN ACTIVITY ++>>";
	private var iodis = Dispatchers.IO;
	private lateinit var fbauth : FirebaseAuth;
	private var fbuser : FirebaseUser? = null;
	private lateinit var appCache: SharedPreferences;
	
	private lateinit var navHost : NavHostFragment;
	private lateinit var navContro : NavController;
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		fbauth = FirebaseAuth.getInstance();
		appCache = getSharedPreferences(getString(R.string.app_pref), Context.MODE_PRIVATE);
		CheckUSERauthen();
		
		SetupNavHost();
	}
	
	/**
	 * Setup the navigation host
	 */
	private fun SetupNavHost()
	{
		navHost = supportFragmentManager.findFragmentById(R.id.main_nav_host) as NavHostFragment;
		navContro = navHost.navController;
		findViewById<BottomNavigationView>(R.id.main_bottom_bar).setupWithNavController(navContro);
	}
	/**
	 * Check if user == null. true return to gateActi, else process saving to cache
	 * if user != null -> save gid, uid, email, disname from google provider
	 * else -> save uid, uname from firebase anonymous authen
	 */
	private fun CheckUSERauthen()
	{
		fbuser = fbauth.currentUser;
		if(fbuser == null)
		{
			Log.e(TAG, "CheckUSERauthen: == USER == NULL ??? WHY", )
			GoBACKtoGATE();
		} else
		{
			lifecycleScope.launch(iodis) {
				var editor = appCache.edit();
				if(fbuser?.isAnonymous == true)
				{
					//saving userid = username
					editor.apply {
						putString(KONSTANT.username, fbuser?.uid);
						putString(KONSTANT.useruid, fbuser?.uid);
					}
				} else
				{
					// at this step , mostly getting data from cache, not from provider
					var usinfo = fbuser?.providerData?.get(1);
					Log.d(TAG, "CheckUSERauthen: == Name from google provider ${usinfo?.displayName}");
					var usname = appCache.getString(KONSTANT.username, "");
					if(usname?.isBlank() == true)
					{
						usname = usinfo?.displayName;
					}
					editor.apply {
						putString(KONSTANT.username, usname);
						putString(KONSTANT.useruid, fbuser?.uid);
						putString(KONSTANT.usergid, usinfo?.uid);
						putString(KONSTANT.usergmail, usinfo?.email);
						putString(KONSTANT.fone, usinfo?.phoneNumber);
					}
				}
				editor.apply {
					putLong(KONSTANT.cache_timestamp, System.currentTimeMillis());
					apply();
				}
			}
		}
	}
	
	fun GoBACKtoGATE(){
		var inte = Intent(this, GateActivity::class.java);
		inte.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK;
		startActivity(inte);
		finish();
	}
	
	fun SavingTOcache()
	{
		Log.d(TAG, "SavingTOcache: == running");
		var editor = appCache.edit();
		fbuser = fbauth.currentUser;
		var usinfo = fbuser?.providerData?.get(0);
		Log.d(TAG, "SavingTOcache: ${fbuser?.providerId} - ${fbuser?.providerData?.size}");
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

}