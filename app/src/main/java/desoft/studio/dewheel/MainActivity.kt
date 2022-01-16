package desoft.studio.dewheel

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.edit
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import desoft.studio.dewheel.Kontrol.DataControl
import desoft.studio.dewheel.Kontrol.WedaKontrol
import desoft.studio.dewheel.kata.FireUser
import desoft.studio.dewheel.kata.Kadress
import desoft.studio.dewheel.kata.WheelJolly
import desoft.studio.dewheel.katic.KONSTANT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity()
{
	private val TAG = "-des- <<++ MAIN ACTIVITY ++>>";
	private var iodis = Dispatchers.IO;

	private lateinit var handlerWorker : Handler;
	private var currentActiveNet: Network? = null;
	private lateinit var conmana : ConnectivityManager;
	private lateinit var fbauth : FirebaseAuth;
	private var fbuser : FirebaseUser? = null;
	private lateinit var appCache: SharedPreferences;
	private val wedaKontrol : WedaKontrol by viewModels{WedaKontrol.DataWheelKontrolFactory((application as Wapplication).repo)};

	private val dataKontrol : DataControl by viewModels { DataControl.DataFactory(application) }
	private lateinit var navHost : NavHostFragment;
	private lateinit var navContro : NavController;
	private var loosingBottomDialog : BottomSheetDialog? = null;

	private var topbar : ActionBar? = null;

	/**
	* *				onCreate
	 * . initialize places api
	 * .check online status of your app
	 * . if online -> get fb user , compare to appcache id
	 * . if appcache id == fb.uid -> query from rom to get update user live data
	 * . if not - > go back to gate
	 * .chech if user identity has any errors, like cannot get current user from firebase
	*/
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		handlerWorker = Handler(Looper.getMainLooper());
		conmana = getSystemService(ConnectivityManager::class.java);
		appCache = getSharedPreferences(getString(R.string.app_cache_preference), Context.MODE_PRIVATE);
		fbauth = FirebaseAuth.getInstance();

		if(KF_CHECK_ONLINE_STAT()) {
			KF_VERIFY_USER();
			var cacheid = appCache.getString(KONSTANT.useruid, "");
			if(cacheid.equals(fbuser?.uid)) {
				wedaKontrol.VM_FIND_USER_LOCAL(fbuser?.uid!!);
			} else {
				appCache.edit {
					clear();
					commit();
				}
				KF_TO_GATE_ACTIVITY();
			}
		}
		UI_SETUP_CONNECTION_DIALOG();
		UI_SETUP_APPBAR();
		UI_SETUP_HOST();
	}
	/**
	* *						onStart
	 * . register for connection callback
	*/
	override fun onStart()
	{
		Log.d(TAG, "onStart: MAIN ACTIVITY");
		super.onStart();
		conmana.registerDefaultNetworkCallback(defNetworkCallback, handlerWorker);
	}

/*	override fun onBackPressed() {
		var alerdia = AlertDialog.Builder(this)
			.setMessage("You are about to exit. Do you want to continue?")
			.setCancelable(true)
			.setPositiveButton("Exit"){_, _ ->
				finishAndRemoveTask();
			}
			.setNegativeButton("Cancel", {dia , _ ->
				dia.dismiss();
			});
		alerdia.show();
	}*/
	/**
	*	*					onStop
	 *. unregister network callback
	*/
	override fun onStop() {
		super.onStop();
		conmana.unregisterNetworkCallback(defNetworkCallback);
	}
	// + --------->>-------->>--------->>*** -->>----------->>>>
	/**
	* *					UI_SETUP_APPBAR
	*/
	private fun UI_SETUP_APPBAR()
	{
		var appbar = findViewById<Toolbar>(R.id.main_material_toolbar);
		if(supportActionBar?.isShowing == true) {
			supportActionBar?.hide();
		}
		setSupportActionBar(appbar);
		topbar = supportActionBar;
	}
	/**
	 * *		UI_SETUP_HOST
	 *
	 */
	private fun UI_SETUP_HOST()
	{
		navHost = supportFragmentManager.findFragmentById(R.id.main_nav_host) as NavHostFragment;
		navContro = navHost.navController;
		//findViewById<BottomNavigationView>(R.id.main_bottom_bar).setupWithNavController(navContro);
	}
	/**
	 * *			KF_VERIFY_USER
	 * . Check if user == null. true return to gate activity
	 * . If user verified == false, but user is not null -> re write to app cache
	 */
	private fun KF_VERIFY_USER()
	{
		fbuser = fbauth.currentUser;
		if(fbuser == null) {
			Log.e(TAG, "CheckUSERauthen: == USER == NULL ??? WHY, MAY BE FIREBASE DELETE IT" );
			KF_TO_GATE_ACTIVITY();
		}
		else {
			appCache.edit().apply{
				putBoolean(KONSTANT.userexist, true);
				apply();
			}
		}
	}
	/**
	* *				KF_TO_GATE_ACTIVITY
	*/
	fun KF_TO_GATE_ACTIVITY()
	{
		var inte = Intent(this, GateActivity::class.java);
		startActivity(inte);
	}
	//#region NETWORK AREA
	/**
	* ! 		defNetworkCallback : NETWORK CALLBACK
	*/
	private val defNetworkCallback = object : ConnectivityManager.NetworkCallback(){
		override fun onAvailable(network: Network) {
			super.onAvailable(network);
			Log.i(TAG, "${Thread.currentThread().name} NETWORK onAvailable: ");
			currentActiveNet = network;
			loosingBottomDialog?.dismiss();
		}
		// this method is not guaranteed to be called before onLost
		override fun onLosing(network: Network, maxMsToLive: Int) {
			super.onLosing(network, maxMsToLive);
			Log.i(TAG, "${Thread.currentThread().name} NETWORK onLosing: ");
		}

		override fun onLost(network: Network) {
			super.onLost(network);
			Log.i(TAG, "${Thread.currentThread().name} NETWORK onLost: ");
			loosingBottomDialog?.show();
		}

		override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities)
		{
			super.onCapabilitiesChanged(network, networkCapabilities);
			Log.i(TAG, "onCapabilitiesChanged: $networkCapabilities");
		}

		override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
			super.onLinkPropertiesChanged(network, linkProperties);
			Log.i(TAG, "onLinkPropertiesChanged: $linkProperties");
		}
		override fun onUnavailable() {
			super.onUnavailable();
		}
	}
	/**
	 * *				KF_CHECK_ONLINE_STAT
	 * Check for network connectivity
	 */
	private fun KF_CHECK_ONLINE_STAT(): Boolean
	{
		currentActiveNet = conmana.activeNetwork;
		if(currentActiveNet == null)
		{// showing no connection dialog
			GateActivity.ShowingNOCONNECTIONdialog(this);
			return false;
		} else
		{
			return true;
		}
	}
	//#endregion
	/**
	 * *						UI_SETUP_CONNECTION_DIALOG
	 * Generate and set up bottomsheet dialog
	 */
	private fun UI_SETUP_CONNECTION_DIALOG()
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

	// + --------->>-------->>--------->>*** -->>----------->>>>
