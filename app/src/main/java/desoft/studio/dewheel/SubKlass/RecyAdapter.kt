package desoft.studio.dewheel.SubKlass

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import desoft.studio.dewheel.kata.WheelJolly

class RecyAdapter(val ctx : Context, var design: Int) : RecyclerView.Adapter<RecyAdapter.Kholder>() {

    /**
    * VIEW HOLDER CLASS = inner -> access properties of outer class
     * inner class holds reference to outer class
    */
    inner class Kholder(iv: View) : RecyclerView.ViewHolder(iv)
    {
        /*var prompt = iv.findViewById<TextView>(R.id.jolly_prompt);
        var addr : TextView = iv.findViewById(R.id.jolly_address);
        var jollytime :TextView = iv.findViewById(R.id.jolly_time);
        var creator : TextView = iv.findViewById(R.id.jolly_creator_name);
        var takbtn : Button = iv.findViewById(R.id.jolly_talk_btn);*/
        init {
            viewStupFunc?.InitView(iv);
        }

        fun FILLING_VIEW(data : WheelJolly){
            viewStupFunc?.FillingData(data);
        }
    }

    /**
    * ? --INTERFACE FOR GENERIC VIEW SETUP
    */
    public interface I_Adapter_View_Setup {
        fun InitView(v: View);
        fun FillingData(data: WheelJolly);
    }
    /**
    * ? ------> RECYCLERVIEW ADAPTER IMPLEMENTATION
    */
    private val TAG = "-des- ;;;=== -RECYCLERVIEW ADAPTER- ===;;;";

    var viewStupFunc : I_Adapter_View_Setup? = null;

    private var itemlst : ArrayList<WheelJolly> = arrayListOf();

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Kholder {
        val v = LayoutInflater.from(parent.context).inflate(design, parent, false);
        return Kholder(v);
    }

    override fun onBindViewHolder(holder: Kholder, position: Int) {
        var data = itemlst.get(position);
        holder.FILLING_VIEW(data);
    }

    override fun getItemCount(): Int {
        return itemlst.size ?: 0;
    }

    // _ assign view setup interface
    fun KF_ASSIGN_VIEW_SETUP_INTERFACE(param : I_Adapter_View_Setup)
    {
        viewStupFunc = param;
    }

    fun KF_CONSUME_JOLLY(karam: WheelJolly)
    {
        itemlst.add(karam);
        notifyItemChanged(itemlst.indexOf(karam));
    }
    companion object {

    }
}