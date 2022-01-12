package desoft.studio.dewheel.Kontrol

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import desoft.studio.dewheel.kata.FireEvent
import desoft.studio.dewheel.local.Kevent
import desoft.studio.dewheel.local.Kuser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis

class WedaKontrol(private val repo: RepoWheel) :  ViewModel()
{
    private val TAG = "-des- k-- WEDA CONTROL ( VIEW MODEL) -->l"
    private val iodis = Dispatchers.IO;

    val userLivedata : MutableLiveData<Kuser?> = MutableLiveData<Kuser?>();

    init {
        Log.w(TAG, "Init: ViewMODEL IS CREATED" );
    }

    // + USER DATA REPO CONTROL--------->>-------->>--------->>*** -->>----------->>>>
    /**
    * *             VM_ADD_USER_LOCAL
    */
    fun VM_ADD_USER_LOCAL(inuser : Kuser): Job
    {
        // using withcontext as an alternative for aync/wait
        return viewModelScope.launch {
            repo.REPO_LOCAL_INSERT_USER(inuser);
        }
    }
    /**
    * *             VM_UPDATE_USER_LOCAL
     * . update current existing user
    */
    fun VM_UPDATE_USER_LOCAL(inser : Kuser) : Job
    {
        return viewModelScope.launch {
            repo.REPO_LOCAL_UPDATE_USER(inser);
        }
    }

    /**
    * *         VM_FIND_USER_LOCAL
    */
    fun VM_FIND_USER_LOCAL(inid : String)
    {
        viewModelScope.launch() {
            var resuser  = repo.REPO_LOCAL_FIND_USER(inid);
            Log.w(TAG, "VM_FIND_USER: found user $resuser" );
            userLivedata.value = resuser?: null;
        }
    }

    /**
    * *             VM_DELETE_ALL_USER_LOCAL
    */
    fun VM_DELETE_ALL_USER_LOCAL()
    {
        viewModelScope.launch(iodis) {
            repo.REPO_LOCAL_DELETE_ALL_USER();
        }
    }

    // + EVENT DATA REPO CONTROL--------->>-------->>--------->>*** -->>----------->>>>

    /**
    * *             VM_ADD_EVENT_LOCAL
    */
    suspend fun VM_ADD_EVENT_LOCAL(evnk : Kevent): Long
    {
        return repo.REPO_LOCAL_ADD_EVENT(evnk);
    }

    /**
    * *             VM_DELETE_ALL_EVENT_LOCAL
    */
    fun VM_DELETE_ALL_EVENT_LOCAL()
    {
        viewModelScope.launch {
            repo.REPO_LOCAL_DELETE_ALL_EVENT();
        }
    }

    // + --------->>-------->>--------->>*** -->>----------->>>>
    // * DATA WHEEL KONTROL FACTORY
    class DataWheelKontrolFactory(private val repoWheel: RepoWheel) : ViewModelProvider.Factory
    {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if(modelClass.isAssignableFrom(WedaKontrol::class.java))
            {
                Log.w("-des- <== WEDA KONTROL FACTORY ==>", "" );
                return WedaKontrol(repoWheel) as T;
            }
            throw IllegalArgumentException("Unknown View Model class");
        }

    }

    // + REMOTE THING--------->>-------->>--------->>*** -->>----------->>>>
    private var fbdata = FirebaseDatabase.getInstance();
    private var fbstore = FirebaseFirestore.getInstance();

    private var evntFlowJob: Job? = null;
    private var livevnt : MutableLiveData<MutableSet<FireEvent>> = MutableLiveData<MutableSet<FireEvent>>();
    private var muevntset: MutableSet<FireEvent> = mutableSetOf();
    /**
    * *             VM_GET_REMOTE_EVENTS
    */
    fun VM_GET_REMOTE_EVENTS(state : String, region : String)
    {
        evntFlowJob = viewModelScope.launch(iodis) {
            repo.REPO_FB_GET_EVENTS(state, region)
                .onEach {
                    muevntset.add(it);
                }
                .collect {
                    Log.d(TAG, "VM_GET_REMOTE_EVENTS: size of evnt ${it}");
                    withContext(Dispatchers.Main) {
                        livevnt.value = muevntset;
                    }
                }

        }
    }

    /**
    * *         VM_STOP_DATABASE_EVENT_FETCHING
     * . stop the current fetching of events from database
    */
    fun VM_STOP_DATABASE_EVENT_FETCHING()
    {
        Log.i(TAG, "VM_STOP_DATABASE_EVENT_FETCHING: STOP CURRENT FETCHING OF EVENTS");
        evntFlowJob?.cancel();
        muevntset.clear();
    }

}
// Log.i("TIME MEASURING $time");
val time = measureTimeMillis {
    // couroutine function here

}
