package desoft.studio.dewheel

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AddressComponents
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import desoft.studio.dewheel.Kontrol.DataControl
import desoft.studio.dewheel.SubKlass.JollyRecyAdapter
import desoft.studio.dewheel.kata.Kadress
import desoft.studio.dewheel.kata.WheelJolly
import desoft.studio.dewheel.katic.KONSTANT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch
import java.util.*

private const val TAG :  String = "-des- [[== WHEEL FRAGMENT ==]]";

class WheelFragment() : Fragment()
{
	private val iodis = Dispatchers.IO;

	private val dataKontrol : DataControl by activityViewModels();
	private lateinit var appcache : SharedPreferences;
	
	private val locationPermLauncher: ActivityResultLauncher<Array<String>> = GetLOCATIONpermissionCB();
	private val coar_perm = android.Manifest.permission.ACCESS_COARSE_LOCATION;
	private val fine_perm = android.Manifest.permission.ACCESS_FINE_LOCATION;
	private var locationPermCheckFlag : Boolean = false;

	private lateinit var locationEnableSettings : Button;
	private lateinit var fulocation : FusedLocationProviderClient;
	private lateinit var lowPowerQuest : LocationRequest;
	private lateinit var accuQuest : LocationRequest;
	private lateinit var slowLocReq: LocationRequest;
	private lateinit var cliSettings : SettingsClient;
	private lateinit var geocoder : Geocoder;
	private var currLocation : Kadress? = null;
	private var locConfLauncher = KF_LOC_CONFIG_LAUNCHER();
	private lateinit var locationHead : LinearLayout;
	private lateinit var locationTitle : TextView;
	private lateinit var getCurrLocationBtn : TextView;
	private var areaPickLauncher = KF_GOOGLE_PLACES_COMPLETE_CB_REGISTER();
	private var pickedFields = listOf<Place.Field>(Place.Field.ADDRESS_COMPONENTS, Place.Field.LAT_LNG);
	
	private lateinit var hideViewBtn : ImageView;
	private lateinit var locationCopy : TextView;
	private lateinit var addBtn : ImageView;
	
	private lateinit var jolliesRecycler : RecyclerView;
	private lateinit var jolliesAdapter : JollyRecyAdapter;

	private lateinit var noLocationSetViewGroup : LinearLayout;
	private lateinit var refreshBtn : Button;
	private lateinit var noactiViewGroup : LinearLayout;
	private lateinit var noActivitycretButton : Button;
	
	private var fbuser : FirebaseUser? = null;
	
	private val cancelTokSrc : CancellationTokenSource = CancellationTokenSource();
	
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		Log.i(TAG, "onCreate: IS BEING CALLED");
		super.onCreate(savedInstanceState);
		Places.initialize(requireContext(), BuildConfig.GOOG_KEY);
		Places.createClient(requireContext());

		CheckLOCATIONperm();

		appcache = requireActivity().getSharedPreferences(getString(R.string.app_cache_preference), Context.MODE_PRIVATE);
		
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

