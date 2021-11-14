package desoft.studio.dewheel

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentResultListener
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.libraries.places.api.model.AddressComponents
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import desoft.studio.dewheel.Kontrol.DataControl
import desoft.studio.dewheel.kata.Kadress
import desoft.studio.dewheel.katic.KONSTANT
import kotlinx.coroutines.launch
import java.util.*

private const val TAG :  String = "-des- [[== WHEEL FRAGMENT ==]]";

class WheelFragment : Fragment()
{
	private lateinit var dataFutory : DataControl.DataFactory;
	private lateinit var dataKontrol : DataControl;
	private lateinit var locationLiveDat: MutableLiveData<Kadress>;
	private lateinit var appcache : SharedPreferences;
	
	
	private val locationPermLauncher: ActivityResultLauncher<Array<String>> = GetLOCATIONpermissionCB();
	private val coar_perm = android.Manifest.permission.ACCESS_COARSE_LOCATION;
	private val fine_perm = android.Manifest.permission.ACCESS_FINE_LOCATION;
	private lateinit var fulocation : FusedLocationProviderClient;
	private lateinit var lowPowerQuest : LocationRequest;
	private lateinit var accuQuest : LocationRequest;
	private lateinit var slowLocReq: LocationRequest;
	private lateinit var cliSettings : SettingsClient;
	private lateinit var geocoder : Geocoder;
	private var currLocation : Location? = null;
	private var locConfLauncher = KF_LOC_CONFIG_LAUNCHER();
	private lateinit var locationHead : LinearLayout;
	private lateinit var locationText : TextView;
	private lateinit var getCurrLocationBtn : TextView;
	private lateinit var locationPickFrag : LocationPickFragment;
	
	private lateinit var hideViewBtn : ImageView;
	private lateinit var locationCopy : TextView;
	
