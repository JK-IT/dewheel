package desoft.studio.dewheel.kata

/**
* Database : EVENTS
 * 	-JID	-KID	-NAME	-ADDR	-TIME
*/

data class WheelJolly(
	var jid: String? = null,
	var creator: String? = null,
	var kid:String? = null,
	var name: String? = null,
	var addr: String? = null,
	var area: String? = null,
	var time: Long? = null,
)