/**
 * ?VIEW MODEL - DATA CONTROL RELATED
 */

/**
* 	*						KF_UPuserTOstore()
*/
	fun KF_UPuserTOstore(usr : FireUser)
	{
		Log.d(TAG, "KF_UPuserTOstore: == Get user to upload $usr");
		dataKontrol.KF_VM_UP_USER(usr);
	}
	
	/**
	 * *					UPLOAD FLAG WATCHER
	 * // ! upload flag observer
	 * write to cache to have appropriate action later
	 */
	private val uploadFlagWatcher = Observer<Boolean>{
		if(it) {
			Log.i(TAG, "successfully updated user to store: ");
			appCache.edit().apply{
				putBoolean(KONSTANT.userexist, true);
				putBoolean(KONSTANT.user_upload_flag, true);
				putLong(KONSTANT.cache_timestamp, System.currentTimeMillis());
				apply();
			}
		} else {
			appCache.edit().apply {
				putBoolean(KONSTANT.user_upload_flag, false);
				putBoolean(KONSTANT.userexist, false);
				putLong(KONSTANT.cache_timestamp, System.currentTimeMillis());
				apply();
			}
		}
	}

// + --------->>-------->>--------->>*** -->>----------->>>>
	/**
	* 	*					KF_UPLOAD_JOLLY
	 * 	! UPLOADING JOLLY EVENT TO DATABASE
	*/
	fun KF_UPLOAD_JOLLY(iname: String, iaddr:String, itime:Long, ivenue : Kadress) {
		dataKontrol.KF_VM_UP_JOLLY(iname, iaddr, itime, ivenue);
	}

	/**
	* *							JOLLY EVENT WATCHER
	 * ! register observer watcher on jolly event
	*/
	// ! Jolly UPLOADING WATCHER
	private val jollyWatcher = Observer<Boolean> {
		if (it) {
			Toast.makeText(this, "Successful uploading the event", Toast.LENGTH_SHORT).show();
			//navContro.navigate(R.id.action_jollyCreationFragment_to_wheelRoot);
		} else {
			Log.e(
				TAG,
				"Jolly uploading flag: User is null or Failed to contact server",
				throw java.lang.RuntimeException("Check your View model if User is NUll")
			);
			Toast.makeText(
				this,
				"Failed uploading the occurrence. Please try again later!!",
				Toast.LENGTH_SHORT
			).show();

		}
	}
// + --------->>-------->>--------->>*** -->>----------->>>>

	/**
	 * *									KF_START_CHAT_ROOM
	* . UPLOAD CHAT NODE ON DB
	 * . START CHAT ACTIVITY
	*/
	fun KF_START_CHAT_ROOM(evnt: WheelJolly)
	{
		var inte = Intent(this, ChatActivity::class.java);
		var bund = Bundle();
		bund.apply {
			putParcelable(ChatActivity.chatJollyBundlekey, evnt);
			putString(ChatActivity.chatJollyRoomKey, evnt.jid);
		}
		inte.putExtra(ChatActivity.chatIntentkey, bund);
		lifecycleScope.launch(iodis) {
			var done = dataKontrol.KF_VM_CHATROOM(evnt.jid!! ,evnt);

			when(done) {
				is DataControl.ResultBox.RoomExist -> {
					if(done.yesorno) startActivity(inte);
				}
				is DataControl.ResultBox.VoidResult -> {
					done.resu.addOnSuccessListener {
						startActivity(inte);
					}
				}
			}
		}
	}



}
/*		// this callback keep being called -> waste of resource if u don't do anything
		var activNetCallback = object : ConnectivityManager.OnNetworkActiveListener{
			override fun onNetworkActive()
			{
				Log.i(TAG, "onNetworkActive: == DEFAULT SYSTEM NETWORK HAS GONE TO ACTIVE OR HIGH STATE");
			}
		}
		//conmana.addDefaultNetworkActiveListener(activNetCallback);*/