package desoft.studio.dewheel.Kontrol

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import desoft.studio.dewheel.local.Kuser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis

class WedaKontrol(private val repo: RepoWheel) :  ViewModel()
{
    private val TAG = "-des- k-- WEDA CONTROL ( VIEW MODEL) -->l"
    private val iodis = Dispatchers.IO;

    val userLivedata : MutableLiveData<Kuser?> = MutableLiveData<Kuser?>();

    init {
        Log.w(TAG, "Init: ViewMODEL IS CREATED" );
    }

    // + --------->>-------->>--------->>*** -->>----------->>>>
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
            repo.REPO_UPDATE_USER(inser);
        }
    }

    /**
    * *         VM_FIND_USER
    */
    fun VM_FIND_USER(inid : String)
    {
        viewModelScope.launch() {
            var resuser  = repo.REPO_LOCAL_FIND_USER(inid);
            Log.w(TAG, "VM_FIND_USER: found user $resuser" );
            userLivedata.value = resuser?: null;
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
}
// Log.i("TIME MEASURING $time");
val time = measureTimeMillis {
    // couroutine function here

}
