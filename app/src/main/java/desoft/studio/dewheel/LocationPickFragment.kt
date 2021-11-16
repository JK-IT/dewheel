package desoft.studio.dewheel

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.*


class LocationPickFragment(var paramctx : Context): BottomSheetDialogFragment()
{
	private val TAG: String = "-des [[-- LOCATION PICK FRAGMENT --]]";
	
	private lateinit var beha : BottomSheetBehavior<FrameLayout>;
	private lateinit var plaCli : PlacesClient;
	private lateinit var gooAutoView : AutocompleteSupportFragment;
	private lateinit var errorview : TextView;
	
	/**
	 * initialize the places sdk
	 */
	override fun onCreate(savedInstanceState : Bundle?) {
		super.onCreate(savedInstanceState);
		Places.initialize(requireContext(), BuildConfig.GOOG_KEY);
		plaCli = Places.createClient(requireContext());
	}
	override fun onCreateView(inflater : LayoutInflater, container : ViewGroup?, savedInstanceState : Bundle?) : View?
	{
		var v = inflater.inflate(R.layout.frag_location_pick, container, false);
		errorview = v.findViewById(R.id.lopick_error);
		return v;
	}
	
	override fun onViewCreated(view : View, savedInstanceState : Bundle?) {
		super.onViewCreated(view, savedInstanceState);
		gooAutoView = childFragmentManager.findFragmentById(R.id.lopick_goog_autocomplete) as AutocompleteSupportFragment;
		KF_SETUP_GOOGLE_AUTOCOMPLETE();
	}
	
	override fun onStart() {
		super.onStart();
		beha = (dialog as BottomSheetDialog).behavior;
		beha.apply {
			isFitToContents = false;
			state = BottomSheetBehavior.STATE_HALF_EXPANDED;
			isHideable = false;
			isGestureInsetBottomIgnored = false;
		}
	}
	
	/**
	 * * set up google places autocomplete
	 * setup listener on auto complete
	 */
	private fun KF_SETUP_GOOGLE_AUTOCOMPLETE()
	{
		gooAutoView.setCountry(Locale.getDefault().country);
		gooAutoView.setPlaceFields(listOf(Place.Field.ADDRESS_COMPONENTS, Place.Field.LAT_LNG));
		gooAutoView.setTypeFilter(TypeFilter.REGIONS);
		gooAutoView.setOnPlaceSelectedListener(object: PlaceSelectionListener{
			override fun onError(stat : Status) {
				Log.e(TAG, "onError: Picking places from auto complete ${stat.statusMessage}");
				(parentFragment as WheelFragment).KF_PICKED_PLACES_RETURN(WheelFragment.LocationResultContainer.Error(stat.statusMessage, stat.statusCode.toString(), stat.status));
				dismiss();
			}
			override fun onPlaceSelected(picked : Place) {
				Log.i(TAG, "onPlaceSelected: = Picked places = === >>>>> ${picked.addressComponents} [[[[ ${picked.latLng} ]]]]]]");
				var valid : Boolean = false;
				for(comp in picked.addressComponents.asList())
				{
					var type = comp.types.get(0);
					if(type.contains("neighborhood")){
						valid = (valid.or(true));
					}
					if(type.contentEquals("locality")){
						valid =(valid or true);
					}
				}
				if(	!	valid) {
					errorview.text = "Please pick a city";
					errorview.visibility = View.VISIBLE;
				}else
				{
					errorview.visibility = View.INVISIBLE;
					if(parentFragment is WheelFragment)
					{
						(parentFragment as WheelFragment).KF_PICKED_PLACES_RETURN(WheelFragment.LocationResultContainer.Success(picked.addressComponents, picked.latLng));
						dismiss();
					}
				}
				
			}
		})
	}
	
	companion object{
		const val fragtag : String = "LOCATION PICK FRAGMENT";
	}
}