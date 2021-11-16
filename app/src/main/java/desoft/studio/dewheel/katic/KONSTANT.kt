package desoft.studio.dewheel.katic

object KONSTANT
{
	val username : String = "USER DISPLAY NAME";
	val useruid : String = "USER UNIQUE ID";
	val usergid : String = "USER GOOGLE ID";
	val usergmail : String = "USER GOOGLE MAIL ADDRESS";
	val fone: String = "USER GOOGLE PHONE NUMBER";
	val gender: String = "USER GENDER";
	val sexori: String = "USER SEXUAL ORIENTATION";
	val favor: String = "USER FAVORITE THINGS";
	val cache_timestamp : String = "CACHE SAVING TIME";
	/**
	 * verified user -> create a user account from data control, upload it to firestore, or should i wait till user fill out all information
	 * or u cannot become verified user, if u don't fill out other required fields
	 * ? VALUE TYPE == BOOLEAN
 	 */
	val verified : String = "VERIFED USER";
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