package desoft.studio.dewheel

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import desoft.studio.dewheel.Kontrol.DataControl
import desoft.studio.dewheel.kata.Kmessage
import desoft.studio.dewheel.kata.WheelJolly
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {
    private val TAG = "-des- <<++ CHAT ROOM ACTIVITY ++>>";
    private val iodis = Dispatchers.IO;

    private var jollyta: WheelJolly? = null;
    private var roomid : String? = null;

    private var fbuser: FirebaseUser? = null;

    private var kiewmodel : DataControl? = null;

    private var appisOnline : Boolean = false;

    /**
     * *                    ONCREATE
     * CHECK USER AUTHENTICATION
     * SET UP DATA FROM BUNDLE
     * REGISTER OBSERVER FOR CONNECTION STATE WITH REALTIME DATABASE
     * REGISTER FOR MSG WATCHER ON DATABASE
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat);
        //_ check if user is illegal
        KF_CHECK_USER_AUTHENTICATION();
        // _ get Intent bundle info
        var bdl = intent.getBundleExtra(chatIntentkey);
        if (bdl != null) {
            Log.d(TAG, "onCreate: ${bdl.getParcelable<WheelJolly>(chatJollyBundlekey)}");
            jollyta = bdl.getParcelable<WheelJolly>(chatJollyBundlekey);
            roomid = bdl.getString(chatJollyRoomKey);
        }
        // _ setup view
        KF_SETUP_VIEWS();

        lifecycleScope.launch {
        // _ register firebase server connection observer when application is at least started state
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                kiewmodel?.infostate?.addValueEventListener(fbLinkWatcher);
            }
        }
        //_ register for msg reference on database
        kiewmodel?.romLiveMsg?.observe(this, msgWatcher);
    }

    /**
     * *            KF_SETUP_VIEWS
     * @ setup collapsing toolbar
     * ----> assign title of chat room,  name from wheel jolly
     */
    private fun KF_SETUP_VIEWS() {
        setSupportActionBar(findViewById(R.id.chat_toolbar));
        supportActionBar?.apply {
            title = jollyta?.creator;
            setDisplayShowHomeEnabled(true);
            setDisplayHomeAsUpEnabled(true);
            elevation = 5F;
        }

        var tblayout = (findViewById<CollapsingToolbarLayout>(R.id.chat_collapse_tb));
        tblayout.isTitleEnabled = false;
        //tblayout.title = jollyta?.creator;
    }

    /** *                ONSTART
     * . check if user is error
     * . check if app is connected to database
     * . send null msg to room for other one can parsing msg and get info from you
     */
    override fun onStart() {
        super.onStart();
        if(appisOnline) {
            if(fbuser == null || fbuser!!.isAnonymous == true)
            {
                Log.i(TAG, "onStart: USER IS NULL ${fbuser == null} OR ANONYMOUS ${fbuser?.isAnonymous == true}");
                KF_BACKTO_GATE();
            }
        }
    }

    /**
     * *            KF_CHECK_USER_AUTHENTICATION
     * . check if user is login , != null
     * . check if user is anonymous
     * . setup view model if user != null
     * . setup viewmodel with user from database
     */
    private fun KF_CHECK_USER_AUTHENTICATION() {
        var backtogate: Boolean = false;
        if (Firebase.auth.currentUser != null) {
            if (Firebase.auth.currentUser!!.isAnonymous != true) {
                fbuser = Firebase.auth.currentUser!!;
                kiewmodel = DataControl(application);
                kiewmodel!!.KF_VM_SETUP_USER_FROM_FIREBASE();
            } else {
                backtogate = true;
            }
        } else {
            backtogate = true;
        }
        if (backtogate) KF_BACKTO_GATE();
    }

    /**
     * *                        KF_BACKTO_GATE
     * RETURN USER TO GATE ACTIVITY, IF FIREBASE USER IS NULL
     */
    private fun KF_BACKTO_GATE()
    {
        Log.i(TAG, "KF_BACKTO_GATE: is called");
        var inte = Intent(this, GateActivity::class.java);
        inte.apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK;
        }
        startActivity(inte);
        finishAndRemoveTask();
    }

    // + --------->>-------->>--------->>*** -->>----------->>>>

    /**
    * *                     fbLinkWatcher
     * ! firebase connection state observer
     * . if connected -> send msg to the room on server
    */
    private val fbLinkWatcher  = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val state = snapshot.getValue(Boolean::class.java) ?: false;
            if(state) {
                Log.d(TAG, "onDataChange: CONNTECTED TO Firebase DATABAE and ROOM ID $roomid");
                appisOnline = true;
                roomid?.let { kiewmodel?.KF_VM_SEND_DUMMY_MSG(it) };
            }
            else {
                Log.d(TAG, "onDataChange: NO CONNECTION TO DATABASE");
                appisOnline = false;
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Log.i(TAG, "onCancelled: FB LINK WATCHER IS CANCELLED");
        }
    }

    /**
    * *             msgWatcher
     * ! watcher for live message on server
    */
    private val msgWatcher = object: Observer<Kmessage>{
        override fun onChanged(t: Kmessage?) {
            Log.i(TAG, "msgWatcher :  Data change on live msg $t");
        }

    }

    // + --------->>-------->>--------->>*** -->>----------->>>>
    companion object
    {
        val chatIntentkey : String = "CHAT ROOM INFO INTENT KEY";
        val chatJollyBundlekey: String = " CHAT JOLLY BUNDLE KEY";
        val chatJollyRoomKey: String = "CHAT ROOM ID KEY";
    }
}