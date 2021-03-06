package desoft.studio.dewheel.DataKenter

import android.util.Log
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import desoft.studio.dewheel.kata.BriefFireEvent
import desoft.studio.dewheel.kata.FireEvent
import desoft.studio.dewheel.katic.KONSTANT
import desoft.studio.dewheel.local.Kevent
import desoft.studio.dewheel.local.Klocalbase
import desoft.studio.dewheel.local.Ksaved
import desoft.studio.dewheel.local.Kuser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.coroutines.cancellation.CancellationException

class RepoWheel (private val indaba : Klocalbase )
{
    private val TAG :String = "-des- +++ WHEEL REPOSITORY +++";

    //+    USER LOCAL REPO --------->>-------->>--------->>*** -->>----------->>>>
    suspend fun REPO_LOCAL_INSERT_USER(inuser : Kuser)
    {
        return withContext(Dispatchers.IO){
            Log.w(TAG, "REPO_LOCAL_INSERT_USER: ");
            indaba.kablesDao().InsertUser(inuser);
            //Log.w(TAG, "REPO_LOCAL_INSERT_USER: Row id that is inserted $res");
        }
    }

    suspend fun REPO_LOCAL_UPDATE_USER(inuer : Kuser)
    {
        withContext(Dispatchers.IO) {
            Log.w(TAG, "REPO_UPDATE_USER: ");
            indaba.kablesDao().UpdateUser(inuer);
        }
    }
    
    suspend fun REPO_LOCAL_FIND_USER(inid : String): Kuser?
    {
        return (indaba.kablesDao().FindUser(inid));
    }

    suspend fun REPO_LOCAL_GET_USER() : Kuser?
    {
        return indaba.kablesDao().GetUser();
    }
    
    suspend fun REPO_LOCAL_DELETE_ALL_USER()
    {
        indaba.kablesDao().DeleteAllUser();
    }
    //_    USER LOCAL REPO --------->>-------->>--------->>*** -->>----------->>>>
    // +        EVENT LOCAL REPO --------->>-------->>--------->>*** -->>----------->>>>

    suspend fun REPO_LOCAL_ADD_EVENT(evnt: Kevent) : Long
    {
        return withContext(Dispatchers.IO) {
            Log.w(TAG, "REPO_ADD_EVENT: ");
            indaba.kablesDao().InsertEvent(evnt);
        }
    }

    suspend fun REPO_LOCAL_DELETE_ALL_EVENT()
    {
        indaba.kablesDao().DeleteAllEvent();
    }
    // _        EVENT LOCAL REPO --------->>-------->>--------->>*** -->>----------->>>>
    // +        SAVED EVENT LOCAL REPO--------->>-------->>--------->>*** -->>----------->>>>
    suspend fun REPO_ADD_SAVED_EVNT(sevnt : Ksaved)
    {
        indaba.kablesDao().AddSavedEvnt(sevnt);
    }

    suspend fun REPO_GET_ALL_SAVED() : List<Ksaved>
    {
        return indaba.kablesDao().GetAllSaved();
    }

    suspend fun REPO_GET_SAVED(inid: String) : Ksaved
    {
        return indaba.kablesDao().FindSaved(inid);
    }

    suspend fun REPO_GET_SAVED_FROM(inadmin: String, inregion : String): List<Ksaved>
    {
        return indaba.kablesDao().ListSavedWith(inadmin, inregion);
    }

    suspend fun REPO_DELETE_SAVED_ID(inid : String)
    {
        indaba.kablesDao().DeleteWithId(inid);
    }
    // _        SAVED EVENT LOCAL REPO--------->>-------->>--------->>*** -->>----------->>>>
    // +        EVENT REMOTE REPO --------->>-------->>--------->>*** -->>----------->>>>

    private val fbdatabase = FirebaseDatabase.getInstance();

    suspend fun REPO_FB_GET_EVENTS(state: String, region: String) : Flow<BriefFireEvent>
    {
        return callbackFlow<BriefFireEvent> {
            var refe = fbdatabase.getReference(KONSTANT.evntFireDatabasePath).child(Locale.getDefault().country).child(state).child(region).orderByKey();
            var currlis = refe.addChildEventListener(object: ChildEventListener{
                    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                        snapshot.getValue(FireEvent::class.java)?.let {
                            var bit = BriefFireEvent(snapshot.key, it);
                            trySendBlocking(bit).onFailure { thrit ->
                                Log.w(TAG, "onChildAdded: EVENT FAILED TO SENT TO CONSUMER ${thrit?.message}");
                            }
                        }
                    }
                    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                        snapshot.getValue(FireEvent::class.java)?.let {
                            var bit = BriefFireEvent(snapshot.key, it)
                            trySendBlocking(bit).onFailure { thrit ->
                                Log.w(TAG, "onChildAdded: EVENT FAILED TO SENT TO CONSUMER ${thrit?.message}");
                            }
                        }
                    }
                    override fun onChildRemoved(snapshot: DataSnapshot) {
                        snapshot.getValue(FireEvent::class.java)?.let {
                            var bit = BriefFireEvent(snapshot.key, it)
                            trySendBlocking(bit).onFailure { thrit ->
                                Log.w(TAG, "onChildAdded: EVENT FAILED TO SENT TO CONSUMER ${thrit?.message}");
                            }
                        }
                    }
                    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                        snapshot.getValue(FireEvent::class.java)?.let {
                            var bit = BriefFireEvent(snapshot.key, it)
                            trySendBlocking(bit).onFailure { thrit ->
                                Log.w(TAG, "onChildAdded: EVENT FAILED TO SENT TO CONSUMER ${thrit?.message}");
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG,"onCancelled: CALLBACK FLOW EVENT DATABASE CANCELLED ${error.message}");
                        cancel(CancellationException("Cancel Exception", error.toException()));
                    }
                })
            awaitClose {
                Log.w(TAG, "REPO_FB_GET_EVENTS: Remove listener and stop the flow");
                refe.removeEventListener(currlis);
            }
        }
    }
    // _        EVENT REMOTE REPO --------->>-------->>--------->>*** -->>----------->>>>
}