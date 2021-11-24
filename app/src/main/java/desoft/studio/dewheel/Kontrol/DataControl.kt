package desoft.studio.dewheel.Kontrol

import android.app.Application
import android.util.Log
import androidx.annotation.NonNull
import androidx.lifecycle.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import desoft.studio.dewheel.kata.K_User
import desoft.studio.dewheel.kata.Kadress
import desoft.studio.dewheel.kata.WheelJolly
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

private const val TAG : String = "-des- k-- DATA CONTROL ( VIEW MODEL) -->l";

class DataControl(@NonNull ctx : Application) : AndroidViewModel(ctx)
{
	private var defdis = Dispatchers.Default;
	private var iodis = Dispatchers.IO;

	private lateinit var realdbSource : RealtimeSource;

	//private var fbuser : FirebaseUser? = null;
	private var user : K_User = K_User();
	private var userstore = Firebase.firestore.collection("users");

	// * WHEEL JOLLY LIVE DATA
	val wjolly: MutableLiveData<WheelJolly> = MutableLiveData<WheelJolly>();
	// * uploading status
	val sucuload :MutableLiveData<Boolean> by lazy {
		MutableLiveData<Boolean>();
	}
	// * assigned location
	val pickedLocation : MutableLiveData<Kadress> = MutableLiveData<Kadress>();

	// * jolly FLOW JOB
	private var jollyJob : Job? = null;
	// * a map that will hold old data and area
	//_ 1st param = city, 2nd param = list of jollies
	val oldJollies : Map<String, List<WheelJolly>> = mutableMapOf();
	// * jolly live data
	val jollies : MutableLiveData<ArrayList<WheelJolly>> = MutableLiveData<ArrayList<WheelJolly>>(arrayListOf());

	// * single jolly
	var jolly : MutableLiveData<WheelJolly?> = MutableLiveData<WheelJolly?>();
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
		realdbSource = RealtimeSource();
	}
	//_ clean up function
	override fun onCleared()
	{
		Log.i(TAG, "onCleared: == VIEW MODEL CLEAN UP IS CALLED");
		super.onCleared();
	}

	//#region SETUP K WHEEL USER SECTION
	/**
	* SETUP VIEW MODEL WITH ASSIGNED FIREBASE USER
	*/
	fun KF_VM_SETUP_USER_FROM_FIREBASE (){ //(inuser : FirebaseUser){
		viewModelScope.launch(defdis) {
			//fbuser = inuser;
			var fbuser = FirebaseAuth.getInstance().currentUser;
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
	//#endregion

	//#region USER AND FIRESTORE SECTION
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
	//#endregion

	//#region REALTIME DATABASE AND JOLLIES SECTION
	/**
	* * Upload Jolly Occurrence to Database
	*/
	fun KF_VM_UP_JOLLY(injoname: String, injoaddr:String, injotime:Long, inVenue: Kadress)
	{
		if(user != null)
		{
			viewModelScope.launch() {
				Log.i(TAG, "KF_VM_UP_JOLLY: == Uploading jolly");
				var wjo = WheelJolly(System.currentTimeMillis().toString(), user.app_user_name!!, user.kid!!, injoname, injoaddr,inVenue.locality.toString(), injotime);
				realdbSource.jollydb.child(inVenue.admin1!!).child(wjo.area!!).child(wjo.jid!!).setValue(wjo)
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
			Log.e(TAG, "KF_VM_UP_JOLLY: == fbuser is anonymous ${FirebaseAuth.getInstance().currentUser?.isAnonymous} or user is null ${user == null}");
			jollyupload.value = false;
		}
	}

	/**
	* GET DATA FROM REALTIME BASE WITH AREA
	 * * updating the events live data
	*/
	@InternalCoroutinesApi
	fun KF_VM_GET_JOLLIES_AT(inarea : Kadress) {
		Log.w(TAG, "KF_VM_GET_JOLLIES_AT: == ${inarea.locality} AND remove the old one");
		jollyJob?.cancel(); // ==> this will cancel the flow that is currently on
		if(jollyJob?.isCancelled == true || jollyJob == null)
		{
			jolly.value = null; // reset data on view model
			jollyJob = viewModelScope.launch(){
				realdbSource.KF_GET_JOLLIES_AT(inarea)
					.flowOn(defdis)
					.collect {
						Log.i(TAG, "KF_VM_GET_JOLLIES_AT: ===>>>=== i got the jolly ${it.jid}");
						//jollies.value?.add(it);
						jolly.value = it;
				}
			}
		}
	}
	//#endregion

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