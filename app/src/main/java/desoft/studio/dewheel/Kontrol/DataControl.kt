package desoft.studio.dewheel.Kontrol

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import desoft.studio.dewheel.kata.User
import java.lang.IllegalArgumentException

private const val TAG : String = "-des- k-- DATA CONTROL ( VIEW MODEL) -->l";

class DataControl(ctx : Application) : AndroidViewModel(ctx)
{
	private var fbauth : FirebaseAuth? = null;
	private lateinit var user : User;
	
	/***
	 * Creating  A User class on init, fill out from cache
	 */
	//_ init
	init
	{
		fbauth = FirebaseAuth.getInstance();
		Log.d(TAG, "VIEW MODEL ININT: Data Control is created ${fbauth!!.uid}");
	}
	//_ clean up function
	override fun onCleared()
	{
		super.onCleared()
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