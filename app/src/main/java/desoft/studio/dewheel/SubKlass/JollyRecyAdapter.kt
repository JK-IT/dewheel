package desoft.studio.dewheel.SubKlass

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import desoft.studio.dewheel.MainActivity
import desoft.studio.dewheel.R
import desoft.studio.dewheel.kata.WheelJolly
import java.text.SimpleDateFormat
import java.util.*

class JollyRecyAdapter(val ctx : Context, var design: Int) : RecyclerView.Adapter<JollyRecyAdapter.Kholder>() {

    /**
    * VIEW HOLDER CLASS = inner -> access properties of outer class
     * inner class holds reference to outer class
    */
    inner class Kholder(iv: View) : RecyclerView.ViewHolder(iv)
    {
        var prompt = iv.findViewById<TextView>(R.id.jolly_prompt);
        var addr : TextView = iv.findViewById(R.id.jolly_address);
        var jollytime :TextView = iv.findViewById(R.id.jolly_time);
        var creator : TextView = iv.findViewById(R.id.jolly_creator_name);
        var takbtn : Button = iv.findViewById(R.id.jolly_talk_btn);
        var sfm = SimpleDateFormat("EEE, MMM dd 'at' hh:mm a", Locale.getDefault());

        fun FILLING_VIEW(data : WheelJolly){
            prompt.text = data.name;
            addr.text = data.addr;
            var calen = Calendar.getInstance();
            calen.timeInMillis = data.time!!;
            jollytime.text = sfm.format(data.time);
            creator.text = data.creator;
            takbtn.setOnClickListener {
                (ctx as MainActivity).KF_START_CHAT_ROOM(data);
            }
        }
    }

    /**
    * ? ------> RECYCLERVIEW ADAPTER IMPLEMENTATION
    */
    private val TAG = "-des- ;;;=== -RECYCLERVIEW ADAPTER- ===;;;";

    private var itemlst : ArrayList<WheelJolly> = arrayListOf();

    private var attchedRecyView : RecyclerView? = null;

    // _ getting a referrence to recyclerview on attached
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        attchedRecyView = recyclerView;
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JollyRecyAdapter.Kholder {
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

    fun KF_CONSUME_JOLLY(karam: WheelJolly)
    {
        itemlst.add(karam);
        notifyItemChanged(itemlst.indexOf(karam));
    }
    fun KF_CLEAR_DATA()
    {
        itemlst.clear();
        notifyDataSetChanged();
    }
    companion object {

    }
}