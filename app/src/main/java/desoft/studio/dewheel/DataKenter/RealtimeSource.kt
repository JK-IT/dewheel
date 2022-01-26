package desoft.studio.dewheel.DataKenter

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import desoft.studio.dewheel.kata.Kadress
import desoft.studio.dewheel.kata.WheelJolly
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.transform

class RealtimeSource {

    private val TAG = "-des- ||==*** REALTIME DATABASE SOURCE *** ==||";

    private val fbauth : FirebaseAuth = FirebaseAuth.getInstance();
    private var fbuser : FirebaseUser;
    //_ chat room db reference
    var chatdb : DatabaseReference;

    // _ jolly database reference
    var jollydb : DatabaseReference;
    private var requestJollyDatRef : DatabaseReference? = null;
    private var jollySpot : Kadress? = null;
    //private var jollycb : Flow<>? = null;
    private var jollyChildListener : ChildEventListener? = null;

    init {
        fbuser = fbauth.currentUser!!;
        jollydb = Firebase.database.getReference(jollyRef)
        chatdb = Firebase.database.getReference(roomRef);
    }
// _ --------->>-------->>--------->>*** -->>----------->>>>

    //#region JOLLY INTERACTIVE FUNCTIONS SECTION
    suspend fun KF_GET_JOLLIES_AT(inaddress : Kadress)  : Flow<WheelJolly>
    {
        jollySpot = inaddress;
        requestJollyDatRef = jollydb.child(inaddress.admin1.toString()).child(inaddress.locality.toString());
        return callbackFlow {
            jollyChildListener = jollydb.child(inaddress.admin1.toString()).child(inaddress.locality.toString())
                .addChildEventListener(object : ChildEventListener {
                    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                        //Log.w(TAG, "onChildAdded: NEW EVENT IS ADDED ----> ::::: ${snapshot.key} and previous name $previousChildName");
                        trySend(snapshot).isSuccess;
                    }

                    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                        TODO("Not yet implemented")
                    }

                    override fun onChildRemoved(snapshot: DataSnapshot) {
                        TODO("Not yet implemented")
                    }

                    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                        TODO("Not yet implemented")
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }
                })
            awaitClose{
                Log.w(TAG, "KF_GET_JOLLIES_AT: == ;;;;  SOURCE REMOVE CHILD LISTENER , check if requestspot is null ${jollySpot==null} and listener is null ${jollyChildListener == null}");
                if(jollyChildListener != null && requestJollyDatRef != null)
                {
                    requestJollyDatRef?.removeEventListener(jollyChildListener!!);
                    jollySpot = null;
                    jollyChildListener = null;
                    requestJollyDatRef = null;
                }
            };
        }.buffer(128, BufferOverflow.SUSPEND)
        .transform { inval ->
            inval.getValue<WheelJolly>()?.let { emit(it) };
        }
        /*.onEach{
            Log.w(TAG, "KF_GET_JOLLIES_AT: ==;;;== REPO ${Thread.currentThread().name}");
        }*/
    }

    sealed class JollyState{
        data class Success(val jolly: WheelJolly): JollyState();
        data class Error (val exce: Throwable): JollyState();
    }
    //#endregion


    companion object
    {
        const val jollyRef : String = "jollies";
        const val roomRef: String = "chatrooms";
        const val roomsgRef : String = "roomsg";
    }
}