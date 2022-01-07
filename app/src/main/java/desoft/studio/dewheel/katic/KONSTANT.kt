package desoft.studio.dewheel.katic

object KONSTANT
{
	val userexist : String = "USER EXIST";
	val userauthen: String = "USER AUTHENTICATION";
	val useronstore : String = "USER ON FIRESTORE";
	val useruid : String = "USER UNIQUE ID";
	val usergid : String = "USER GOOGLE ID";
	val usergmail : String = "USER GOOGLE MAIL ADDRESS";
	val locationSuborNei : String = "LOCATION SUB-REGION";
	val locationRegion : String = "USER FAVORITE REGION-CITI-LOCATION";

	val evntRegularType : Int = 0;
	val evntInstantType : Int = 1;

	val timePickerReqKey = "TIME PICKER REQUEST KEY";
	val timeMilliSecBundleKey = "TIME MILLISECOND BUNDLE KEY";

	val userFirestorePath = "users";

	val username : String = "USER DISPLAY NAME";
	val fone: String = "USER GOOGLE PHONE NUMBER";
	val gender: String = "USER GENDER";
	val sexori: String = "USER SEXUAL ORIENTATION";
	val favor: String = "USER FAVORITE THINGS";
	val cache_timestamp : String = "CACHE SAVING TIME";

/*	val ACTIVITY_REQ_CODE : String = "ACTIVITY REQUEST CODE";
	val GOO_IN_REQ_CODE : Int = 111;
	val AUTOCOMPLETE_REQ_CODE : Int = 112;*/

	/**
	 * verified user -> create a user account from data control, upload it to firestore, or should i wait till user fill out all information
	 * or u cannot become verified user, if u don't fill out other required fields
	 * ? VALUE TYPE == BOOLEAN
 	 */
	// * BOOLEAN
	val user_upload_flag: String = "USER UPLOADING FLAG";
	// * STRING
	val lati_flag : String = "USER LATITUDE LOCATION";
	val logi_flag : String = "USER LONGITUDE LOCATION"
	
	val goo_package_name = "com.google.android.apps.maps";
}

/**
 * TODO HOW TO TURN OFF/SAVE POWER WHEN APP GO TO BACKGROUND ON LOCATION REQUEST
 * todo DO CHECK AUTHENTIC OF USER WHEN APP IS GOING TO BACKGROUND, CHECK IF USER IS STILL LOG IN OR NOT
 */