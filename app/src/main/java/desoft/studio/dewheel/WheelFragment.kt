package desoft.studio.dewheel

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.text.InputType
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import java.util.*

private const val TAG :  String = "-des- [[== WHEEL FRAGMENT ==]]";

class WheelFragment : Fragment()
{
	private val locationPermLauncher: ActivityResultLauncher<Array<String>> = GetLOCATIONpermissionCB();
	private val coar_perm = android.Manifest.permission.ACCESS_COARSE_LOCATION;
	private val fine_perm = android.Manifest.permission.ACCESS_FINE_LOCATION;
	private lateinit var fulocation : FusedLocationProviderClient;
	private lateinit var lowPowerQuest : LocationRequest;
	private lateinit var accuQuest : LocationRequest;
	private lateinit var slowLocReq: LocationRequest;
	private lateinit var cliSettings : SettingsClient;
	private lateinit var geocoder : Geocoder;
	private var locConfLauncher = KF_LOC_CONFIG_LAUNCHER();
	private lateinit var locationEdit : TextInputEditText;
	private var currLocation : Location? = null;
	
	private val cancelTokSrc : CancellationTokenSource = CancellationTokenSource();
	
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState);
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
	}
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
	                          savedInstanceState: Bundle?): View?
	{
		// Inflate the layout for this fragment
		var v = inflater.inflate(R.layout.frag_wheel, container, false);
		locationEdit = v.findViewById(R.id.wheel_location_tedit);
		KF_SETUP_VIEWS();
		return v;
	}
	
	private fun KF_SETUP_VIEWS() {
		locationEdit.inputType = InputType.TYPE_NULL;
		locationEdit.setOnClickListener {
			Log.i(TAG, "KF_SETUP_VIEWS: = location edit is clicked");
			KF_FRESHlocation();
		}
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?)
	{
		CheckLOCATIONperm();
	}
	
	override fun onStop() {
		cancelTokSrc.cancel();
		super.onStop();
	}
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
	private fun KF_GET_LOCATION_WITH_LEVEL(prior : Int)
	{
		Log.d(TAG, "KF_GET_LOCATION_WITH_LEVEL: = Getting location with accuracy");
		fulocation.getCurrentLocation(prior, cancelTokSrc.token)
			.addOnSuccessListener { loc ->
				if(loc != null)
				{
					Log.d(TAG, "kf_FRESHlocation: == current location ${loc.accuracy} - ${loc.latitude} - ${loc.longitude}");
					currLocation = loc;
					if(Geocoder.isPresent()) {
						var laddr = geocoder.getFromLocation(loc.latitude, loc.longitude, 1);
						if (laddr.isNotEmpty())
							locationEdit.text = SpannableStringBuilder(laddr[0].locality);
					}
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
				if(Geocoder.isPresent()) {
					var laddr = geocoder.getFromLocation(it.latitude, it.longitude, 1);
					if (laddr.isNotEmpty())
						locationEdit.text = SpannableStringBuilder(laddr[0].locality);
				}
			} else
				Log.i(TAG, "KF_ESTIMATE_LOCATION: = LAST LOCATION NULL");
		}
	}

}