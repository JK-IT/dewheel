package desoft.studio.dewheel.Kontrol

import android.app.Application
import android.util.Log
import androidx.annotation.NonNull
import androidx.lifecycle.*
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import desoft.studio.dewheel.kata.K_User
import desoft.studio.dewheel.kata.Kadress
import desoft.studio.dewheel.kata.WheelJolly
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG : String = "-des- k-- DATA CONTROL ( VIEW MODEL) -->l";

class DataControl(@NonNull ctx : Application) : AndroidViewModel(ctx)
{
	private var defdis = Dispatchers.Default;
	private var iodis = Dispatchers.IO;

	private var fbuser : FirebaseUser? = null;
	private var user : K_User = K_User();
	
	private var userstore = Firebase.firestore.collection("users");
	private var jollydb = Firebase.database.getReference("jollies");
	
	// * WHEEL JOLLY LIVE DATA
	val wjolly: MutableLiveData<WheelJolly> = MutableLiveData<WheelJolly>();
	// * uploading status
	val sucuload :MutableLiveData<Boolean> by lazy {
		MutableLiveData<Boolean>();
	}
	// * assigned location
	val pickedLocation : MutableLiveData<Kadress> = MutableLiveData<Kadress>();
	
	// * jolly update flag
	val jollyupload: MutableLiveData<Boolean> by lazy {
		MutableLiveData<Boolean>();
	}

	/***
	 * Creating  A User class on init, fill out from cache or from FirebaseUser parameter
	 * only creating Kuser if user is not anonymous
	 */
	//_ init
	init
	{
		Log.w(TAG, "VIEW MODEL ININT: Data Control is created");
	}
	//_ clean up function
	override fun onCleared()
	{
		Log.i(TAG, "onCleared: == VIEW MODEL CLEAN UP IS CALLED");
		super.onCleared();
	}

	/**
	* SETUP VIEW MODEL WITH ASSIGNED FIREBASE USER
	*/
	fun KF_VM_SETUP_WITH_FIREBASE (inuser : FirebaseUser){
		viewModelScope.launch(defdis) {
			fbuser = inuser;
			if(fbuser!!.isAnonymous == false)
			{
				var gid = fbuser!!.providerData.get(1).uid;
				var fbid = fbuser!!.providerData.get(0).uid;
				var email = fbuser!!.email;
				user = K_User(gid, fbid, email);
				Log.w(TAG, "Init: Hey , This is the user calling View Model Init $user");
			}
		}
	}
	/**
	*	SETUP USER NAME FROM CACHE
	*/
	fun KF_VM_SETUP_USER(iname :String, igender: String? =null, isexori : String? =null, ifavorite: String? = null)
	{
		viewModelScope.launch(defdis) {
			user.app_user_name = iname;
			if(igender != null)
			{
				user.gender = igender; user.sorient = isexori; user.favorite = ifavorite;
			}
			Log.i(TAG, "KF_VM_SETUP_USER: == Setup user uptodate $user");
		}
	}
	/**
	 * !UPload user to firestore
	 * assing user to this uploaded user
	 * make sure to check that fb user is not anonymous, aka not failed to sign up with fb server
	 */
	fun KF_VM_UP_USER(inusr: K_User){
		viewModelScope.launch (iodis) {
			inusr.kid?.let {
				userstore.document(it).set(inusr).addOnSuccessListener {
					sucuload.value = true;
					user = inusr;
					Log.i(TAG, "KF_VM_UP_USER: == User just sign up and got uploaded");
				}.addOnFailureListener {
					sucuload.value = false;
					Log.e(TAG, "KF_VMuploadUSER: = FAILED TO UPLOAD USER ", it);
				}
			}
		}
	}
	
	/**
	* * Upload Jolly Occurrence to Database
	*/
	fun KF_VM_UP_JOLLY(injoname: String, injoaddr:String, inarea: String, inadmin1: String, injotime:Long)
	{
		if(user != null)
		{
			viewModelScope.launch() {
				Log.i(TAG, "KF_VM_UP_JOLLY: == Uploading jolly");
				var wjo = WheelJolly(System.currentTimeMillis().toString(), user.app_user_name!!, user.kid!!, injoname, injoaddr, inarea, injotime);
				jollydb.child(inadmin1).child(wjo.area).child(wjo.jid).setValue(wjo)
					.addOnCompleteListener {
						if( ! it.isSuccessful)
						{
							Log.e(TAG, "KF_VM_UP_JOLLY: Failed to upload jolly why????", it.exception);
							jollyupload.value = false;
						} else {
							Log.d(TAG, "KF_VM_UP_JOLLY: == SUCCESSFULL UPLOADING JOLLY");
							jollyupload.value = true;
						}
					}
			}
		} else {
			Log.e(TAG, "KF_VM_UP_JOLLY: == fbuser is anonymous ${fbuser?.isAnonymous} or user is null ${user == null}");
			jollyupload.value = false;
		}
	}

	/**
	* GET DATA FROM REALTIME BASE WITH AREA
	 * * updating the events live data
	*/
	fun KF_VM_GET_JOLLIES_AT(inState: String, inarea: String) {
		Log.w(TAG, "KF_VM_GET_JOLLIES_AT: jollies user $user");
		Log.w(TAG, "KF_VM_GET_JOLLIES_AT: == Getting at State $inState, city $inarea");
		viewModelScope.launch(iodis){
			jollydb.child(inState).child(inarea).addChildEventListener(jolliesCallback);
		}
	}
	// ? CALLBACK GETTING JOLLIES FROM DATABASE
	private val jolliesCallback = object : ChildEventListener{
		override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
			Log.w(TAG, "onChildAdded: NEW EVENT IS ADDED ----> ::::: ${snapshot.key} and previous name $previousChildName");
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
			TODO("Not yet implemented")
		}

	}

	/**
	 * *----------------------------------------
	* ? THIS IS FACTORY OF THIS VIEW MODEL
	*/
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