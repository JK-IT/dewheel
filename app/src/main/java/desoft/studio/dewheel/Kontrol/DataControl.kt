package desoft.studio.dewheel.Kontrol

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import desoft.studio.dewheel.kata.K_User
import desoft.studio.dewheel.kata.Kadress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG : String = "-des- k-- DATA CONTROL ( VIEW MODEL) -->l";

class DataControl(ctx : Application) : AndroidViewModel(ctx)
{
	private var defdis = Dispatchers.Default;
	private var iodis = Dispatchers.IO;
	
	private lateinit var user : K_User;
	
	private var storedb = Firebase.firestore;
	private var userstore = storedb.collection("users");
	
	// * uploading status
	val sucuload :MutableLiveData<Boolean> by lazy {
		MutableLiveData<Boolean>();
	}
	// * assigned location
	val pickedLocation : MutableLiveData<Kadress> by lazy {
		MutableLiveData<Kadress>();
	}
	
	/***
	 * Creating  A User class on init, fill out from cache
	 */
	//_ init
	init
	{
		Log.d(TAG, "VIEW MODEL ININT: Data Control is created");
	}
	//_ clean up function
	override fun onCleared()
	{
		super.onCleared();
	}
	
	/**
	 * UPload user to firestore
	 */
	fun KF_VMuploadUSER(usr: K_User){
		viewModelScope.launch (iodis) {
			usr.kid?.let {
				userstore.document(it).set(usr).addOnSuccessListener {
					sucuload.value = true;
				}.addOnFailureListener {
					sucuload.value = false;
					Log.e(TAG, "KF_VMuploadUSER: = FAILED TO UPLOAD USER ", it);
				}
			}
		}
		
	}
	
	class DataFactory(private var appli: Application): ViewModelProvider.AndroidViewModelFactory(appli)
	{
		override fun <T : ViewModel> create(modelClass: Class<T>): T
		{
			if(modelClass.isAssignableFrom(DataControl::class.java))
			{
				return DataControl(appli) as T;
			}
			throw IllegalArgumentException("NOT A DATA CONTROL CLASS");
		}
	}
}