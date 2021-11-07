package desoft.studio.dewheel

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment



class LocationPickFragment(var paramctx : Context): BottomSheetDialogFragment()
{
	private val TAG: String = "-des [[-- LOCATION PICK FRAGMENT --]]";
	
	private lateinit var beha : BottomSheetBehavior<FrameLayout>;
	private lateinit var plaCli : PlacesClient;
	private lateinit var gooAutoView : AutocompleteSupportFragment;
	
	override fun onCreate(savedInstanceState : Bundle?) {
		super.onCreate(savedInstanceState);
		Places.initialize(requireContext(), BuildConfig.GOOG_KEY);
		plaCli = Places.createClient(requireContext());
	}
	override fun onCreateView(inflater : LayoutInflater, container : ViewGroup?, savedInstanceState : Bundle?) : View?
	{
		var v = inflater.inflate(R.layout.frag_location_pick, container, false);
		return v;
	}
	
	override fun onViewCreated(view : View, savedInstanceState : Bundle?) {
		super.onViewCreated(view, savedInstanceState);
		gooAutoView = childFragmentManager.findFragmentById(R.id.lopick_goog_autocomplete) as AutocompleteSupportFragment;
	}
	
	override fun onStart() {
		super.onStart();
		gooAutoView.setPlaceFields(listOf(Place.Field.ADDRESS_COMPONENTS, Place.Field.LAT_LNG));
		beha = (dialog as BottomSheetDialog).behavior;
		beha.apply {
			isFitToContents = false;
			state = BottomSheetBehavior.STATE_HALF_EXPANDED;
			isHideable = false;
			isGestureInsetBottomIgnored = false;
		}
	}
	
	companion object{
		const val fragtag : String = "LOCATION PICK FRAGMENT";
	}
}