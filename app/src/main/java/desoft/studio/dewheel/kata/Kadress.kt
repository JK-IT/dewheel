package desoft.studio.dewheel.kata

/**=null
 * this will be used as database model for storing a location - user or event
 * * For searching, we only need neighbor, locality, admin** areas, zip
 */
data class Kadress(
	var name: String? = null,
	var line : String?=null,
	var neighbor: String?=null,
	var locality: String?=null,
	var admin2: String?=null,
	var admin1: String?=null,
	var zip : Int?=null,
	var country: String?=null,
	var lati: Double?=null,
	var longi: Double?=null
)
