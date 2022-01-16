package desoft.studio.dewheel

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AddressComponents
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.floatingactionbutton.FloatingActionButton
import desoft.studio.dewheel.Kontrol.WedaKontrol
import desoft.studio.dewheel.SubKlass.JollyRecyAdapter
import desoft.studio.dewheel.kata.BriefFireEvent
import desoft.studio.dewheel.katic.KONSTANT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import java.util.*

class EventListFragment : Fragment() {

    private val TAG = "-des- [[== EVENT LIST FRAGMENT ==]]";
    private lateinit var appCache : SharedPreferences;
    private val wedaKontrol: WedaKontrol by activityViewModels{WedaKontrol.DataWheelKontrolFactory((activity?.application as Wapplication).repo)};
    private var evntjob : Job? = null;


    private lateinit var placlient : PlacesClient;
    private val placeCompleteLauncher = KF_AUTO_LOCATION_LAUNCHIN();

    private var lastlocatet :String? = "";
    private var locaNeighborhood: String? = null;
    private var locaCity : String? = null;
    private var pickedComponent : AddressComponents? = null;

    private lateinit var evntlocationtet : TextView;
    private lateinit var locationfab : FloatingActionButton;

    private lateinit var recyview : RecyclerView;
    private lateinit var recyadapter : JollyRecyAdapter;

    /**
    * *                 onCreate
    */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var regiontet = arguments?.getString("regiontext");
        lastlocatet = regiontet;
        Log.d(TAG, "onCreate: RECEIVED ARGUMENT $regiontet");

        appCache = requireContext().getSharedPreferences(resources.getString(R.string.app_cache_preference), Context.MODE_PRIVATE);
        Places.initialize(requireContext(), BuildConfig.GOOG_KEY);
        placlient = Places.createClient(requireContext());

        lifecycleScope.launch {
            var state = appCache.getString(KONSTANT.userSavedState, "");
            repeatOnLifecycle(Lifecycle.State.STARTED){
                Log.i(TAG, "onCreate: REPEATE IS CALLED TO GET REMOTE EVENTS");
                //evntjob?.let { evntjob?.cancelAndJoin(); }
                evntjob = launch { wedaKontrol.VM_GET_REMOTE_EVENTS(state!!, lastlocatet!!); }
            }
        }
        wedaKontrol.livevnt.observe(this, evntObserver);
    }
    /**
    * *                     onCreateView
    */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.frag_event_list, container, false)
    }
    /**
    * *                     onViewCreated
    */
    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        super.onViewCreated(v, savedInstanceState);

        evntlocationtet = v.findViewById(R.id.evntlst_location_tet);
        evntlocationtet.text = lastlocatet;

        locationfab = v.findViewById(R.id.evntlst_set_location_fab);
        locationfab.setOnClickListener {
            var fielist = listOf< Place.Field>(Place.Field.ADDRESS_COMPONENTS);
            var autointe = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fielist).setTypeFilter(TypeFilter.REGIONS)
                .setCountry(Locale.getDefault().country).setHint("Enter zipcode or cities").build(requireContext());
            placeCompleteLauncher.launch(autointe);
        }

        recyview = v.findViewById(R.id.evntlst_recyview);
        recyview.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false );
        recyadapter = JollyRecyAdapter(requireContext(), recyview, wedaKontrol);
        recyview.adapter = recyadapter;

    }
    /**
    * *                     onStart
    */
    override fun onStart() {
        super.onStart();
        wedaKontrol.VM_GET_ALL_SAVED();
    }
    /**
    * *                     onStop
    */
    override fun onStop() {
        super.onStop();
        Log.i(TAG, "onStop: Event list Fragment on stope");
    }

    // + --------->>-------->>--------->>*** -->>----------->>>>
    /**
    * *         KF_AUTO_LOCATION_LAUNCHIN
     * . register for auto complete location
    */
    private fun KF_AUTO_LOCATION_LAUNCHIN() :ActivityResultLauncher<Intent>
    {
        return registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            when(it.resultCode) {
                Activity.RESULT_OK -> {
                    var res = Autocomplete.getPlaceFromIntent(it.data);
                    var admin1 : String = "";
                    var zip : String = "";
                    for(comp in res.addressComponents.asList()) {
                        if(comp.types[0].contentEquals("neighborhood")){
                            locaNeighborhood = comp.name;
                        }
                        if(comp.types[0].contentEquals("locality")) {
                            locaCity = comp.name;
                        }
                        if(comp.types[0].contentEquals("postal_code")) {
                            zip = comp.name;
                        }
                        if(comp.types[0].contentEquals("administrative_area_level_1")) {
                            if(comp.shortName.isNullOrBlank())
                                admin1 = comp.name;
                            else
                                admin1 = comp.shortName;
                        }
                    }
                    pickedComponent = res.addressComponents;
                    if( !locaNeighborhood.isNullOrBlank() && locaCity.isNullOrBlank()) {
                        evntlocationtet.text = locaNeighborhood;
                    }
                    else {
                        evntlocationtet.text = locaCity;
                    }
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        appCache.edit {
                            putString(KONSTANT.userSavedNeighborhood, locaNeighborhood);
                            putString(KONSTANT.userSavedCity, locaCity);
                            putString(KONSTANT.userSavedZip, zip);
                            putString(KONSTANT.userSavedState, admin1);
                            commit();
                        }
                    }
                    recyadapter.KF_CLEAR_EVENT();
                    lastlocatet = evntlocationtet.text.toString();
                    lifecycleScope.launch {
                        evntjob?.cancelAndJoin();
                        evntjob = launch { wedaKontrol.VM_GET_REMOTE_EVENTS(admin1, locaCity?: locaNeighborhood!!);}
                    }
                }
                Activity.RESULT_CANCELED -> {
                    Log.w(TAG, "KF_AUTO_LOCATION_LAUNCHIN: Activity failed");
                    KF_SIMPLE_DIALOG("Failed to connect to server, please try again");
                }
            }
        }
    }

    /**
    *               * evntObserver
     *  . observer for events on server
    */
    private var evntObserver = Observer<BriefFireEvent?>(){ evnit ->
        Log.i(TAG, "Fire Event Observer: Getting event $evnit");
        evnit?.let { recyadapter.KF_ADD_EVENT(evnit); }

    }
    /**
    * *                 KF_SIMPLE_DIALOG
    */
    private fun KF_SIMPLE_DIALOG(inmess : String)
    {
        var dia = AlertDialog.Builder(requireContext()).setMessage(inmess)
            .setPositiveButton("Ok"){diainte, key ->
                diainte.dismiss();
            }
        dia.create().show();
    }
}