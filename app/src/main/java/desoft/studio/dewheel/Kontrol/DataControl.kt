package desoft.studio.dewheel.Kontrol

import android.app.Application
import android.util.Log
import androidx.annotation.NonNull
import androidx.lifecycle.*
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import desoft.studio.dewheel.kata.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentLinkedQueue

private const val TAG : String = "-des- k-- DATA CONTROL ( VIEW MODEL) -->l";

class DataControl(@NonNull ctx : Application) : AndroidViewModel(ctx)
{
	private var defdis = Dispatchers.Default;
	private var iodis = Dispatchers.IO;

	private var realdbSource : RealtimeSource;
	//! .info/connected = general status -> indicate if app is connecting to server
	val infostate = Firebase.database.getReference(".info/connected");
	//!	roomid: id of requested room
	var roomid: String? = null;
	//! rooMsgReg : a reference to realtime database location for message in the room
	var rooMsgRef :DatabaseReference? = null;
	//! rommsg : message on the room
	val romLiveMsg : MutableLiveData<Kmessage> = MutableLiveData<Kmessage>();
	//! concurrentLinkedQueue -> to store room msg and process with model FIFO
	var roMsgContainer = ConcurrentLinkedQueue<Kmessage>();

	//private var fbuser : FirebaseUser? = null;
	var userFilled : Boolean = false;
	var user : K_User = K_User();
	private var userstore = Firebase.firestore.collection("users");

	// ! uploading status
	val userUploadFlag :MutableLiveData<Boolean> by lazy {
		MutableLiveData<Boolean>();
	}
	// ! assigned location
	val pickedLocation : MutableLiveData<Kadress> = MutableLiveData<Kadress>();

	// ! jolly FLOW JOB
	private var jollyJob : Job? = null;
	// ! single jolly, WHEEL JOLLY LIVE DATA
	var jolly : MutableLiveData<WheelJolly?> = MutableLiveData<WheelJolly?>();
	// ! jolly update flag
	val jollyUploadFlag: MutableLiveData<Boolean> by lazy {
		MutableLiveData<Boolean>();
	}

	/**
	 * * 					INIT
	 * Creating  A User class on init, fill out from cache or from FirebaseUser parameter
	 * only creating Kuser if user is not anonymous
	 */
	init
	{
		Log.w(TAG, "VIEW MODEL ININT: Data Control is created");
		realdbSource = RealtimeSource();
	}
	/*
	* * 					ONCLEARED
	*/
	override fun onCleared()
	{
		Log.i(TAG, "onCleared: == VIEW MODEL CLEAN UP IS CALLED");
		super.onCleared();
	}

	//#region SETUP K WHEEL USER SECTION
	/**
	 * * KF_VM_SETUP_USER_FROM_FIREBASE
	! SETUP VIEW MODEL WITH ASSIGNED FIREBASE AND GOOGLE IDEN USER
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
				userFilled = true;
				Log.w(TAG, "Init: Hey , This is the user calling View Model Init $user");
			}
		}
	}

	/**
	 * *							KF_VM_SETUP_USER
	*	SETUP USER NAME FROM CACHE
	*/
	fun KF_VM_FILLOUT_USER(iname :String, igender: String? =null, isexori : String? =null, ifavorite: String? = null)
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
	 * * KF_VM_UP_USER
	 * assing user to this uploaded user
	 * make sure to check that fb user is not anonymous, aka not failed to sign up with fb server
	 */
	fun KF_VM_UP_USER(inusr: K_User){
		viewModelScope.launch (iodis) {
			inusr.kid?.let {
				userstore.document(it).set(inusr).addOnSuccessListener {
					userUploadFlag.value = true;
					user = inusr;
					Log.i(TAG, "KF_VM_UP_USER: == User just sign up and got uploaded");
				}.addOnFailureListener {
					userUploadFlag.value = false;
					Log.e(TAG, "KF_VMuploadUSER: = FAILED TO UPLOAD USER ", it);
				}
			}
		}
	}
	//#endregion

