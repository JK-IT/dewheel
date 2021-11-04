package desoft.studio.dewheel.kata

/**
 * upload event on database, what u need
 * -----------------------------------------------------------
 * places picture  ===      [[[[[[----]]]]]]]                   |
 *           ++++   lll   +++  llll          aaaaaa             |
 *      kkk    ,,,..  kkkkk  ...,,, kkk >>>><<<<<               |
 * name: keyle == app_user_name                                 |
 * prompt: let's hangout at this beautiful restaurant           |
 * time and date : milli-second format                          |
 * location: -> this will be dynamic                            |
 * user id on firestore, to get userinfo                        |
 * (click on see button will read data of user from server )    |
 * see user-info: {gender, sexual orientation, favorite things} |
 * Chat -> will open chat activity
 */

/**
 * fone number, email , address will be secret, only u give it to the other
 * id: gmail_gid
 * fbid: uid
 * app_user_name
 * gender
 * sexual orientation
 * favorite
 */
data class K_User(
	var kid : String?,
	var fbid: String?,
	var email : String?,
	var app_user_name: String? = null,
	var gender: String?=null,
	var sorient: String?=null,
	var favorite: String?=null
               )
