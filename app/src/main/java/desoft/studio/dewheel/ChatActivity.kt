package desoft.studio.dewheel

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.CollapsingToolbarLayout
import desoft.studio.dewheel.kata.WheelJolly

class ChatActivity : AppCompatActivity() {
    private val TAG = "-des- <<++ CHAT ROOM ACTIVITY ++>>";

/**
* *     ONCREATE
 * SET UP DATA FROM BUNDLE
*/
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat);

        // _ get Intent bundle info
        var bdl = intent.getBundleExtra(chatIntentkey);
        if(bdl != null) {
            Log.d(TAG, "onCreate: ${bdl.getParcelable<WheelJolly>(chatJollykey)}");
        }
        // _ setup view
        KF_SETUP_VIEWS();
    }

    /**
    * * KF_SETUP_VIEWS
    */
    private fun KF_SETUP_VIEWS()
    {
        var tblayout = (findViewById<CollapsingToolbarLayout>(R.id.chat_collapse_tb));
        //tblayout.isTitleEnabled = false;
        //tblayout.title = "Kenlinra";

        setSupportActionBar(findViewById(R.id.chat_toolbar));
        supportActionBar?.apply {
            title = "Kiung";
            setDisplayShowHomeEnabled(true);
            setDisplayHomeAsUpEnabled(true);
            elevation = 5F;
        }
    }
/**
* *         ONSTART
 * CHECK USER AUTHENTICATION
 *
*/
    override fun onStart()
    {
        super.onStart()
    }

    
    // + --------->>-------->>--------->>*** -->>----------->>>>
    companion object
    {
        val chatIntentkey : String = "CHAT ROOM INFO INTENT KEY";
        val chatJollykey: String = " CHAT JOLLY BUNDLE KEY";
    }
}