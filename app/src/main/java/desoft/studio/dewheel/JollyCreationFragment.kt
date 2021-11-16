package desoft.studio.dewheel

import android.app.Activity
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
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import desoft.studio.dewheel.katic.KONSTANT
import java.text.SimpleDateFormat
import java.util.*


class JollyFragment : Fragment() {
	
	private val TAG : String = "-des- [[-- JOLLY CREATION FRAGMENT --]]";
	private var calen: Calendar = Calendar.getInstance();
	
	private var areaLati : Double? = null;
	private var areaLogi: Double? = null;
	private var username: String? = null;
	private var usergid : String? = null;
	
	private lateinit var jollyNameField : TextInputEditText;
	private lateinit var jollyNameLayout: TextInputLayout;
	private lateinit var timepicker : TextView;
	private var timeDialogBuilder: MaterialTimePicker.Builder = MaterialTimePicker.Builder().setTimeFormat(TimeFormat.CLOCK_12H)
		.setTitleText("Pick A Time").setHour(0).setMinute(15);
	private lateinit var dateDisplay : TextView;
	private lateinit var placePickDisplay: TextView;
	private lateinit var placeError: TextView;
	private lateinit var placeClient : PlacesClient;
	private lateinit var lookupmaps: TextView;
	private val fields = listOf<Place.Field>(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS_COMPONENTS, Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.PHOTO_METADATAS);
	private var autocompleteLauncher : ActivityResultLauncher<Intent> = KF_AUTOCOMPLETE_RESULT_CB();
	private var area : String = "";
	
	private lateinit var donebtn: Button;
	private lateinit var canbtn: Button;
	
	override fun onCreate(savedInstanceState : Bundle?) {
		super.onCreate(savedInstanceState);
		arguments?.let {
			areaLati = it.getDouble(KONSTANT.lati_flag);
			areaLogi = it.getDouble(KONSTANT.logi_flag);
			username= it.getString(KONSTANT.username);
			usergid = it.getString(KONSTANT.usergid);
		}
		
		//_ init Google Places Client
		Places.initialize(requireContext(), BuildConfig.GOOG_KEY);
		placeClient = Places.createClient(requireContext());
		
	}
	
	override fun onCreateView(inflater : LayoutInflater, container : ViewGroup?,savedInstanceState : Bundle?) : View? {
		// Inflate the layout for this fragment
		var v = inflater.inflate(R.layout.frag_jolly_creating, container, false);
		jollyNameField = v.findViewById(R.id.evnt_name_field);
		jollyNameLayout = v.findViewById(R.id.evnt_name_layout);
		timepicker = v.findViewById(R.id.evnt_picktime_btn);
		dateDisplay = v.findViewById(R.id.evnt_date_display);
		placePickDisplay = v.findViewById(R.id.evnt_addrpick_btn);
		placeError = v.findViewById(R.id.evnt_place_error);
		lookupmaps = v.findViewById(R.id.evnt_lookup_googlemaps);
		donebtn = v.findViewById(R.id.evnt_done_btn);
		canbtn = v.findViewById(R.id.evnt_cancel_btn);
		return v;
	}
	
	override fun onViewCreated(view : View, savedInstanceState : Bundle?) {
		//super.onViewCreated(view, savedInstanceState)
		KF_SETUP_VIEWS();
	}
	
	private fun KF_SETUP_VIEWS()
	{
		//_ get user time
		timepicker.setOnClickListener {
			timeDialogBuilder.build().also { tipicker ->
				tipicker.addOnPositiveButtonClickListener {
					Log.d(TAG, "KF_SETUP_VIEWS: = user picked ${tipicker.hour}, and ${tipicker.minute}");
					calen.set(Calendar.HOUR, tipicker.hour);
					calen.set(Calendar.MINUTE, tipicker.minute);
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
		placePickDisplay.setOnClickListener {
			val inte = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
				.setCountry(Locale.getDefault().country).setHint("Restaurants, bars, museums, hotels, stadiums...").build(requireContext());
			autocompleteLauncher.launch(inte);
		}
		//_open google maps
		lookupmaps.setOnClickListener {
			var gmminte : Uri? = null;
			if(areaLati != null)
			{
				gmminte = Uri.parse("geo:${areaLati.toString()},${areaLogi.toString()}?z=14f");
			} else
			{
				gmminte = Uri.parse("geo:0,0?z=16f&q=${Locale.getDefault().getDisplayCountry()}");
			}
			var mapinte = Intent(Intent.ACTION_VIEW, gmminte);
			mapinte.setPackage(KONSTANT.goo_package_name);
			startActivity(mapinte);
		}
		//_ setu pbutton
		// ! Done button, create jolly event
		donebtn.setOnClickListener {
			Log.i(TAG, "KF_SETUP_VIEWS: == DONE BUTTON IS CLICKED");
			if(jollyNameField.text.isNullOrBlank())
			{
				jollyNameLayout.error = "This field cannot be empty";
				return@setOnClickListener;
			} else {
				jollyNameLayout.error = null;
			}
			if (placePickDisplay.text.isNullOrBlank()){
				placeError.visibility = View.VISIBLE;
				return@setOnClickListener;
			} else {
				placeError.visibility = View.INVISIBLE;
			}
			var name: String = jollyNameField.text.toString()!!;
			var addr: String = placePickDisplay.text.toString()!!;
			var time: Long = calen.timeInMillis;
			(requireContext() as MainActivity).KF_UPLOAD_JOLLY(name, addr, area, time); //-> also navigate back to wheel fragment
		}
		canbtn.setOnClickListener {
			it.findNavController().navigate(R.id.action_jollyCreationFragment_to_wheelFragment);
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
			Log.i(TAG, "KF_AUTOCOMPLETE_RESULT_CB: == Pickup returning from autocomplete");
			when(it.resultCode){
				Activity.RESULT_OK ->{
					var re = Autocomplete.getPlaceFromIntent(it.data);
					Log.d(TAG, "KF_AUTOCOMPLETE_RESULT_CB: == user picks this places ${re.addressComponents}");
					if(area.isNotBlank()) area = "";
					for(typ in re.addressComponents.asList())
					{
						if(typ.types.get(0).contentEquals("locality")) {
								area = typ.name;
						}
					}
					var finish = "${re.name}\n${re.address}";
					placePickDisplay.text = finish;
				}
				AutocompleteActivity.RESULT_ERROR ->{
					var stat = Autocomplete.getStatusFromIntent(it.data);
					Log.e(
						TAG, "KF_AUTOCOMPLETE_RESULT_CB: = FAILED TO GET LOCATION ${stat.statusMessage}", RuntimeException("${stat.statusCode}"));
				}
				Activity.RESULT_CANCELED->{
					Log.w(TAG, "KF_AUTOCOMPLETE_RESULT_CB: == Activity pickup is canceled");
				}
			}
		}
	}
	
	companion object {
		/**
		 * Use this factory method to create a new instance of
		 * this fragment using the provided parameters.
		 *
		 * @param param1 Parameter 1.
		 * @param param2 Parameter 2.
		 * @return A new instance of fragment JollyFragment.
		 */
		@JvmStatic
		fun newInstance(param1 : String, param2 : String) =
			JollyFragment().apply {
				arguments = Bundle().apply {
				
				}
			}
		
		const val fragTag : String = "EVENT CREATOR FRAGMENT TAG";
		const val timeTag : String = "EVENT CREATOR TIME PICKER FRAGMENT TAG";
	}
}