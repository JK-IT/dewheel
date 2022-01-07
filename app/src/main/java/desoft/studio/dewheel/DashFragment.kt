package desoft.studio.dewheel

import android.app.AlertDialog
import android.app.Dialog
import android.content.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AddressComponent
import com.google.android.libraries.places.api.model.AddressComponents
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import desoft.studio.dewheel.Kontrol.WedaKontrol
import desoft.studio.dewheel.SubKlass.WimePicker
import desoft.studio.dewheel.kata.FireEvent
import desoft.studio.dewheel.kata.FireUser
import desoft.studio.dewheel.katic.KONSTANT
import desoft.studio.dewheel.local.Kevent
import desoft.studio.dewheel.local.Kuser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.asTask
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * A simple [Fragment] subclass.
 * Use the [DashFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DashFragment : Fragment() {

    private val TAG = "-des- [[== DASHBOARD FRAGMENT ==]]";
    private val iodis = Dispatchers.IO;
    private val bgdis = Dispatchers.IO;
    private lateinit var rootview : View;
    private lateinit var placlient : PlacesClient;
    //_data control for view
    private val wedakontrol : WedaKontrol by activityViewModels { WedaKontrol.DataWheelKontrolFactory((requireActivity().application as Wapplication).repo) }
    private var userInRom : Boolean = false;

    private var fbuser : FirebaseUser? = null;
    private var fbstore : FirebaseFirestore = FirebaseFirestore.getInstance();
    private var fbdata : FirebaseDatabase = FirebaseDatabase.getInstance();
    private var uiUser : Kuser? = null;
    private var appCache : SharedPreferences? = null;
    private lateinit var gooInOption : GoogleSignInOptions;
    private lateinit var gooInClient: GoogleSignInClient;
    private var gooInAcnt : GoogleSignInAccount? = null;
    private val gooinLaunchin = KF_GOOIN_FOR_RESULT_CB();
    private val autoUserLaunchin = KF_USERLOCATION_FOR_RESULT_CB();
    private val autoEvntLaunchin = KF_EVNT_LOCATION_FOR_RESULT_CB();

    //.. ui variable
    private lateinit var uiUsername : TextView;
    private lateinit var uiVerifiedImg : ImageView;
    private lateinit var uiGooInBtn: Button;
    private lateinit var uiUsernameLayout : TextInputLayout;
    private lateinit var uiUsernameEdit: TextInputEditText;
    private lateinit var uiUserGenderLayout: TextInputLayout;
    private lateinit var uiUserGenderEdit: TextInputEditText;
    private lateinit var uiUserSorientLayout: TextInputLayout;
    private lateinit var uiUserSorientEdit: TextInputEditText;
    private lateinit var uiUserTrailLayout: TextInputLayout;
    private lateinit var uiUserTrailEdit: TextInputEditText;
    private lateinit var uiUpdateBtn : Button;
    private lateinit var uiUserLocationTet:TextView;
    private lateinit var uiUserLocationBtn: Button;
    private lateinit var userfillinGrp: LinearLayout;
    private lateinit var uiOpenUserFieldsBtn: FrameLayout;
    private lateinit var uiUnlockUserEdtBtn: Button;

    private lateinit var uiEvntTitleLout : TextInputLayout;
    private lateinit var uiEvntTitle : TextInputEditText;
    private lateinit var uiEvntAbout : TextInputEditText;
    private var uiEvntType : Int = KONSTANT.evntRegularType;
    private lateinit var uiEvntChipRegular : Chip;
    private lateinit var uiEvntChipInstant : Chip;
    private lateinit var uiEvntTime : TextView;
    private lateinit var uiEvntLocation: TextView;
    private lateinit var uiEvntCreateBtn : Button;
    private var evntCale = Calendar.getInstance();
    private var evntAddrcomponent : AddressComponents? = null;
    private var evntLatlng : LatLng? = null;

    private var uihandler : Handler? = null;
    private var fragStart : Boolean  = false;
    /**
    * *             onCreate
     * .setupu google sigin options, google sigin client
     * . getting current firebase user
     * . getting user info from room database
    */
    override fun onCreate(savedInstanceState: Bundle?)
    {
        Log.d(TAG, "onCreate: DASHBOARD ${Locale.getDefault().country}");
        super.onCreate(savedInstanceState);
        Places.initialize(requireContext(), BuildConfig.GOOG_KEY);
        placlient = Places.createClient(requireContext());

        uihandler = Handler(Looper.getMainLooper());
        appCache = activity?.getSharedPreferences(getString(R.string.app_cache_preference), Context.MODE_PRIVATE);
        gooInOption = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestId().requestEmail().build();
        gooInClient = GoogleSignIn.getClient(activity, gooInOption);

        fbuser = FirebaseAuth.getInstance().currentUser;
        wedakontrol.userLivedata.observe(this, userObserver);

        childFragmentManager.setFragmentResultListener(KONSTANT.timePickerReqKey, requireActivity()){ key, bund ->
            if(key == KONSTANT.timePickerReqKey) {
                var timilli = bund.getLong(KONSTANT.timeMilliSecBundleKey);
                evntCale.timeInMillis = timilli;
                var sdf = SimpleDateFormat("EEE, MMM dd 'at' hh:mm a", Locale.getDefault());
                var fomdate = sdf.format(evntCale.time);
                uihandler?.post {
                    uiEvntTime.text = fomdate;
                }
                Log.d(TAG, "FRAGMENT RETURN RESULT $timilli");
            }
        }
    }
    /**
    * *         onCreateView
    */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.frag_dash, container, false)
    }
