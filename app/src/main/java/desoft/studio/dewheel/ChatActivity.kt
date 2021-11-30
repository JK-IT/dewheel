package desoft.studio.dewheel

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import desoft.studio.dewheel.kata.WheelJolly

class ChatActivity : AppCompatActivity() {
    private val TAG = "-des- <<++ CHAT ROOM ACTIVITY ++>>";

/**
* *     ONCREATE
 * SET UP DATA FROM BUNDLE
*/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat);

        // _ get Intent bundle info
        var bdl = intent.getBundleExtra(chatIntentkey);
        if(bdl != null) {
            Log.d(TAG, "onCreate: ${bdl.getParcelable<WheelJolly>(chatJollykey)}");
        }

    }
/**
* *         ONSTART
 * CHECK USER AUTHENTICATION
 *
*/
    override fun onStart() {
        super.onStart()
    }

    
    // + --------->>-------->>--------->>*** -->>----------->>>>
    companion object {
        val chatIntentkey : String = "CHAT ROOM INFO INTENT KEY";
        val chatJollykey: String = " CHAT JOLLY BUNDLE KEY";
    }
}