package com.sierrawireless.avphone

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.ListView
import android.widget.TextView
import com.sierrawireless.avphone.activity.MainActivity
import com.sierrawireless.avphone.adapter.RunListViewAdapter
import com.sierrawireless.avphone.model.AvPhoneObjectData
import com.sierrawireless.avphone.service.LogMessage
import com.sierrawireless.avphone.service.NewData
import com.sierrawireless.avphone.tools.Tools
import java.text.SimpleDateFormat
import java.util.*

/**
 * A component in charge of listening for service events (new data, logs) and updating the view accordingly.
 */
class DataViewUpdater(private val view: View, private val activity: MainActivity) : BroadcastReceiver() {

    private val hourFormat = SimpleDateFormat("HH:mm:ss", Locale.FRENCH)
    private var objectsManager: ObjectsManager? = null

    init {
        objectsManager = ObjectsManager.getInstance()
    }

    override fun onReceive(context: Context, intent: Intent) {

        if (intent is NewData) {
            setNewData(intent)
        } else if (intent is LogMessage) {
            setLogMessage(intent.message, System.currentTimeMillis(), intent.alarm)
        }
    }

    fun onStart(startedSince: Long?, lastData: NewData, logMsg: String?, lastRun: Long?) {
        this.setStartedSince(startedSince)
        this.setNewData(lastData)
        this.setLogMessage(logMsg, lastRun, false)

        // activate alarm button
        //view.findViewById(R.id.alarm_switch).setEnabled(true);
    }

    fun onStop() {
        this.setStartedSince(null)

        // deactivate alarm button
        //view.findViewById(R.id.alarm_switch).setEnabled(false);
    }

    @SuppressLint("SetTextI18n")
    private fun setLogMessage(log: String?, timestamp: Long?, alarm: Boolean) {
        val logView: TextView = if (alarm) {
            findView(R.id.alarm_log)
        } else {
            findView(R.id.service_log)
        }
        if (log != null) {
            logView.text = hourFormat.format(if (timestamp != null) Date(timestamp) else Date()) + " - " + log
            logView.visibility = View.VISIBLE
        } else {
            logView.visibility = View.GONE
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setStartedSince(startedSince: Long?) {
        val startedTextView = findView(R.id.started_since)
        if (startedSince != null) {
            startedTextView.text = (view.context.getString(R.string.started_since) + " "
                    + SimpleDateFormat("dd/MM HH:mm:ss", Locale.FRENCH).format(Date(startedSince)))
            startedTextView.visibility = View.VISIBLE
        } else {
            startedTextView.visibility = View.GONE
        }
    }

    private fun setNewData(data: NewData) {

        val phoneListView = view.findViewById<ListView>(R.id.phoneListView)
        val listPhone = ArrayList<HashMap<String, String>>()

        val rssi: String = when {
            data.rssi != null -> data.rssi!!.toString() + " dBm"
            data.rsrp != null -> data.rsrp!!.toString() + " dBm"
            else -> "Unknown"
        }

        var temp: HashMap<String, String> = HashMap()

        temp[Tools.NAME] = "RSSI"
        temp[Tools.VALUE] = rssi
        listPhone.add(temp)

        temp = HashMap()
        temp[Tools.NAME] = "Operator"
        if (data.operator == null) {
            temp[Tools.VALUE] = ""
        } else {
            temp[Tools.VALUE] = data.operator!!
        }
        listPhone.add(temp)

        temp = HashMap()
        temp[Tools.NAME] = "Bytes Sent"
        if (data.bytesSent == null) {
            temp[Tools.VALUE] = "0 Mo"
        } else {
            temp[Tools.VALUE] = (data.bytesSent!! / (1024f * 1024f)).toString() + " Mo"
        }
        listPhone.add(temp)

        temp = HashMap()
        temp[Tools.NAME] = "Bytes Received"
        if (data.bytesReceived == null) {
            temp[Tools.VALUE] = "0 Mo"
        } else {
            temp[Tools.VALUE] = (data.bytesReceived!! / (1024f * 1024f)).toString() + " Mo"
        }
        listPhone.add(temp)

        temp = HashMap()
        temp[Tools.NAME] = "Network Type"
        if (data.networkType == null) {
            temp[Tools.VALUE] = ""
        } else {
            temp[Tools.VALUE] = data.networkType!!
        }
        listPhone.add(temp)

        temp = HashMap()
        temp[Tools.NAME] = "Latitude"
        if (data.latitude == null) {
            temp[Tools.VALUE] = ""
        } else {
            temp[Tools.VALUE] = data.latitude!!.toString()
        }
        listPhone.add(temp)

        temp = HashMap()
        temp[Tools.NAME] = "Longitude"
        if (data.longitude == null) {
            temp[Tools.VALUE] = ""
        } else {
            temp[Tools.VALUE] = data.longitude!!.toString()
        }
        listPhone.add(temp)
        val adapter = RunListViewAdapter(activity, listPhone)
        phoneListView.adapter = adapter
        phoneListView.invalidateViews()

        setCustomDataValues()
    }

    private fun setCustomDataValues() {
        val objectListView = view.findViewById<ListView>(R.id.objectLstView)
        objectsManager = ObjectsManager.getInstance()
        val obj = objectsManager!!.currentObject
        var temp: HashMap<String, String>
        val listObject = ArrayList<HashMap<String, String>>()
        for (ldata in obj!!.datas) {
            temp = HashMap()
            temp[Tools.NAME] = ldata.name
            if (ldata.mode != AvPhoneObjectData.Mode.None) {
                temp[Tools.VALUE] = ldata.current.toString()
            } else {
                temp[Tools.VALUE] = ldata.defaults
            }
            listObject.add(temp)
        }
        val adapter = RunListViewAdapter(activity, listObject)
        objectListView.adapter = adapter
        objectListView.invalidateViews()
    }

    private fun findView(id: Int): TextView {
        return view.findViewById<View>(id) as TextView
    }

}