/**
* *                 onViewCreated
 * . update ui with current user
*/
    override fun onViewCreated(v: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(v, savedInstanceState);
        rootview = v;

        uiUsername = v.findViewById(R.id.dashboard_username);
        uiVerifiedImg = v.findViewById(R.id.dashboard_verified_img);
        uiUserLocationTet = v.findViewById(R.id.dashboard_location_tet);
        uiUserLocationBtn = v.findViewById(R.id.dashboard_set_location_btn);
        uiOpenUserFieldsBtn = v.findViewById(R.id.dashboard_header_edit_btn);
        uiUpdateBtn  = v.findViewById(R.id.dashboard_header_user_info_update_btn);
        uiUnlockUserEdtBtn = v.findViewById(R.id.dashboard_header_edt_unlock_btn);
        userfillinGrp = v.findViewById(R.id.dashboard_header_user_info_grp);

        uiUsernameLayout = v.findViewById(R.id.dashboard_header_user_name_layout);
        uiUsernameEdit = v.findViewById(R.id.dashboard_header_user_name_edt);
        uiUserGenderLayout= v.findViewById(R.id.dashboard_header_user_gender_layout);
        uiUserGenderEdit= v.findViewById(R.id.dashboard_header_user_gender_edt);
        uiUserSorientLayout= v.findViewById(R.id.dashboard_header_user_sexori_layout);
        uiUserSorientEdit= v.findViewById(R.id.dashboard_header_user_sexori_edt);
        uiUserTrailLayout= v.findViewById(R.id.dashboard_header_user_about_layout);
        uiUserTrailEdit= v.findViewById(R.id.dashboard_header_user_about_edt);

        uiOpenUserFieldsBtn.setOnClickListener {
            if(userfillinGrp.isVisible) {
                userfillinGrp.visibility = View.GONE;
            }
            else {
                userfillinGrp.visibility = View.VISIBLE;
            }
        }
        uiGooInBtn = v.findViewById(R.id.dashboard_header_google_login_btn);
        var verified = appCache?.getBoolean(KONSTANT.userauthen, false);
        if(verified == true) {
            uiVerifiedImg.setColorFilter(resources.getColor(R.color.seed, null));
        } else {
            uiVerifiedImg.setColorFilter(resources.getColor(R.color.grey, null));
        }
        uiGooInBtn.isEnabled = !(verified?: false);
        uiGooInBtn.setOnClickListener {
            var inte = gooInClient.signInIntent;
            try {
                gooinLaunchin.launch(inte);
            } catch (err : ActivityNotFoundException) {
                Log.e(TAG, "GOOGLE INTNET ACTIITY NOT FOUND ",err);
            }
        }
        uiUpdateBtn.setOnClickListener {
            if(appCache?.getBoolean(KONSTANT.userauthen, false) == false || uiUser == null) {
                KF_SIMPLE_INFORM_DIALOG("Please loging in with Google account to continue").show();
            }
            else {
                // validate -> update to rom , upload to firestore
                if(KF_USER_FILL_VALIDATE()) {
                    uiUser?.let {
                        it.local_username = uiUsernameEdit.text.toString();
                        it.remote_username = (fbuser?.providerData?.get(1)?.displayName);
                        it.user_gender = uiUserGenderEdit.text.toString();
                        it.user_sorientation = uiUserSorientEdit.text.toString();
                        it.user_trails = uiUserTrailEdit.text.toString();
                    }
                    viewLifecycleOwner.lifecycleScope.launch() {
                        var upding = wedakontrol.VM_UPDATE_USER_LOCAL(uiUser!!);
                        uiUser?.let {
                            var remoteuser = FireUser(it.fuid, it.google_id, it.google_email, it.local_username, it.user_gender, it.user_sorientation, it.user_trails);
                            launch(bgdis) {
                                fbstore?.let {
                                    it.collection(KONSTANT.userFirestorePath).document(remoteuser.fid!!).set(remoteuser)
                                        .addOnSuccessListener {
                                            Toast.makeText(requireContext(), "Successfully Updating", Toast.LENGTH_SHORT).show();
                                            appCache?.edit {
                                                putBoolean(KONSTANT.useronstore, true);
                                                commit();
                                            }
                                            UI_TOGGLE_USER_FILLING_FIELDS(false);
                                        }
                                        .addOnFailureListener {
                                            Log.w(TAG, "UPLOADING USER ERROR", it);
                                            KF_SIMPLE_INFORM_DIALOG("Failed to update user information. Please try again later").show();
                                            appCache?.edit {
                                                putBoolean(KONSTANT.useronstore, false);
                                                commit();
                                            }
                                            UI_TOGGLE_USER_FILLING_FIELDS(true);
                                        }
                                }
                            }
                        }
                    }
                }
            }
        }
        uiUnlockUserEdtBtn.setOnClickListener {
            UI_TOGGLE_USER_FILLING_FIELDS(!(uiUpdateBtn.isEnabled)) ;
        }

        var caclocation = appCache?.getString(KONSTANT.locationRegion, "");
        if(!caclocation.isNullOrBlank()) {
            uiUserLocationTet.setText(caclocation);
        }
        uiUserLocationBtn.setOnClickListener {
            var plainte = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, listOf(Place.Field.ADDRESS_COMPONENTS))
                .setTypeFilter(TypeFilter.REGIONS).setCountry(Locale.getDefault().country)
                .setHint("Enter Citi or Zipcode")
                .build(requireContext());
            autoUserLaunchin.launch(plainte);
        }

        uiEvntTitleLout = v.findViewById(R.id.dashboard_evnt_title_lout);
        uiEvntTitle = v.findViewById(R.id.dashboard_evnt_title);
        uiEvntAbout = v.findViewById(R.id.dashboard_evnt_description);
        uiEvntChipRegular = v.findViewById(R.id.dashboard_chip_regular);
        uiEvntChipInstant = v.findViewById(R.id.dashboard_chip_instant);
        uiEvntTime = v.findViewById(R.id.dashboard_evnt_time);
        uiEvntLocation = v.findViewById(R.id.dashboard_evnt_location);
        uiEvntCreateBtn= v.findViewById(R.id.dashboard_evnt_create_btn);

        uiEvntTitle.doOnTextChanged { text, start, before, count ->
            //Log.d(TAG, "onTextChanged count $text $start $before $count ");
            if(uiEvntTitle.text.isNullOrBlank()) {
                v.findViewById<TextInputLayout>(R.id.dashboard_evnt_title_lout).error = "This field is required";
            } else {
                v.findViewById<TextInputLayout>(R.id.dashboard_evnt_title_lout).error = null;
            }
        }
        uiEvntChipInstant.setOnCheckedChangeListener { buttonView, isChecked ->
            if(isChecked) {
                uiEvntTime.isEnabled = false;
                uiEvntLocation.isEnabled = false;
                uiEvntType = KONSTANT.evntInstantType;
            }
        }
        uiEvntChipRegular.setOnCheckedChangeListener { buttonView, isChecked ->
            if(isChecked) {
                uiEvntTime.isEnabled = true;
                uiEvntLocation.isEnabled = true;
                uiEvntType = KONSTANT.evntInstantType;
            }
        }
        uiEvntTime.setOnClickListener {
            WimePicker().show(childFragmentManager, WimePicker.fragtag);
        }
        uiEvntLocation.setOnClickListener {
            var fielst = listOf(Place.Field.ADDRESS, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS_COMPONENTS);
            var autointe = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fielst)
                .setCountry(Locale.getDefault().country).build(requireContext());
            autoEvntLaunchin.launch(autointe);
        }
            // . put evnt to rom, upload to fb
        uiEvntCreateBtn.setOnClickListener {
            if(uiUser == null || (appCache?.getBoolean(KONSTANT.useronstore, false) == false)) {
                KF_SIMPLE_INFORM_DIALOG("Please log in as Google user and fill out information to continue").show();
            }
            else {
                if(KF_VALIDATE_EVT_FIELDS()) {
                    viewLifecycleOwner.lifecycleScope.launch(bgdis) {
                        var comlst = mutableMapOf<String, String>();
                        for(comp in evntAddrcomponent?.asList()!!) {
                            if(comp.types[0].contentEquals("neighborhood")) {
                                comlst["neighborhood"] =  comp.name;
                            }
                            if(comp.types[0].contentEquals("locality")) {
                                comlst["locality"] =  comp.name;
                            }
                            if(comp.types[0].contentEquals("administrative_area_level_1")) {
                                comlst["admin1"] = comp.name;
                            }
                            if(comp.types[0].contentEquals("postal_code")) {
                                comlst["zipcode"] =  comp.name;
                            }
                            if(comp.types[0].contentEquals("country")) {
                                comlst["country"] =  comp.name;
                            }
                        }
                        var evnk = Kevent(0, uiEvntTitle.text.toString(), uiEvntAbout.text.toString(), uiEvntType, uiEvntTime.text.toString(), evntCale.timeInMillis, uiEvntLocation.text.toString(), evntLatlng!!.latitude, evntLatlng!!.longitude,
                            comlst["locality"], comlst["neighborhood"], comlst["admin1"], comlst["zipcode"], comlst["country"]);

                        //var remoteupload =
                        var resid = async { wedakontrol.VM_ADD_EVENT(evnk); }
                        resid.asTask().addOnSuccessListener {
                            Log.i(TAG, "new evnt id ${it}");
                        }
                        async {
                            uiUser?.let {
                                var fireevnt = FireEvent(uiEvntTitle.text.toString(), (it.local_username?: it.remote_username)!!, it.fuid!!, uiEvntAbout.text.toString(),
                                uiEvntTime.text.toString(), uiEvntLocation.text.toString());
                                fbdata.getReference("events").child(Locale.getDefault().country).child(comlst["admin1"]!!).child(((comlst["locality"]?: comlst["neighborhood"]).toString())).child(System.currentTimeMillis().toString())
                                    .setValue(fireevnt).addOnSuccessListener {
                                        KF_CLEAR_EVNT_FIELDS();
                                        uihandler?.post {
                                            Toast.makeText(requireContext(), "Successful creating event", Toast.LENGTH_SHORT).show();
                                        }
                                    }.addOnFailureListener {
                                        Log.w(TAG, "EVent uploading failure ${it.message}");
                                        KF_SIMPLE_INFORM_DIALOG("Failed to upload event to server, please try again later");
                                    }
                            }
                        }.await();
                    }
                }
                else {
                    KF_SIMPLE_INFORM_DIALOG("Please fill out all event fields").show();
                }
            }
        }
    }

    /**
    * *                 onStart
    */
    override fun onStart()
    {
        Log.d(TAG, "onStart: DASHBOARD");
        super.onStart();
        fragStart = true;
        uiUser?.let {
            UI_UPDATE_UI(it);
        }
    }
    // + --------->>-------->>--------->>*** -->>----------->>>>

    /**
     * *                 KF_USER_FILL_VALIDATE
     * . checking and validate the user info fields
     */
    private fun KF_USER_FILL_VALIDATE() : Boolean
    {

        if(uiUsernameEdit.text.toString().isNullOrBlank() == true) {
            uiUsernameLayout.error = "Required Field";
        } else {
            uiUsernameLayout.error = null;
        }

        if(uiUserGenderEdit.text.toString().isNullOrBlank() == true) {
            uiUserGenderLayout.error = "Required Field";
        } else {
            uiUserGenderLayout.error = null;
        }

        if(uiUserSorientEdit.text.toString().isNullOrBlank() == true) {
            uiUserSorientLayout.error = "Required Field";
        } else {
            uiUserSorientLayout.error = null;
        }
        return !( (uiUsernameEdit.text.toString().isNullOrBlank()) || (uiUserGenderEdit.text.toString().isNullOrBlank()) || (uiUserSorientEdit.text.toString().isNullOrBlank()));
    }
    /**
     *   *               UI_TOGGLE_USER_FILLING_FIELDS
     *   . disable all user information filling fields
     */
    private fun UI_TOGGLE_USER_FILLING_FIELDS(enab: Boolean)
    {
        uiUsernameEdit.isEnabled = enab;
        uiUserGenderLayout.isEnabled = enab;
        uiUserSorientEdit.isEnabled = enab;
        uiUserTrailEdit.isEnabled = enab;
        uiUpdateBtn.isEnabled = enab;
    }
    /**
    * *         userObserver
     * . update ui with user info from room database
    */
    private val userObserver = Observer<Kuser?>{
        if(it == null) {
            Log.d(TAG,  " User Observer onChanged: USER IS NULL FROM ROOM DATABASE");
            userInRom = false;
        } else {
            Log.d(TAG, "User Observer onChanged: USER IS not null, $it");
            userInRom = true;
            uiUser = it;
            UI_UPDATE_UI(it);
        }
    }

    /**
    * *                 UI_UPDATE_UI
     * . update ui with user
    */
    private fun UI_UPDATE_UI(inusr : Kuser)
    {
        fragStart?.let {
            if(fragStart) {
                uihandler?.post {
                    inusr.remote_username?.let {
                        uiUsername.text= it;
                        uiUsernameEdit.setText(it);
                    }
                    inusr.local_username?.let{
                        uiUsername.text = it;
                        uiUsernameEdit.setText(it);
                    }
                    inusr.user_gender?.let {
                        uiUserGenderEdit.setText(it);
                    }
                    inusr.user_sorientation?.let {
                        uiUserSorientEdit.setText(it);
                    }
                    inusr.user_trails?.let {
                        uiUserTrailEdit.setText(it);
                    }
                }
            }
        }
    }
    /**
    *   *             KF_GOOIN_FOR_RESULT_CB
     * . a callback on activity result
     * . write to database new sigin google acc
     * . update cache
     * . link current fb user with new google sigin
    */
    private fun KF_GOOIN_FOR_RESULT_CB() : ActivityResultLauncher<Intent>
    {
        return registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            try {
                gooInAcnt = GoogleSignIn.getSignedInAccountFromIntent(it.data).getResult(ApiException::class.java);
                Log.i(TAG, "KF_INTENT_LAUNCHER_CB: gooInAcnt ${gooInAcnt?.id}");
                Log.d(TAG, "KF_SETUP_VERIFIED_UI: id token ${gooInAcnt?.idToken}");
                KF_SETUP_VERIFIED_UI();
            } catch (exc : ApiException) {
                Log.e(TAG, "KF_INTENT_LAUNCHER_CB: Error == $exc");
            }
        }
    }
    /**
    * *             KF_USERLOCATION_FOR_RESULT_CB
     * . callback for user favorite location
    */
    private fun KF_USERLOCATION_FOR_RESULT_CB() : ActivityResultLauncher<Intent>
    {
        return registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            var repla  = Autocomplete.getPlaceFromIntent(it.data);
            var sub = "";
            var region = "";
            var loctring = "";
            Log.d(TAG, "KF_AUTOPLACE_FOR_RESULT_CB: ${repla.addressComponents}");
            for(comp in repla.addressComponents.asList()) {
                if(comp.types[0].contentEquals("sublocality") || comp.types[0].contentEquals("neighborhood")) {
                    sub = comp.name;
                    loctring = comp.name;
                }
                if(comp.types[0].contentEquals("locality")) {
                    region = comp.name;
                    loctring = comp.name;
                }
            }

            uiUserLocationTet.setText("$loctring");
            appCache?.edit {
                putString(KONSTANT.locationSuborNei, sub);
                putString(KONSTANT.locationRegion, region);
                commit();
            }
            Log.d(TAG, "KF_AUTOPLACE_FOR_RESULT_CB: $repla");
        }
    }
    /**
    * *                             KF_EVNT_LOCATION_FOR_RESULT_CB
     * . callback for event location
    */
    private fun KF_EVNT_LOCATION_FOR_RESULT_CB() : ActivityResultLauncher<Intent>
    {
        return registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            var resupla = Autocomplete.getPlaceFromIntent(it.data);
            uihandler?.post {
                Log.i(TAG, "Event Location ${resupla.addressComponents}");
                uiEvntLocation.setText("${resupla.name}\n${resupla.address}");
                evntAddrcomponent = resupla.addressComponents;
                evntLatlng = resupla.latLng;
            }
        }
    }
    /**
    * *                     KF_SETUP_VERIFIED_UI
     * @param inpa: verified or not
    */
    private fun KF_SETUP_VERIFIED_UI()
    {
        viewLifecycleOwner.lifecycleScope.launch() {
            launch(iodis) {
                var kuser = Kuser(fbuser?.uid!!, gooInAcnt?.email, gooInAcnt?.id, gooInAcnt?.displayName);
                wedakontrol.VM_ADD_USER_LOCAL(kuser).invokeOnCompletion {
                    Log.i(TAG, "KF_SETUP_VERIFIED_UI: job Completion $it");
                    if(it==null) {
                        Log.d(TAG, "KF_SETUP_VERIFIED_UI: Done writing to room database");
                        uihandler?.post {
                            userInRom = true;
                        }
                    }
                }
            }
            launch {
                var cred = GoogleAuthProvider.getCredential(gooInAcnt?.idToken, null);
                fbuser?.linkWithCredential(cred)
                    ?.addOnSuccessListener {
                        fbuser = it.user;
                        var gooproder = fbuser?.providerData?.get(1);
                        uiVerifiedImg.setColorFilter(resources.getColor(R.color.seed, null));
                        uiGooInBtn.isEnabled = false;
                        appCache?.edit {
                            putBoolean(KONSTANT.userexist, true);
                            putBoolean(KONSTANT.userauthen, true);
                            putString(KONSTANT.useruid, fbuser?.uid);
                            putString(KONSTANT.usergid, gooproder?.uid);
                            putString(KONSTANT.usergmail, gooproder?.email);
                            putString(KONSTANT.username, gooproder?.displayName);
                            commit();
                        }
                        uiUser = Kuser(fbuser?.uid!!, gooproder?.email, gooproder?.uid, null, gooproder?.displayName);
                        uiUsername.text =  gooproder?.displayName;
                    }
                    ?.addOnFailureListener {
                        Log.w(TAG, "KF_VERIFIED_WITH_GOOGLE: ${it.message}");
                        uiVerifiedImg.setColorFilter(resources.getColor(R.color.grey, null));
                        uiGooInBtn.isEnabled = true;
                        Toast.makeText(requireContext(), "Cannot connect to server. Please try again later", Toast.LENGTH_LONG).show();
                    }

            }
        }
    }
    /**
    * *             KF_VALIDATE_EVT_FIELDS
     * .validate event fields
    */
    private fun KF_VALIDATE_EVT_FIELDS() : Boolean
    {
        return (!uiEvntTitle.text.isNullOrBlank()) && (!uiEvntTime.text.isNullOrBlank()) && (!uiEvntLocation.text.isNullOrBlank());
    }

    /**
    * *         KF_CLEAR_EVNT_FIELDS
    */
    private fun KF_CLEAR_EVNT_FIELDS()
    {
        uiEvntTitle.text = null;
        uiEvntTitleLout.error = null;
        uiEvntAbout.text = null;
        uiEvntType = KONSTANT.evntRegularType;
        uiEvntLocation.text = null;
        uiEvntTime.text = null;
    }

    /**
    * *             KF_SIMPLE_INFORM_DIALOG
     * . create simple informed dialog with message
    */
    private fun KF_SIMPLE_INFORM_DIALOG(inmess : String) : Dialog
    {
        return AlertDialog.Builder(requireContext())
            .setMessage(inmess)
            .setPositiveButton("Ok", object : DialogInterface.OnClickListener{
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    dialog?.dismiss();
                }
            })
            .create();
    }

}