		fbuser = FirebaseAuth.getInstance().currentUser;
	}
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
	                          savedInstanceState: Bundle?): View?
	{
		// Inflate the layout for this fragment
		var v = inflater.inflate(R.layout.frag_wheel, container, false);
		locationEnableSettings = v.findViewById(R.id.wheel_location_setting_btn);
		locationHead = v.findViewById(R.id.wheel_location_head);
		locationTitle = v.findViewById(R.id.wheel_location_tedit);
		getCurrLocationBtn = v.findViewById(R.id.wheel_get_current_location);
		hideViewBtn = v.findViewById(R.id.wheel_hide_view_btn);
		locationCopy = v.findViewById(R.id.wheel_location_copy);
		addBtn = v.findViewById(R.id.wheel_add_btn);
		noactiViewGroup = v.findViewById(R.id.wheel_no_events_grp);
		noLocationSetViewGroup = v.findViewById(R.id.wheel_no_location_prompt_grp);
		refreshBtn = v.findViewById(R.id.wheel_refresh_on_location_btn);
		noActivitycretButton = v.findViewById(R.id.wheel_no_activities_create_btn);
		jolliesRecycler = v.findViewById(R.id.wheel_jollies_display);

		return v;
	}
	
	/**
	 * *set up related view functions
	 */

	@OptIn(InternalCoroutinesApi::class)
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		KF_SETUP_VIEWS();
	}

	@OptIn(InternalCoroutinesApi::class)
	private fun KF_SETUP_VIEWS() {
		//_ location display name
		locationTitle.inputType = InputType.TYPE_NULL;
		locationTitle.setOnClickListener {
			Log.i(TAG, "KF_SETUP_VIEWS: = location edit is clicked");
			var inte = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, pickedFields)
				.setTypeFilter(TypeFilter.REGIONS)
				.setCountry(Locale.getDefault().country)
				.setHint("Enter zipcode or citi")
				.build(requireContext());
			areaPickLauncher.launch(inte);
		}
		//_ get current location btn
		getCurrLocationBtn.setOnClickListener{
			if(locationPermCheckFlag)
			{
				KF_FRESHlocation();
			} else {
				locationPermLauncher.launch(arrayOf(coar_perm, fine_perm));
			}
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
		if(fbuser?.isAnonymous == true || appcache.getBoolean(KONSTANT.user_upload_flag, false) == false)
		{
			addBtn.isEnabled = false;
		} else
		{
			addBtn.setOnClickListener {
				//KF_NAVIGATE_TO_JOLLY_CREATION();
			}
		}
		//_ location settings button -> go to per settings
		locationEnableSettings.setOnClickListener {
			var seti = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", requireActivity().packageName, null));
			startActivity(seti);
		}
		//_ setup refresh button
		refreshBtn.setOnClickListener {
			KF_GET_JOLLIES_ON_AREA();
		}
		//_ setup the no actvity -> create button
		noActivitycretButton.setOnClickListener {
			//KF_NAVIGATE_TO_JOLLY_CREATION();
		}
		// _ setup recyclerview
		jolliesAdapter = JollyRecyAdapter(requireContext(), jolliesRecycler, null);
		jolliesRecycler.layoutManager = LinearLayoutManager(requireContext());
		jolliesRecycler.adapter = jolliesAdapter;
	}
	
	/**
	* * Good places to adding observer or listener callback
	 * adding observer on live datas from view model
	 * picked location
	 * jolly
	*/
	@OptIn(InternalCoroutinesApi::class)
	override fun onStart()
	{
		Log.i(TAG, "onStart: IS BEING CALLED");
		super.onStart();
		Log.d(TAG, "onStart: ==>>> setup OBSERVER ON VIEW MODEL");

		dataKontrol.pickedLocation.observe(requireActivity(), locationWatcher);
		if(dataKontrol.pickedLocation.value == null)
		{
			Log.w(TAG, "onStart: >>>> value of location on view model is null, call estimation");
			KF_ESTIMATE_LOCATION();
		}

		dataKontrol.jolly.observe(requireActivity(), jollyWatcher);
	}
	
	override fun onStop() {
		cancelTokSrc.cancel();
		childFragmentManager.clearFragmentResult(locationQuestKey);
		super.onStop();
	}
	
	//#region LOCATION REGION
	/**
	 * location permission requesting
	 * Failed == show the location prompt view that tell users to pick a location
	 */
	private fun CheckLOCATIONperm()
	{
		if(ContextCompat.checkSelfPermission(requireContext(), coar_perm) == PackageManager.PERMISSION_GRANTED
			&& ContextCompat.checkSelfPermission(requireContext(), fine_perm) == PackageManager.PERMISSION_GRANTED)
		{
			locationPermCheckFlag = true;
		} else
		{
			locationPermCheckFlag = false;
		}
	}
	/**
	 * generate location permission launcher
	 */
	private fun GetLOCATIONpermissionCB() : ActivityResultLauncher<Array<String>>
	{
		return registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions(),{
			var resu: Boolean = true;
			for(ele in it)
			{
				resu = resu && ele.value;
			}
			if(resu == true){
				// getting location request
				locationPermCheckFlag = true;
				KF_FRESHlocation();
			} else{
				locationPermCheckFlag = false;
				noLocationSetViewGroup.visibility = View.VISIBLE;
				jolliesRecycler.visibility = View.GONE;
			}
		})
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
					KF_GET_ADDR_FROM_LATLNG(loc.latitude, loc.longitude, 1);
				} else {
					Log.w(TAG, "KF_FRESHlocation: = fresh location null so u may out of reach area" );
					Toast.makeText(requireContext(), "Cannot reach you at your current location. Please try again later", Toast.LENGTH_SHORT).show();
					KF_TOGGLE_VIEW_GRP(R.id.wheel_no_location_prompt_grp, View.VISIBLE);
				}
			} .addOnFailureListener {
				cancelTokSrc.cancel();
				Log.e(TAG, "KF_GET_LOCATION: = Failed to get current location", it);
					Toast.makeText(requireContext(), "Cannot get your current location. Please check location settings, internet connection and try again", Toast.LENGTH_LONG).show();
				KF_TOGGLE_VIEW_GRP(R.id.wheel_no_location_prompt_grp, View.VISIBLE);
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
					} else {
						KF_TOGGLE_VIEW_GRP(R.id.wheel_no_location_prompt_grp, View.VISIBLE);
					}
				}
			}
		}
	}
	
	/**
	* * HELPER FUNCTIONS THAT IS CALLED TO GET ADDRESSES FROM LATI AND LONGI
	*/
	private fun KF_GET_ADDR_FROM_LATLNG(inlati: Double, inlngi:Double, maxres: Int)
	{
		viewLifecycleOwner.lifecycleScope.launch {
			var addrlst = geocoder.getFromLocation(inlati, inlngi, maxres);
			Log.d(TAG, "KF_GET LOCATION FROM LAT/LNG: ${addrlst[0].locality}");
			if(addrlst.size != 0) {
				addrlst[0]?.let {
					var kadd = Kadress(null, null, it.subLocality, it.locality, it.subAdminArea, it.adminArea, it.postalCode?.toInt(),it.countryName, inlati, inlngi );
					dataKontrol.pickedLocation.value = kadd;
				}
			} else {
				var kadd = Kadress().also {
					it.lati = inlati; it.longi = inlngi;
				}
				dataKontrol.pickedLocation.value = kadd;
			}
		}
	}
	/**
	* ?REGISTER OBSERVER FOR LOCATION CHANGING
	 * set up location text display N copy text view
	 * saving region to appcache
	 * get jollies from database at that location
	 * disable the location prompt popup to display event result
	*/

	@OptIn(InternalCoroutinesApi::class)
	private val locationWatcher = Observer<Kadress>(){
		Log.d(TAG, "LOCATION Changed OBSERVER Callback is being CALLED");
		if(it != null)
		{
			KF_TOGGLE_VIEW_GRP(R.id.wheel_jollies_display, View.VISIBLE);
			currLocation = it; // -->> assign value of view model's location
			if(it.neighbor != null )
			{
				if(it.locality != null){
					locationTitle.text = "${it.neighbor}, ${it.locality}";
					locationCopy.text = locationTitle.text;
				} else {
					locationTitle.text = "${it.neighbor}";
					locationCopy.text = locationTitle.text;
				}
			} else {
				locationTitle.text = it.locality;
				locationCopy.text = locationTitle.text;
			}
			appcache.edit().apply {
				putString(KONSTANT.lati_flag, it.lati.toString());
				putString(KONSTANT.logi_flag, it.longi.toString());
				putLong(KONSTANT.cache_timestamp, System.currentTimeMillis());
				commit();
			}
			KF_GET_JOLLIES_ON_AREA();
		} else
			Log.w(TAG, "-->>> Location value on VIEW MODEL <<<--: IS NULL");
	}
	//#endregion

	//#region AUTOCOMPLETE PLACES ACTIVITY CALLBACK
	/**
	* ? register a callback for activity contract
	*/

	private fun KF_GOOGLE_PLACES_COMPLETE_CB_REGISTER() : ActivityResultLauncher<Intent>
	{
		return registerForActivityResult(ActivityResultContracts.StartActivityForResult(),{
			if(it.resultCode == AutocompleteActivity.RESULT_OK)
			{
				var pla = Autocomplete.getPlaceFromIntent(it.data);
				KF_PICKED_PLACES_RETURN(LocationResultContainer.Success(pla.addressComponents, pla.latLng));
			} else if(it.resultCode == AutocompleteActivity.RESULT_CANCELED)
			{
				Log.w(TAG, "KF_GOOGLE_PLACES_COMPLETE_CB_REGISTER: AUTOCOMPLETE IS CANCELED");
			}
			else {
				var stat = Autocomplete.getStatusFromIntent(it.data);
				Log.e(TAG, "KF_GOOGLE_PLACES_COMPLETE_CB_REGISTER: error === >>", Throwable("Error getting picked places from auto complete ${stat.statusCode} <-> ${stat.statusMessage}"));
			}
		})
	}

	/**
	* ? HANDLE RESULT COMING FROM LOCATION PICKED FRAGMENT
	*/
	fun KF_PICKED_PLACES_RETURN(resu : LocationResultContainer)
	{
		lifecycleScope.launch {
			when(resu){
				is LocationResultContainer.Success ->{
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
					//_handle cases like SAN PEDRO
					if(kadd.locality.isNullOrBlank())
					{
						var addrlst = geocoder.getFromLocation(resu.latlng.latitude, resu.latlng.longitude, 1);
						addrlst[0]?.let {
							kadd.locality = it.locality;
						}
					}
					kadd.lati = resu.latlng.latitude;
					kadd.longi = resu.latlng.longitude;
					Log.i(TAG, "KF_PICKED_PLACES_RETURN: SETTING VALUE OF CURRENT LOCATION on view model");
					dataKontrol.pickedLocation.value = kadd;
				}
				is LocationResultContainer.Error -> {
					Log.e(TAG,"KF_PICKED_PLACES_RETURN: = Error on Picking up the places ${resu.code} - ${resu.msg}");
				}
			}
		}

	}
	/**
	* ? A CONTAINER FOR THE RETURNING RESULT FROM AUTOCOMPLETE FRAGMENT
	*/
	sealed class LocationResultContainer{
		data class Success(var addcompo : AddressComponents, var latlng : LatLng): LocationResultContainer()
		data class Error(var msg:String?, var code:String?, var extra: Any?): LocationResultContainer()
	}
	//#endregion

	/**
	* ! --- CALLING VIEW MODEL TO GET DATA WHEN LOCATION IS AVAILABLE
	 * showing error view if there is error
	 * showing result as list
	*/


	@OptIn(InternalCoroutinesApi::class)
	private fun KF_GET_JOLLIES_ON_AREA()
	{
		Log.i(TAG, "KF_GET_JOLLIES_ON_AREA: >>>=== GETTING JOLLIES REFRESH AT ${currLocation?.locality}");
		jolliesAdapter.KF_CLEAR_EVENT();

		lifecycleScope.launch {
			dataKontrol.KF_VM_GET_JOLLIES_AT(currLocation!!);
		}
	}

	/**
	* ? JOLLY OBSERVER
	 *
	*/
	private val jollyWatcher = Observer<WheelJolly?>(){
		if(it != null )
		{
			//jolliesAdapter.KF_ADD_EVENT(it);
			KF_TOGGLE_VIEW_GRP(R.id.wheel_jollies_display, View.VISIBLE);
		}else {
			KF_TOGGLE_VIEW_GRP(R.id.wheel_no_events_grp, View.VISIBLE);
		}
	}

	/**
	* ! *** === HELPERS FUNCTIONS === ***
	 * */

	/**
	* ? SETUP LOCATION AND NAVIGATE TO CREATE JOLLY FRAGMENT
	*/
	/*private fun KF_NAVIGATE_TO_JOLLY_CREATION()
	{
		if(currLocation != null)
		{
			//Log.i(TAG, "KF_NAVIGATE_TO_JOLLY_CREATION: -- ;;; >> CURRENT LOCATION WHEN ADDING ACTIVITY $currLocation");
			var bund = Bundle();
			bund.apply {
				putDouble(KONSTANT.lati_flag, currLocation?.lati!!);
				putDouble(KONSTANT.logi_flag, currLocation?.longi!!);
			}
			findNavController().navigate(R.id.action_global_jollyCreationFragment, bund);
		} else
		{
			var actn = WheelFragmentDirections.actionGlobalJollyCreationFragment();
			findNavController().navigate(actn);
		}
	}*/

	/**
	* ? VIEW TOGGLING GROUP
	 * only works, if only 1 view is enabled at a time
	*/
	private fun KF_TOGGLE_VIEW_GRP(vint : Int, enable : Int)
	{
		when(vint){
			R.id.wheel_no_location_prompt_grp ->{
				noLocationSetViewGroup.visibility = enable;
				noactiViewGroup.visibility = View.GONE;
				jolliesRecycler.visibility = View.GONE;
			}
			R.id.wheel_no_events_grp -> {
				noactiViewGroup.visibility = enable;
				noLocationSetViewGroup.visibility = View.GONE;
				jolliesRecycler.visibility = View.GONE;
			}
			R.id.wheel_jollies_display -> {
				jolliesRecycler.visibility = enable;
				noactiViewGroup.visibility = View.GONE;
				noLocationSetViewGroup.visibility = View.GONE;
			}
		}
	}
	
	companion object {
		const val locationQuestKey : String = "LOCATION REQUEST KEY";
		
		const val placesName : String = "LOCATION NAME";
		const val placesLati : String = "LOCATION LATITUDE";
		const val placesLongi : String = "LOCATION LONGITUDE";
	}
}