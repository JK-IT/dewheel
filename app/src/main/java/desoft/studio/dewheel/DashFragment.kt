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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import desoft.studio.dewheel.Kontrol.WedaKontrol
import desoft.studio.dewheel.kata.FireUser
import desoft.studio.dewheel.katic.KONSTANT
import desoft.studio.dewheel.local.Kuser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    //_data control for view
    private val wedakontrol : WedaKontrol by activityViewModels { WedaKontrol.DataWheelKontrolFactory((requireActivity().application as Wapplication).repo) }
    private var userInRom : Boolean = false;

    private var fbuser : FirebaseUser? = null;
    private var fbstore : FirebaseFirestore?  = null;
    private var uiUser : Kuser? = null;
    private var appCache : SharedPreferences? = null;
    private lateinit var gooInOption : GoogleSignInOptions;
    private lateinit var gooInClient: GoogleSignInClient;
    private var gooInAcnt : GoogleSignInAccount? = null;
    private val gooLaunchin = KF_INTENT_LAUNCHER_CB();

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
    private lateinit var uiUserSetLocation:TextView;
    private lateinit var userfillinGrp: LinearLayout;
    private lateinit var uiOpenEditBtn: FrameLayout;
    private lateinit var uiUnlockEdtBtn: Button;

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
        Log.d(TAG, "onCreate: DASHBOARD");
        super.onCreate(savedInstanceState);
        uihandler = Handler(Looper.getMainLooper());
        appCache = activity?.getSharedPreferences(getString(R.string.app_cache_preference), Context.MODE_PRIVATE);
        gooInOption = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestId().requestEmail().build();
        gooInClient = GoogleSignIn.getClient(activity, gooInOption);

        fbuser = FirebaseAuth.getInstance().currentUser;
        fbstore= FirebaseFirestore.getInstance();
        wedakontrol.userLivedata.observe(this, userObserver);
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
        uiUserSetLocation = v.findViewById(R.id.dashboard_favorite_location);
        uiOpenEditBtn = v.findViewById(R.id.dashboard_header_edit_btn);
        uiUpdateBtn  = v.findViewById(R.id.dashboard_header_user_info_update_btn);
        uiUnlockEdtBtn = v.findViewById(R.id.dashboard_header_edt_unlock_btn);
        userfillinGrp = v.findViewById(R.id.dashboard_header_user_info_grp);

        uiUsernameLayout = v.findViewById(R.id.dashboard_header_user_name_layout);
        uiUsernameEdit = v.findViewById(R.id.dashboard_header_user_name_edt);
        uiUserGenderLayout= v.findViewById(R.id.dashboard_header_user_gender_layout);
        uiUserGenderEdit= v.findViewById(R.id.dashboard_header_user_gender_edt);
        uiUserSorientLayout= v.findViewById(R.id.dashboard_header_user_sexori_layout);
        uiUserSorientEdit= v.findViewById(R.id.dashboard_header_user_sexori_edt);
        uiUserTrailLayout= v.findViewById(R.id.dashboard_header_user_about_layout);
        uiUserTrailEdit= v.findViewById(R.id.dashboard_header_user_about_edt);

        uiOpenEditBtn.setOnClickListener {
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
                gooLaunchin.launch(inte);
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
                                        }
                                        .addOnFailureListener {
                                            Log.w(TAG, "UPLOADING USER ERROR", it);
                                            uihandler?.post {
                                                KF_SIMPLE_INFORM_DIALOG("Failed to update user information. Please try again later").show();
                                            }
                                        }
                                }
                            }
                        }
                        upding.invokeOnCompletion {
                            UI_TOGGLE_USER_FILLING_FIELDS(false);
                        }
                    }
                }
            }
        }
        uiUnlockEdtBtn.setOnClickListener {
            UI_TOGGLE_USER_FILLING_FIELDS(!(uiUpdateBtn.isEnabled)) ;
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
    * *             KF_INTENT_LAUNCHER_CB
     * . a callback on activity result
     * . write to database new sigin google acc
     * . update cache
     * . link current fb user with new google sigin
    */
    private fun KF_INTENT_LAUNCHER_CB() : ActivityResultLauncher<Intent>
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
    * *                     KF_SETUP_VERIFIED_UI
     * @param inpa: verified or not
    */
    private fun KF_SETUP_VERIFIED_UI()
    {
        viewLifecycleOwner.lifecycleScope.launch() {
            launch(iodis) {
                var kuser = Kuser(fbuser?.uid!!, gooInAcnt?.email, gooInAcnt?.id, gooInAcnt?.displayName);
                wedakontrol.VM_ADD_USER_LOCAL(kuser);
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
}