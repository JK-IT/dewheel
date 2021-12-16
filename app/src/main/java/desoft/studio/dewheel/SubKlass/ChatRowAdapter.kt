package desoft.studio.dewheel.SubKlass

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import desoft.studio.dewheel.R
import desoft.studio.dewheel.kata.Kmessage

class ChatRowAdapter(var ctx: Context, var design : Int?= null) : RecyclerView.Adapter<ChatRowAdapter.ChatRowVholder>() {


    // + --------->>-------->>--------->>*** -->>----------->>>>
    // *    Chat Row VIew Holder Class
    inner class ChatRowVholder(inview : View) : RecyclerView.ViewHolder(inview) {
        var annou = inview.findViewById<TextView>(R.id.chat_row_inform);
        var body = inview.findViewById<TextView>(R.id.chat_row_body);

        fun BindMsg(km : Kmessage)
        {
            annou.text = km.sendername;
            body.text = km.sendermsg;
        }
    }

    // + --------->>-------->>--------->>*** -->>----------->>>>
    // * Chat Row Adapter Members

    var _recyview : RecyclerView? = null;
    var eleList : MutableList<Kmessage> = mutableListOf();

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        _recyview = recyclerView;
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatRowVholder {
        if(design == null)
            design = R.layout.design_chat_row;
        var vi = LayoutInflater.from(parent.context).inflate(design!!, parent, false);
        return ChatRowVholder(vi);
    }

    override fun onBindViewHolder(holder: ChatRowVholder, position: Int) {
        holder.BindMsg(eleList.get(position));
    }

    override fun getItemCount(): Int {
        return eleList.size;
    }

    // + --------->>-------->>--------->>*** -->>----------->>>>
    /**
    * *             KF_ADD_MSG
     * ! adding message to list, and update chat row
    */
    fun KF_ADD_MSG(kme : Kmessage)
    {
        eleList.add(kme);
        notifyItemInserted(eleList.indexOf(kme));
    }

}