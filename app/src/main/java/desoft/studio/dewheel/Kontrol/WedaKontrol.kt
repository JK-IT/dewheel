package desoft.studio.dewheel.Kontrol

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import desoft.studio.dewheel.kata.BriefFireEvent
import desoft.studio.dewheel.local.Kevent
import desoft.studio.dewheel.local.Ksaved
import desoft.studio.dewheel.local.Kuser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
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

    override fun onCleared() {
        super.onCleared();
        Log.w(TAG, "onCleared: VIEW MODEL IS SHUTTING DOWN");
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

    // + SAVED EVENT DATA KONTROL--------->>-------->>--------->>*** -->>----------->>>>

    var savedLiveLst : MutableLiveData<List<Ksaved>?> = MutableLiveData<List<Ksaved>?>();
    /**
    *   *                     VM_ADD_SAVED_EVNT
    */
    fun VM_ADD_SAVED_EVNT(saved: Ksaved)
    {
        viewModelScope.launch(iodis) {
            Log.w(TAG, "VM_ADD_SAVED_EVNT: adding saved evnt to database");
            repo.REPO_ADD_SAVED_EVNT(saved);
        }
    }
    /**
    * *                         VM_GET_ALL_SAVED
    */
    fun VM_GET_ALL_SAVED() : List<Ksaved>?
    {
        viewModelScope.launch(iodis) {
            var salst = repo.REPO_GET_ALL_SAVED();
            Log.w(TAG, "VM_GET_ALL_SAVED: ${salst.size}");
            if(salst.size != 0){
                Log.w(TAG, "PRINTING SAVED EVENT ${salst[0].firevnt}" );
            }
        }
        return null;
    }
    /**
    * *                             VM_GET_SAVED_FROM
    */
    fun VM_GET_SAVED_FROM(inadmin : String, inregion : String)
    {
        viewModelScope.launch(iodis) {
            var lstsaved : List<Ksaved> = mutableListOf();
            withContext(Dispatchers.Main){
                savedLiveLst.value = null;
            }
            withContext(iodis) {
                lstsaved = repo.REPO_GET_SAVED_FROM(inadmin, inregion);
                Log.w(TAG, "VM_GET_SAVED_FROM: $inregion, ${lstsaved.size?: 0}");
                if(lstsaved.size != 0) {
                    withContext(Dispatchers.Main){
                        savedLiveLst.value = lstsaved;
                    }
                }
            }
        }
    }
    /**
    * *                     VM_DELETE_SAVED_EVNT
    */
    fun VM_DELETE_SAVED_EVNT(inid: String)
    {
        viewModelScope.launch (iodis) {
            repo.REPO_DELETE_SAVED_ID(inid);
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

    var livevntlst : MutableLiveData<MutableSet<BriefFireEvent?>> = MutableLiveData<MutableSet<BriefFireEvent?>>();
    var livevnt : MutableLiveData<BriefFireEvent?>  = MutableLiveData<BriefFireEvent?>();
    private var muevntset: MutableSet<BriefFireEvent> = mutableSetOf();

    /**
    * *             VM_GET_REMOTE_EVENTS
    */
    suspend fun VM_GET_REMOTE_EVENTS(state : String, region : String)
    {
        Log.w(TAG, "VM_GET_REMOTE_EVENTS: Gettng events from designated location == $state, $region");
        try {
            repo.REPO_FB_GET_EVENTS(state, region)
                .filter { fev ->
                    !muevntset.contains(fev);
                }
                .onEach {
                    muevntset.add(it);
                }
                .buffer(Channel.UNLIMITED)
                .collect {
                    Log.w(TAG, "VM_GET_REMOTE_EVENTS: size of evnt ${muevntset.size}");
                    withContext(Dispatchers.Main) {
                        //livevntlst.value = muevntset;
                        livevnt.value = it;
                    }
                }
        } finally {
            Log.w(TAG, "VM_GET_REMOTE_EVENTS: FINALLY IM GETTING CANCELLED on $state, $region");
            muevntset.clear();
            livevnt.value = null;
            livevntlst.value = mutableSetOf();
        }
    }

}
// Log.i("TIME MEASURING $time");
val time = measureTimeMillis {
    // couroutine function here

}
