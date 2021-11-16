package desoft.studio.dewheel.kata

/**
* Database : EVENTS
 * 	-JID	-KID	-NAME	-ADDR	-TIME
*/

data class WheelJolly(
	var jid: String,
	var creator: String,
	var kid:String,
	var name: String,
	var addr: String,
	var area: String,
	var time: Long,
)
