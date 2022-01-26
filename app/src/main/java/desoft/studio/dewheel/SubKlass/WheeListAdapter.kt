package desoft.studio.dewheel.SubKlass

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import desoft.studio.dewheel.DataKenter.WedaKontrol
import desoft.studio.dewheel.R
import desoft.studio.dewheel.kata.BriefFireEvent
import desoft.studio.dewheel.local.Ksaved
import java.text.SimpleDateFormat
import java.util.*

class WheeListAdapter(val ctx : Context, private var recyview : RecyclerView, private var wemodel : WedaKontrol?) : RecyclerView.Adapter<WheeListAdapter.Kholder>() {

    /**
    * VIEW HOLDER CLASS = inner -> access properties of outer class
     * inner class holds reference to outer class
    */
    inner class Kholder(iv: View) : RecyclerView.ViewHolder(iv)
    {
        var title = iv.findViewById<TextView>(R.id.evnt_row_title);
        var about = iv.findViewById<TextView>(R.id.evnt_row_about);
        var addrtet : TextView = iv.findViewById(R.id.evnt_row_address);
        var addrviewbtn: Button = iv.findViewById(R.id.evnt_row_see_on_map);
        var evnttimetet :TextView = iv.findViewById(R.id.evnt_row_time);
        var creator : TextView = iv.findViewById(R.id.evnt_host_name);
        var takbtn : Button = iv.findViewById(R.id.owner_contact_btn);
        var likedbtn : ImageButton = iv.findViewById(R.id.evnt_row_liked_btn);
        var myrun : Runnable? = null;
        var sfm = SimpleDateFormat("EEE, MMM dd 'at' hh:mm a", Locale.getDefault());

        fun FILLING_VIEW(data : BriefFireEvent)
        {
            var fev = data.fev;
            fev?.let {
                title.text = fev?.name;
                if(it.about.isNullOrBlank())    about.visibility = View.GONE;
                else   about.text = it.about;
                addrtet.text = fev?.location;
                evnttimetet.text = fev?.time;
                creator.text = fev?.hostname;
                takbtn.setOnClickListener {

                }
                addrviewbtn.setOnClickListener {
                    //Toast.makeText(ctx, "Clicked", Toast.LENGTH_SHORT).show();
                    var itemaddr = addrtet.text.toString();
                    var gmminte = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(itemaddr)+"&z=15"));
                    (attchedRecyView?.context)?.startActivity(gmminte);
                }
                likedbtn.isActivated=data.saved?: false;
                likedbtn.setOnClickListener {
                    it.isActivated = !(it.isActivated);
                    myrun?.let { it1 ->
                        Log.w(TAG, "FILLING_VIEW: Cancelling runnable");
                        uihandle.removeCallbacks(it1) };
                    myrun = object : Runnable{
                        override fun run() {
                            Log.d(TAG, "run: MY RUNNABLE");
                            if(it.isActivated == true) {
                                var ksa = Ksaved(data.id!!,currarea, curradmin,data.fev);
                                wemodel?.VM_ADD_SAVED_EVNT(ksa);
                            } else {
                                data.id?.let { it1 -> wemodel?.VM_DELETE_SAVED_EVNT(it1) };
                            }
                        }
                    }
                    uihandle.postDelayed(myrun as Runnable, 2000);
                }
            }

        }
    }

    /**
    * ! ------> RECYCLERVIEW ADAPTER IMPLEMENTATION
    */
    private val TAG = "-des- ;;;=== -RECYCLERVIEW ADAPTER- ===;;;";

    private var itemlst : SortedMap<String, BriefFireEvent> = sortedMapOf();

    private var attchedRecyView : RecyclerView? = null;

    private var uihandle = Handler(Looper.getMainLooper());

    private var curradmin: String?=null;
    private var currarea:String?=null;
    // _ getting a referrence to recyclerview on attached
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        attchedRecyView = recyclerView;
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WheeListAdapter.Kholder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.design_evnt_row, parent, false);
        return Kholder(v);
    }

    override fun onBindViewHolder(holder: Kholder, position: Int) {
        var data = itemlst.toList().get(position);
        holder.FILLING_VIEW(data.second);
    }

    override fun getItemCount(): Int {
        return itemlst.size ?: 0;
    }

    fun KF_UPDATE_AREA(inadmin : String, inarea: String)
    {
        curradmin=inadmin; currarea = inarea;
    }

    fun KF_ADD_EVENT(karam: BriefFireEvent)
    {
        if( !itemlst.contains(karam.id)) {
            itemlst.set(karam.id.toString(), karam);
            notifyItemInserted(itemlst.toList().indexOf(Pair(karam.id, karam)));
        }
    }

    fun KF_CONSUME_SAVED_LIST(savedlst : List<Ksaved>)
    {
        Log.i(TAG, "Recycler adapter KF_CONSUME_SAVED_LIST: ${savedlst.size}");
        for(ksa in savedlst) {
            var brievnt = BriefFireEvent(ksa.id, ksa.firevnt, true);
            if( !itemlst.contains(ksa.id)) {
                itemlst.set(ksa.id, brievnt);
                notifyItemInserted(itemlst.toList().indexOf(Pair(ksa.id, brievnt)));
            }
        }
    }

    fun KF_CLEAR_EVENT()
    {
        itemlst.clear();
        notifyDataSetChanged();
    }
    companion object {

    }
}