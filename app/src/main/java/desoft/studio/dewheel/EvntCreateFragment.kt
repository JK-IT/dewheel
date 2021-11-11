package desoft.studio.dewheel

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import desoft.studio.dewheel.katic.KONSTANT
import java.text.SimpleDateFormat
import java.util.*


class EvntCreateFragment(var parentctx : Context):BottomSheetDialogFragment() {
	private val TAG: String = "-des- [[--- EVENT CREATOR FRAGMENT -- ]]";
	
	private lateinit var timepicker : TextView;
	private lateinit var timeDialog: MaterialTimePicker.Builder;
	private lateinit var dateDisplay : TextView;
	private lateinit var placePick: TextView;
	private lateinit var placeClient : PlacesClient;
	private lateinit var lookupmaps: TextView;
	private val fields = listOf<Place.Field>(Place.Field.ID, Place.Field.NAME,Place.Field.ADDRESS_COMPONENTS, Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.PHOTO_METADATAS);
	private var autocompleteLauncher :ActivityResultLauncher<Intent> = KF_AUTOCOMPLETE_RESULT_CB();
	
	private lateinit var donebtn: Button;
	private lateinit var canbtn: Button;
	
	override fun onCreate(savedInstanceState : Bundle?) {
		super.onCreate(savedInstanceState);
		timeDialog = MaterialTimePicker.Builder().setTitleText("Pick a time")
			.setHour(7).setMinute(50).setTimeFormat(TimeFormat.CLOCK_12H);
		Places.initialize(requireContext(), BuildConfig.GOOG_KEY);
		placeClient = Places.createClient(requireContext());
	}
	
	override fun onCreateView(inflater : LayoutInflater, container : ViewGroup?, savedInstanceState : Bundle?) : View? {
		var v = inflater.inflate(R.layout.frag_evnt_dialog, container, false);
		timepicker = v.findViewById(R.id.evnt_picktime_btn);
		dateDisplay = v.findViewById(R.id.evnt_date_display);
		placePick = v.findViewById(R.id.evnt_addrpick_btn);
		lookupmaps = v.findViewById(R.id.evnt_lookup_googlemaps);
		donebtn = v.findViewById(R.id.evnt_done_btn);
		canbtn = v.findViewById(R.id.evnt_cancel_btn);
		return v;
	}
	
	override fun onViewCreated(view : View, savedInstanceState : Bundle?) {
		super.onViewCreated(view, savedInstanceState);
		KF_SETUP_VIEWS();
	}
	
	private fun KF_SETUP_VIEWS()
	{
		//_ get user time
		timepicker.setOnClickListener {
			timeDialog.build().also { tipicker ->
				tipicker.addOnPositiveButtonClickListener {
					Log.d(TAG, "KF_SETUP_VIEWS: = user picked ${tipicker.hour}, and ${tipicker.minute}");
					timepicker.text = SpannableStringBuilder("${tipicker.hour} : ${tipicker.minute}")
				}
				tipicker.addOnNegativeButtonClickListener {
					tipicker.dismiss();
				}
				tipicker.show(childFragmentManager, timeTag);
			}
		}
		//_ set up date
		var fm = SimpleDateFormat("MM/dd/yyyy");
		var kda = fm.format(Calendar.getInstance().time);
		dateDisplay.text = kda;
		//_setup autocomplete on locatio picker
		placePick.setOnClickListener {
			val inte = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
				.setCountry(Locale.getDefault().country).setHint("Restaurants, bars, museums, hotels, stadiums...").build(parentctx);
			autocompleteLauncher.launch(inte);
		}
		//_open google maps
		lookupmaps.setOnClickListener {
			var gmminte : Uri? = null;
			if(parentFragment is WheelFragment)
			{
				var loclatng = (parentFragment as WheelFragment).KF_GET_CURRENT_LATLNG();
				if(loclatng != null)
					gmminte = Uri.parse("geo:${loclatng.get(0)},${loclatng.get(1)}?z=12f");
			} else {
				gmminte = Uri.parse("geo:0,0?q=${Locale.getDefault().displayCountry}");
			}
			var mapinte = Intent(Intent.ACTION_VIEW, gmminte);
			mapinte.setPackage(KONSTANT.goo_package_name);
			startActivity(mapinte);
		}
		//_ setu pbutton
		donebtn.setOnClickListener {
		
		}
		canbtn.setOnClickListener {
			dismiss();
		}
	}
	
	override fun onStart() {
		super.onStart();
		var beha = (dialog as BottomSheetDialog).behavior;
		beha.apply {
			isGestureInsetBottomIgnored = false;
			state = BottomSheetBehavior.STATE_EXPANDED;
			isHideable = false;
			isCancelable = false;
			isDraggable = false;
		}
	}
	
	/**
	* * Activity launcher function
	 * using autocomplete class to get data from intent returns
	 * AUTOCOMPLETEACTIVITY.RESULT_ERROR TO CATCHE ERROR
	*/
	private fun KF_AUTOCOMPLETE_RESULT_CB() : ActivityResultLauncher<Intent>
	{
		return registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
			when(it.resultCode){
				Activity.RESULT_OK ->{
					var re = Autocomplete.getPlaceFromIntent(it.data);
					Log.d(TAG, "KF_AUTOCOMPLETE_RESULT_CB: == user picks this places ${re.name}");
					var finish = "${re.name}\n${re.address}";
					placePick.text = finish;
				}
				AutocompleteActivity.RESULT_ERROR ->{
					var stat = Autocomplete.getStatusFromIntent(it.data);
					Log.e(
						TAG, "KF_AUTOCOMPLETE_RESULT_CB: = FAILED TO GET LOCATION ${stat.statusMessage}", RuntimeException("${stat.statusCode}"));
				}
			}
		}
	}
	companion object {
		const val fragTag : String = "EVENT CREATOR FRAGMENT TAG";
		const val timeTag : String = "EVENT CREATOR TIME PICKER FRAGMENT TAG";
	}
}