package desoft.studio.dewheel

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import desoft.studio.dewheel.Kontrol.DataControl
import desoft.studio.dewheel.katic.KONSTANT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.RuntimeException

class MainActivity : AppCompatActivity()
{
	private val TAG = "-des- <<++ MAIN ACTIVITY ++>>";
	private var iodis = Dispatchers.IO;
	private lateinit var kHandler : Handler;
	private var currDEFAULTnet: Network? = null;
	private lateinit var conmana : ConnectivityManager;
	private lateinit var defaultConnCb: ConnectivityManager.NetworkCallback;
	private lateinit var fbauth : FirebaseAuth;
	private var fbuser : FirebaseUser? = null;
	private lateinit var appCache: SharedPreferences;
	private lateinit var dataFutory : DataControl.DataFactory;
	private lateinit var dataKontrol : DataControl;
	private lateinit var navHost : NavHostFragment;
	private lateinit var navContro : NavController;
	private var loosingBottomDialog : BottomSheetDialog? = null;
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		SetupBOTTOMSheet();
		
		kHandler = Handler(Looper.getMainLooper());
		conmana = getSystemService(ConnectivityManager::class.java);
		if(CheckNETWORKconnection())
		{
			dataFutory = DataControl.DataFactory(application);
			dataKontrol = ViewModelProvider(this, dataFutory).get(DataControl::class.java);
			
			fbauth = FirebaseAuth.getInstance();
			appCache = getSharedPreferences(getString(R.string.app_pref), Context.MODE_PRIVATE);
			CheckUSERauthen();
			
			SetupNavHost();
		}
	}
	
	
	override fun onStart()
	{
		super.onStart();
		SetupDEFAULTconnectionCB();
	}
	
	override fun onStop()
	{
		super.onStop();
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
	
	fun GoBACKtoGATE()
	{
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
	
	/**
	 * Register Callback on connectivity manager
	 * should not call other methods, if it is available as callback, cuz of race condition
	 */
	private fun SetupDEFAULTconnectionCB()
	{
		/*
		var netcap: NetworkCapabilities? = conmana.getNetworkCapabilities(currnet);
		Log.d(TAG, "CheckNETWORKconnection: == network capa ${netcap}");
		var linkpro : LinkProperties? = conmana.getLinkProperties(currnet);
		Log.i(TAG, "CheckNETWORKconnection: == network link prop $linkpro");*/
		defaultConnCb = object:ConnectivityManager.NetworkCallback(){
			//Called when the framework connects and has declared a new network ready for use.
			// if register as default network callback, then only best network instance will invoke this callback
			// do not call getCapabilities or getLinkProperties, cuz of race condition, wait for the callback on those info u need
			override fun onAvailable(network: Network)
			{
				Log.d(TAG, "onAvailable: _____||||||____DEFAULT Network available");
				currDEFAULTnet = network;
				HandlingDEFnetFOUND();
			}
			
			override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities)
			{
				//Log.i(TAG, "onCapabilitiesChanged: _____||||||_____ NEW CAPABILITIES $networkCapabilities")
			}
			
			override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties)
			{
				//Log.d(TAG, "onLinkPropertiesChanged: _____||||||_____ NEW LINK PROPERTIES $linkProperties")
			}
			override fun onLosing(network: Network, maxMsToLive: Int)
			{
				Log.d(TAG, "onLosing: Loosing network connection, wait if on available is called for new candidate");
				
			}
			//Called when a network disconnects or otherwise no longer satisfies this request or callback.
			//only be called when the last network, return from onAvailable, is lost and no other network is available that can satisfy the request
			//If the callback was registered with registerNetworkCallback() it will be called for each network which no longer satisfies the criteria of the callback.
			override fun onLost(network: Network)
			{
				Log.d(TAG, "onLost: NO MORE NETWORK THAT CAN SATISFY THE REQUEST");
				HandlingCONNECTIONlost();
			}
			// call when networkrequest is removed or released or when network request cannot be fulfilled.
			override fun onUnavailable()
			{
				Log.w(TAG, "onUnavailable: There no network availiable that can satisfy the network request");
			}
		}

		conmana.registerDefaultNetworkCallback(defaultConnCb);

	}
	/**
	 * Check for network connectivity
	 * boolean = isdefaultnetworkactive = isactivenetworkmetered
	 * getcurrent_active_network -> null if there are no active network
	 * network capability and link property provides info about network
	 * if NOT NULLL, check for basic capabilities of network, so u can access internet
	 */
	private fun CheckNETWORKconnection(): Boolean
	{
		currDEFAULTnet = conmana.activeNetwork;
		if(currDEFAULTnet == null || conmana.isDefaultNetworkActive == false)
		{// showing no connection dialog
			GateActivity.ShowingNOCONNECTIONdialog(this);
			return false;
		} else
		{
			return true;
		}
	}
	/**
	 * Handling network connection. should be async or sthing that will not block the flow
	 * pop up a dialog, with attempt to reconnect to internet ???
	 * after a timeout --> resume or forcing user to restart the app
	 * finish activity???
	 */
	private fun HandlingCONNECTIONlost()
	{
		kHandler.post {
			if(loosingBottomDialog != null && loosingBottomDialog?.isShowing == false)
			{
				loosingBottomDialog?.show();
			}else
			{
				SetupBOTTOMSheet();
				loosingBottomDialog?.show();
			}
		}
	}
	/**
	 * Connection is back, so we pop back the dialog
	 */
	private fun HandlingDEFnetFOUND()
	{
		kHandler.post {
			if(loosingBottomDialog != null && loosingBottomDialog?.isShowing == true)
			{
				loosingBottomDialog?.dismiss();
			}
		}
	}
	/**
	 * Generate and set up bottomsheet dialog
	 */
	private fun SetupBOTTOMSheet()
	{
		//setting loosing connection bottom sheet dialog
		loosingBottomDialog = BottomSheetDialog(this);
		loosingBottomDialog?.apply {
			setContentView(R.layout.dialog_loosing_net_bottom);
			setCancelable(false);
			setCanceledOnTouchOutside(false);
		}
		val loosingbeha = loosingBottomDialog?.behavior;
		loosingbeha?.apply {
			state= BottomSheetBehavior.STATE_EXPANDED;
			isFitToContents = true;
			isHideable = false;
		}
		
		//set up
	}
	/**
	 * VIEW MODEL - DATA CONTROL RELATED
	 */
}

/*		// this callback keep being called -> waste of resource if u don't do anything
		var activNetCallback = object : ConnectivityManager.OnNetworkActiveListener{
			override fun onNetworkActive()
			{
				Log.i(TAG, "onNetworkActive: == DEFAULT SYSTEM NETWORK HAS GONE TO ACTIVE OR HIGH STATE");
			}
		}
		//conmana.addDefaultNetworkActiveListener(activNetCallback);*/