	//#region REALTIME DATABASE AND JOLLIES SECTION
	/**
	* ! Upload Jolly Occurrence to Database
	 * * KF_VM_UP_JOLLY
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
							jollyUploadFlag.value = false;
						} else {
							Log.d(TAG, "KF_VM_UP_JOLLY: == SUCCESSFULL UPLOADING JOLLY");
							jollyUploadFlag.value = true;
						}
					}
			}
		} else {
			Log.e(TAG, "KF_VM_UP_JOLLY: == fbuser is anonymous ${FirebaseAuth.getInstance().currentUser?.isAnonymous} or user is null ${user == null}");
			jollyUploadFlag.value = false;
		}
	}

	/**
	* GET DATA FROM REALTIME BASE WITH AREA
	 * * KF_VM_GET_JOLLIES_AT
	 * ? updating the events live data
	 * display stale and update data from stale
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
					.onEach{
						//_ we should do sth about this
						var area = pickedLocation.value?.locality;
					}
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

	    // _ --------->>-------->>--------->>*** -->>----------->>>>
	//#region REALTIME DB CHAT ROOM SECTION

	/**
	 * * 	*		KF_VM_CHATROOM
	* ! CREATE CHAT ROOM WITH JOLLY ID ON DATABASE
	 * . check if room exist
	 * . if not => creating new room
	 * chatroom  location
	 * gid_gid	--> status { gid, gid}
	 * --> messages {mgid, mgid, timestamp}
	*/
	suspend fun KF_VM_CHATROOM(rid : String ,jollydata : WheelJolly) : ResultBox
	{
		var snap = KF_VM_CHECK_ROOM_EXIST(	rid); //realdbSource.chatdb.child(rid).get().await();
		if(!snap) {
			Log.d(TAG, "KF_VM_CHATROOM: create room $rid");
			var ro = JollyRoom(jollydata.jid, null);
			var done =  realdbSource.chatdb.child(rid).setValue(ro);
			return ResultBox.VoidResult(done);
		} else {
			Log.w(TAG, "KF_VM_CHATROOM: Room already exist $rid");
			return ResultBox.RoomExist(true);
		}
	}

	/**
	* *			KF_VM_CHECK_EXIST
	 * ! check if location or location exist on server
	*/
	suspend fun KF_VM_CHECK_ROOM_EXIST(inparam: String) : Boolean
	{
		var snap = realdbSource.chatdb.child(inparam).get().await();
		return snap.exists();
	}

	/**
	* 	* 			KF_VM_SEND_DUMMY_MSG
	 * . register roomid to array on viewmodel
	 * todo: save it to room database
	 * . sending dummy msg, which include only your info, and empty msg
	 * . call func to register msg recall on realtime database -> assign value for rooMsgRef
	*/
	fun KF_VM_SEND_DUMMY_MSG(rid : String)
	{
		roomid = rid;
		viewModelScope.launch(iodis) {
			Log.i(TAG, "KF_VM_SEND_DUMMY_MSG: user of this chat activity $user");
			var dmsg =Kmessage(user.kid, System.currentTimeMillis().toString(), "");
			realdbSource.chatdb.child(roomid!!).child(RealtimeSource.roomsgRef).setValue(dmsg)
				.addOnSuccessListener {
					KF_VM_REGISTER_MSG_RECALL();
				}
		}
	}

	/**
	* *					KF_VM_REGISTER_MSG_RECALL
	 * !recall when a room exist on database
	 * todo: wrap it to flow, so u dont have to worry about overflow of messages
	*/
	private fun KF_VM_REGISTER_MSG_RECALL()
	{
		roomid?.let {
			rooMsgRef = realdbSource.chatdb.child(roomid!!).child(RealtimeSource.roomsgRef);
			rooMsgRef?.addValueEventListener(object : ValueEventListener{
				override fun onDataChange(snapshot: DataSnapshot) {
					var msg = snapshot.getValue<Kmessage>();
					//Log.w(TAG, "REGISTER MSG RECALL: This is new msg, $msg");
					msg?.let {
						romLiveMsg.value = it;
					}
				}

				override fun onCancelled(error: DatabaseError) {
					Log.e(TAG, "onCancelled: ERROR ON MSG DATABASE", error.toException());
				}
			})
		}
	}
	//#endregion

	// + --------->>-------->>--------->>*** -->>----------->>>>
	/**
	* *							SEAL CLASS FOR DATA AND ERROR
	 *
	*/
	sealed class ResultBox {
		data class VoidResult(var resu: Task<Void>) : ResultBox();
		data class RoomExist(var yesorno : Boolean) : ResultBox();
	}

	// + --------->>-------->>--------->>*** -->>----------->>>>
	/**
	 * * 							DATA FACTORY
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