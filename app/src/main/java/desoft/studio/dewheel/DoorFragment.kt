package desoft.studio.dewheel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment


/**
 * A simple [Fragment] subclass.
 * Use the [DoorFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DoorFragment : Fragment()
{
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState);
	}
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
	{
		return inflater.inflate(R.layout.frag_door, container, false)
	}
	
	companion object {
		/**
		 * Use this factory method to create a new instance of
		 * this fragment using the provided parameters.
		 *
		 * @param param1 Parameter 1.
		 * @param param2 Parameter 2.
		 * @return A new instance of fragment DoorFragment.
		 */
		@JvmStatic
		fun newInstance() =
			DoorFragment().apply {
				arguments = Bundle().apply {
					/*putString(ARG_PARAM1, param1)
					putString(ARG_PARAM2, param2)*/
				}
			}
	}
}