	private lateinit var addBtn : ImageView;
	
	
	private val cancelTokSrc : CancellationTokenSource = CancellationTokenSource();
	
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState);
		
		dataFutory = DataControl.DataFactory(requireActivity().application, FirebaseAuth.getInstance().currentUser!!);
		dataKontrol = ViewModelProvider(requireActivity(), dataFutory).get(DataControl::class.java);
		locationLiveDat = dataKontrol.pickedLocation;
		appcache = requireActivity().getSharedPreferences(getString(R.string.app_pref), Context.MODE_PRIVATE);
		
		fulocation = LocationServices.getFusedLocationProviderClient(requireContext());
		cliSettings = LocationServices.getSettingsClient(requireContext());
		geocoder = Geocoder(requireContext(), Locale.getDefault());
		slowLocReq = LocationRequest.create().apply {
			interval = 1800000;
			fastestInterval = 60000;
			priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
		}
		accuQuest = LocationRequest.create()?.apply {
			priority = LocationRequest.PRIORITY_HIGH_ACCURACY;
			interval = 5000;
			fastestInterval = 3000;
		}!!
		lowPowerQuest = LocationRequest.create()?.apply {
			priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
			interval = 15000;
			fastestInterval = 7000;
		}!!
		locationPickFrag = LocationPickFragment(requireActivity());
	}
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
	                          savedInstanceState: Bundle?): View?
	{
		// Inflate the layout for this fragment
		var v = inflater.inflate(R.layout.frag_wheel, container, false);
		locationHead = v.findViewById(R.id.wheel_location_head);
		locationText = v.findViewById(R.id.wheel_location_tedit);
		getCurrLocationBtn = v.findViewById(R.id.wheel_get_current_location);
		hideViewBtn = v.findViewById(R.id.wheel_hide_view_btn);
		locationCopy = v.findViewById(R.id.wheel_location_copy);
		addBtn = v.findViewById(R.id.wheel_add_btn);
		
		return v;
	}
	
	
	
	/**
	 * Register FRAGMENT MANAGER FOR RECEIVING PICKED LOCATION
	 */
	override fun onViewCreated(view: View, savedInstanceState: Bundle?)
	{
		KF_SETUP_VIEWS();
		childFragmentManager.setFragmentResultListener(locationQuestKey, this, object : FragmentResultListener{
			override fun onFragmentResult(requestKey : String, result : Bundle) {
				if(requestKey.contentEquals(locationQuestKey)){
					var addrlst = geocoder.getFromLocation(result.getDouble(placesLati)!!, result.getDouble(placesLongi)!!,1 );
					if(addrlst.isNotEmpty()) {
						Log.d(TAG, "onFragmentResult: = this location $addrlst[0]");
					}
				}
			}
		})
		CheckLOCATIONperm();
	}
	private fun KF_SETUP_VIEWS() {
		//_ location display name
		locationText.inputType = InputType.TYPE_NULL;
		locationText.setOnClickListener {
			Log.i(TAG, "KF_SETUP_VIEWS: = location edit is clicked");
			locationPickFrag.show(childFragmentManager, LocationPickFragment.fragtag);
		}
		//_ get current location btn
		getCurrLocationBtn.setOnClickListener{
			KF_FRESHlocation();
		}
		// _ hiding view button
		hideViewBtn.setOnClickListener {
			if(locationHead.isVisible)
			{
				it.rotation = 180F;
				locationHead.visibility = View.GONE;
				locationCopy.visibility = View.VISIBLE;
			} else {
				it.rotation = 360f;
				locationHead.visibility = View.VISIBLE;
				locationCopy.visibility = View.INVISIBLE;
			}
		}
		// adding occurrence btn, the view must be in a navhost to get nav controller
		addBtn.setOnClickListener {
			if( ! appcache.getString(KONSTANT.lati_flag, "").isNullOrBlank()){
				var bund: Bundle = Bundle();
				var lati = appcache.getString(KONSTANT.lati_flag,"");
				bund.apply {
					putDouble(KONSTANT.lati_flag, lati!!.toDouble());
					putDouble(KONSTANT.logi_flag, appcache.getString(KONSTANT.logi_flag, "")!!.toDouble());
					putString(KONSTANT.username, appcache.getString(KONSTANT.username, ""));
					putString(KONSTANT.usergid, appcache.getString(KONSTANT.usergid, ""));
				}
				it.findNavController().navigate(R.id.action_wheelFragment_to_jollyCreationFragment, bund);
			}
			else {
				var actn = WheelFragmentDirections.actionWheelFragmentToJollyCreationFragment();
				it.findNavController().navigate(actn);
			}
		}
	}
	
	/**
	* * Good places to adding observer or listener callback
	 * adding observer on location text
	*/
	override fun onStart() {
		super.onStart();
		locationLiveDat.observe(requireActivity(), locationWatcher);
	}
	
	override fun onStop() {
		cancelTokSrc.cancel();
		childFragmentManager.clearFragmentResult(locationQuestKey);
		super.onStop();
	}
	
	//#region LOCATION REGION
	/**
	 * location permission requesting
	 * Failed == start permission launcher
	 */
	private fun CheckLOCATIONperm()
	{
		if(ContextCompat.checkSelfPermission(requireContext(), coar_perm) == PackageManager.PERMISSION_GRANTED
			&& ContextCompat.checkSelfPermission(requireContext(), fine_perm) == PackageManager.PERMISSION_GRANTED)
		{
			// start things
			KF_LOCATIONgranted();
		} else
		{
			locationPermLauncher.launch(arrayOf(coar_perm, fine_perm));
		}
	}
	/**
	 * generate location permission launcher
	 */
	private fun GetLOCATIONpermissionCB() : ActivityResultLauncher<Array<String>>
	{
		return registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions(), {
			var resu: Boolean = true;
			for(ele in it)
			{
				resu = resu && ele.value;
			}
			if(resu == true){
				// getting location request
				KF_LOCATIONgranted();
			} else{
				// show up fragment, that require permission
			}
		})
	}
	/**
	 * common things to do when location permission granted
	 */
	private fun KF_LOCATIONgranted()
	{
		KF_ESTIMATE_LOCATION();
	}
	
	@SuppressLint("MissingPermission")
	private fun KF_FRESHlocation()
	{
		var lowlocset = LocationSettingsRequest.Builder().addLocationRequest(lowPowerQuest!!);
		KF_CHECK_CLI_CONFIG(lowlocset.build(), LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
	}
	/**
	 * *location configuration launcher
	 */
	private fun KF_LOC_CONFIG_LAUNCHER() : ActivityResultLauncher<IntentSenderRequest>
	{
		return registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()){
			if(it.resultCode == Activity.RESULT_OK)
			{
				Log.d(TAG, "kf_SETTINGSlauncher: == CLIENT SETTINGS RETURN OK");
				KF_GET_LOCATION_WITH_LEVEL(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
			} else {
				Log.d(TAG, "kf_SETTINGSlauncher: == CLIENT SETTINGS RETURN FAILED");
				var snack = Snackbar.make(requireActivity().window.decorView.rootView, "Please allow required settings to get your current location", Snackbar.LENGTH_INDEFINITE);
				snack.setAction("OK"){
					KF_CHECK_CLI_CONFIG(LocationSettingsRequest.Builder().addLocationRequest(lowPowerQuest!!).build(), LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
					snack.dismiss();
				}.show();
			}
		}
	}
	/**
	* REQUEST CLIENT SETTINGS TO SATISFY THE LOCATION QUEST
	*/
	private fun KF_CHECK_CLI_CONFIG(setquest : LocationSettingsRequest, prior: Int)
	{
		cliSettings.checkLocationSettings(setquest)
			.addOnSuccessListener{
				Log.i(TAG, "KF_CHECK_CLI_CONFIG: = location setting satisfied");
				KF_GET_LOCATION_WITH_LEVEL(prior);
			}
			.addOnFailureListener {
				if(it is ResolvableApiException)
				{
					Log.w(TAG, "KF_CHECK_CLI_CONFIG: = location settings not satisfied");
					var inte = it.resolution; // -> intent of resolvable exception
					locConfLauncher.launch(IntentSenderRequest.Builder(inte).build());
				}
			}
	}
	/**
	* Returning current location
	 *@param the priority of request
	*/
	@SuppressLint("MissingPermission")
	private fun KF_GET_LOCATION_WITH_LEVEL(prior : Int) {
		Log.d(TAG, "KF_GET_LOCATION_WITH_LEVEL: = Getting location with accuracy");
		fulocation.getCurrentLocation(prior, cancelTokSrc.token)
			.addOnSuccessListener { loc ->
				if(loc != null)
				{
					currLocation = loc;
					//Log.d(TAG, "kf_FRESHlocation: == current location ${loc.accuracy} - ${loc.latitude} - ${loc.longitude}");
					KF_GET_ADDR_FROM_LATLNG(loc.latitude, loc.longitude, 1);
				} else {
					Log.w(TAG, "KF_FRESHlocation: = fresh location null so u may out of reach area" );
					Toast.makeText(requireContext(), "Cannot reach you at your current location. Please try again later", Toast.LENGTH_SHORT).show();
				}
			} .addOnFailureListener {
				cancelTokSrc.cancel();
				Log.e(TAG, "KF_GET_LOCATION: = Failed to get current location", it);
			}
	}
	/**
	* BATTERY AWARE LOCATION REQUEST
	 * this will be called one time, when starting the app to save data usage
	 * default, the value of text field is "Where are you?"
	*/
	@SuppressLint("MissingPermission")
	private fun KF_ESTIMATE_LOCATION(){
		fulocation.lastLocation.addOnSuccessListener {
			if(it != null){
				currLocation = it;
				viewLifecycleOwner.lifecycleScope.launch{
					KF_GET_ADDR_FROM_LATLNG(it.latitude, it.longitude, 1);
				}
			} else
			{
				Log.i(TAG, "KF_ESTIMATE_LOCATION: = LAST LOCATION NULL , TRY TO GET FROM CACHE");
				viewLifecycleOwner.lifecycleScope.launch {
					var latcache = appcache.getString(KONSTANT.lati_flag, "");
					if( !latcache.isNullOrBlank()){
						var lngcache = appcache.getString(KONSTANT.logi_flag, "");
						if( !lngcache.isNullOrBlank())
						{
							KF_GET_ADDR_FROM_LATLNG(latcache.toDouble(), lngcache.toDouble(), 1);
						}
					}
				}
			}
		}
	}
	
	/**
	* * HELPER FUNCTIONS THAT IS CALLED TO GET ADDRESSES FROM LATI AND LONGI
	*/
	private fun KF_GET_ADDR_FROM_LATLNG(lati: Double, lngi:Double, maxres: Int)
	{
		viewLifecycleOwner.lifecycleScope.launch {
			var addrlst = geocoder.getFromLocation(lati, lngi, maxres);
			Log.d(TAG, "KF_ESTIMATE_LOCATION: $addrlst[0]");
			addrlst[0]?.let {
				var kadd = Kadress(null, null, it.subLocality, it.locality, it.subAdminArea, it.adminArea,
					it.postalCode?.toInt(),it.countryName, it.latitude, it.longitude );
				locationLiveDat.value = kadd;
			}
		}
	}
	
	/**
	* ?REGISTER OBSERVER FOR LOCATION CHANGING
	 * set up location text display N copy text view
	 * refrest from database event at that location
	*/
	private val locationWatcher = Observer<Kadress>(){
		if(it.neighbor != null )
		{
			if(it.locality != null){
				locationText.text = "${it.neighbor}, ${it.locality}";
				locationCopy.text = locationText.text;
			} else {
				locationText.text = "${it.neighbor}";
				locationCopy.text = locationText.text;
			}
		} else {
			locationText.text = it.locality;
			locationCopy.text = locationText.text;
		}
		appcache.edit().apply {
			putString(KONSTANT.region_flag, locationText.text.toString());
			putString(KONSTANT.lati_flag, it.lati.toString());
			putString(KONSTANT.logi_flag, it.longi.toString());
			putLong(KONSTANT.cache_timestamp, System.currentTimeMillis());
			apply();
		}
	}
	//#endregion
	
	//#region RETURNING RESULT FROM LOCATION PICKUP FRAGEMENT
	fun KF_PICKED_PLACES_RETURN(resu : LocationResultContainer)
	{
		lifecycleScope.launch {
			when(resu){
				is LocationResultContainer.Success ->{
					
					var addlatlon = resu.latlng;
					var kadd = Kadress();
					for(comp in resu.addcompo.asList()){
						var type = comp.types.get(0);
						if(type.contains("neighborhood"))	kadd.neighbor = comp.name;
						if(type.contains("locality"))	kadd.locality = comp.name;
						if(type.contains("administrative_area_level_2"))	kadd.admin2 = comp.name;
						if(type.contains("administrative_area_level_1"))	kadd.admin1 = comp.name;
						if(type.contains("postal_code"))	kadd.zip = comp.name.toInt();
						if(type.contains("country"))	kadd.country = comp.name;
					}
					kadd.lati = addlatlon.latitude;
					kadd.longi = addlatlon.longitude;
					locationLiveDat.value = kadd;
					// TODO: refreshing event from server after location update
				}
				is LocationResultContainer.Error -> {
					Log.e(TAG,"KF_PICKED_PLACES_RETURN: = Error on Picking up the places ${resu.code} - ${resu.msg}");
				}
			}
		}
		
	}
	//#endregion
	/**
	* * A CONTAINER FOR THE RETURNING RESULT FROM AUTOCOMPLETE FRAGMENT
	*/
	sealed class LocationResultContainer{
		data class Success(var addcompo : AddressComponents, var latlng : LatLng): LocationResultContainer()
		data class Error(var msg:String?, var code:String?, var extra: Any?): LocationResultContainer()
	}
	
	
	companion object {
		const val locationQuestKey : String = "LOCATION REQUEST KEY";
		
		const val placesName : String = "LOCATION NAME";
		const val placesLati : String = "LOCATION LATITUDE";
		const val placesLongi : String = "LOCATION LONGITUDE";
	}
}