package desoft.studio.dewheel.SubKlass

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import desoft.studio.dewheel.R
import desoft.studio.dewheel.katic.KONSTANT
import java.util.*

class WimePicker()   : DialogFragment()
{
    private val TAG = "-des- --<< TIME DATE PICKER DIALOG >>--";
    private var cale = Calendar.getInstance();
    private lateinit var datpicker : DatePicker;
    private lateinit var timpicker : TimePicker;

    private lateinit var okbtn : Button;
    private lateinit var chngbtn : Button;

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        //super.onCreateView(inflater, container, savedInstanceState)
        var v = inflater.inflate(R.layout.dialog_time_date, container, false);
        return v;
    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?)
    {
        //super.onViewCreated(view, savedInstanceState);
        datpicker = v.findViewById(R.id.dialog_date_picker);
        datpicker.visibility = View.VISIBLE;
        timpicker = v.findViewById(R.id.dialog_time_picker);
        timpicker.visibility = View.GONE;
        okbtn = v.findViewById(R.id.dialog_picker_ok_btn);
        chngbtn = v.findViewById(R.id.dialog_picker_change_btn);

        datpicker.setOnDateChangedListener { view, year, monthOfYear, dayOfMonth ->
            Log.i(TAG, "DATE PICKER CHANGE $year $monthOfYear $dayOfMonth");
            cale.set(Calendar.YEAR, year);
            cale.set(Calendar.MONTH, monthOfYear);
            cale.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        }

        timpicker.setOnTimeChangedListener { view, hourOfDay, minute ->
            Log.i(TAG, "TIME PICKER CHANGE $hourOfDay $minute");
            cale.set(Calendar.HOUR_OF_DAY, hourOfDay);
            cale.set(Calendar.MINUTE, minute);
        }
        chngbtn.setOnClickListener {
            var datfirst = datpicker.visibility;
            datpicker.visibility = timpicker.visibility;
            timpicker.visibility = datfirst;
        }
        okbtn.setOnClickListener {
            var bund = Bundle().also {
                it.putLong(KONSTANT.timeMilliSecBundleKey, cale.timeInMillis);
            }
            setFragmentResult(KONSTANT.timePickerReqKey, bund);
            dismiss();
        }
    }

    companion object
    {
        val fragtag = "TIME PICKER FRAGMENT TAG";
    }


}