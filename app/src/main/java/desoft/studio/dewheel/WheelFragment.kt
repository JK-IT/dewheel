package desoft.studio.dewheel

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource

private const val TAG :  String = "-des- [[== WHEEL FRAGMENT ==]]";

class WheelFragment : Fragment()
{
	private val locationPermLauncher: ActivityResultLauncher<Array<String>> = GetLOCATIONpermissionCB();
	private val coar_perm = android.Manifest.permission.ACCESS_COARSE_LOCATION;
	private val fine_perm = android.Manifest.permission.ACCESS_FINE_LOCATION;
	private lateinit var fulocation : FusedLocationProviderClient;
	private lateinit var slowLocReq: LocationRequest;
	private lateinit var cliSettings : SettingsClient;
	private val cancelTokSrc : CancellationTokenSource = CancellationTokenSource();
	
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState);
		fulocation = LocationServices.getFusedLocationProviderClient(requireContext());
		cliSettings = LocationServices.getSettingsClient(requireContext());
		slowLocReq = LocationRequest.create().apply {
			interval = 1800000;
			fastestInterval = 60000;
			priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
		}
	}
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
	                          savedInstanceState: Bundle?): View?
	{
		// Inflate the layout for this fragment
		var v = inflater.inflate(R.layout.frag_wheel, container, false);
		return v;
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?)
	{
		CheckLOCATIONperm();
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
			kf_LOCATIONgranted();
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
				kf_LOCATIONgranted();
			} else{
				// show up fragment, that require permission
			}
		})
	}
	/**
	 * common things to do when location permission granted
	 */
	private fun kf_LOCATIONgranted()
	{
		kf_FRESHlocation();
	}
	/**
	 * Generate location request with low power consumption and low accuracy, using wifi or network
	 */
	private fun kf_LOWpowLOCATIONrequest(){
	
	}
	
	/**
	 * Lazily getting location of devices
	 * get last location available, if failed -> do nothing, if null -> get current location
	 */
	@SuppressLint("MissingPermission")
	private fun kf_LASTlocation()
	{

		fulocation.lastLocation.addOnSuccessListener {
			if(it == null)
			{
				Log.w(TAG, "kf_LAZILYgetLOCATION: == LAST LOCATION IS NULL");
			} else {
				//todo: do sthing to ui
			}
		}
	}
	
	@SuppressLint("MissingPermission")
	private fun kf_FRESHlocation()
	{
		var powerlocation = LocationRequest.create()?.apply {
			priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
			interval = 1800000;
		}
		var locset = LocationSettingsRequest.Builder().addLocationRequest(powerlocation!!);
		cliSettings.checkLocationSettings(locset.build()).addOnSuccessListener {
			fulocation.getCurrentLocation(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY, cancelTokSrc.token).addOnSuccessListener {
				if(it != null)
				{
					Log.d(TAG, "kf_FRESHlocation: == current location ${it.latitude}");
				}
			}
		}
			.addOnFailureListener {
				if(it is ResolvableApiException)
				{
					//start intent
				}
			}
	}
	/**
	 * generate setting launcher
	 */
	private fun kf_SETTINGSlauncher() : ActivityResultLauncher<IntentSenderRequest>
	{
		return registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()){
			if(it.resultCode == Activity.RESULT_OK)
			{
				Log.d(TAG, "kf_SETTINGSlauncher: == CLIENT SETTINGS RETURN OK");
			} else {
				Log.d(TAG, "kf_SETTINGSlauncher: == CLIENT SETTINGS RETURN FAILED");
			}
		}
	}
	/**
	 * TODO HOW TO TURN OFF/SAVE POWER WHEN APP GO TO BACKGROUND ON LOCATION REQUEST
	 */
}