package com.sierrawireless.avphone

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.SwitchCompat
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.ListView
import android.widget.TextView

import com.sierrawireless.avphone.adapter.RunListViewAdapter
import com.sierrawireless.avphone.auth.AuthUtils
import com.sierrawireless.avphone.auth.Authentication
import com.sierrawireless.avphone.message.IMessageDisplayer
import com.sierrawireless.avphone.model.AvPhoneObject
import com.sierrawireless.avphone.model.AvPhoneObjectData
import com.sierrawireless.avphone.service.LogMessage
import com.sierrawireless.avphone.service.MonitoringService
import com.sierrawireless.avphone.service.NewData
import com.sierrawireless.avphone.task.IAsyncTaskFactory
import com.sierrawireless.avphone.task.SyncWithAvListener
import com.sierrawireless.avphone.task.SyncWithAvParams
import com.sierrawireless.avphone.task.SyncWithAvResult
import com.sierrawireless.avphone.task.SyncWithAvTask
import com.sierrawireless.avphone.tools.Tools

import net.airvantage.utils.AvPhonePrefs
import net.airvantage.utils.PreferenceUtils

import java.util.ArrayList
import java.util.HashMap

open class RunFragment : AvPhoneFragment(), MonitorServiceListener, CustomLabelsListener {

    private var viewUpdater: DataViewUpdater? = null

    private var lView: View? = null

    private var monitorServiceManager: MonitorServiceManager? = null

    private var systemUid: String? = null
    private var systemName: String? = null

    private var taskFactory: IAsyncTaskFactory? = null
    private var objectName: String? = null
    private var objectsManager: ObjectsManager = ObjectsManager.getInstance()
    private lateinit var phoneBtn: Button
    private lateinit var objectBtn: Button
    private lateinit var phoneListView: ListView

    private lateinit var objectListView: ListView

    private val serviceSwitch: SwitchCompat
        get() = lView!!.findViewById<View>(R.id.service_switch) as SwitchCompat

    // Alarm button
    private var onAlarmClick: View.OnClickListener = View.OnClickListener {
        Log.d(LOGTAG, "On alarm button click")

        monitorServiceManager!!.sendAlarmEvent()
    }

    override var errorMessageView: TextView
        get() = lView!!.findViewById<View>(R.id.run_error_message) as TextView
        set(textView) {

        }


    fun setTaskFactory(taskFactory: IAsyncTaskFactory) {
        this.taskFactory = taskFactory
    }

    fun setObjectName(name: String) {
        this.objectName = name
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is MonitorServiceManager) {
            this.setMonitorServiceManager(context as MonitorServiceManager)
        }

        if (context is CustomLabelsManager) {
            this.setCustomLabelsManager(context as CustomLabelsManager)
        }
    }

    private fun setMonitorServiceManager(manager: MonitorServiceManager) {
        this.monitorServiceManager = manager
        this.monitorServiceManager!!.setMonitoringServiceListener(this)
    }

    private fun setCustomLabelsManager(manager: CustomLabelsManager) {
        manager.setCustomLabelsListener(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(TAG, "onCreateView: " + this)
        objectsManager = ObjectsManager.getInstance()
        objectsManager.changeCurrent(objectName!!)

        lView = inflater.inflate(R.layout.fragment_run, container, false)
        viewUpdater = DataViewUpdater(lView!!, activity as MainActivity)


        // register service listener
        LocalBroadcastManager.getInstance(activity).registerReceiver(viewUpdater,
                IntentFilter(NewData.NEW_DATA))
        LocalBroadcastManager.getInstance(activity).registerReceiver(viewUpdater,
                IntentFilter(LogMessage.LOG_EVENT))

        val isServiceRunning = monitorServiceManager!!.isServiceRunning()

        val serviceSwitch = lView!!.findViewById<SwitchCompat>(R.id.service_switch)
        serviceSwitch.isChecked = isServiceRunning

        if (!this.monitorServiceManager!!.isServiceStarted(objectName!!)) {
            if (this.monitorServiceManager!!.oneServiceStarted()) {
                //stop the service
                this.monitorServiceManager!!.stopMonitoringService()
            }
            //registerNewDevice();
            this.monitorServiceManager!!.startMonitoringService(objectName!!)
        }

        serviceSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                startMonitoringService()
            } else {
                stopMonitoringService()
            }
        }

        if (isServiceRunning) {
            val service = monitorServiceManager!!.monitoringService
            if (service != null) {
                this.onServiceStarted(service)
            }
        }


        // Alarm button
        val alarmButton = lView!!.findViewById<Button>(R.id.alarm_btn)
        alarmButton.setOnClickListener(onAlarmClick)

        // Make links clickable in info view.
        val infoMessageView = lView!!.findViewById<TextView>(R.id.run_info_message)
        infoMessageView.linksClickable = true
        infoMessageView.movementMethod = LinkMovementMethod.getInstance()

        // Might had those before initialization
        if (systemUid != null && systemName != null) {
            setLinkToSystem(systemUid, systemName)
        }

        phoneBtn = lView!!.findViewById(R.id.phone)
        objectBtn = lView!!.findViewById(R.id.`object`)
        phoneListView = lView!!.findViewById(R.id.phoneListView)
        objectListView = lView!!.findViewById(R.id.objectLstView)
        objectBtn.text = objectName
        phoneBtn.setBackgroundColor(resources.getColor(R.color.grey_1))
        phoneListView.visibility = View.VISIBLE
        objectListView.visibility = View.GONE

        phoneBtn.setOnClickListener {
            phoneListView.visibility = View.VISIBLE
            objectListView.visibility = View.GONE
            phoneBtn.isSelected = true
            phoneBtn.isPressed = true
            phoneBtn.setBackgroundColor(resources.getColor(R.color.grey_1))
            objectBtn.isSelected = false
            objectBtn.isPressed = false
            objectBtn.setBackgroundColor(resources.getColor(R.color.grey_4))
        }

        objectBtn.setOnClickListener {
            phoneListView.visibility = View.GONE
            objectListView.visibility = View.VISIBLE
            phoneBtn.isSelected = false
            phoneBtn.isPressed = false
            phoneBtn.setBackgroundColor(resources.getColor(R.color.grey_4))
            objectBtn.isSelected = true
            objectBtn.isPressed = true
            objectBtn.setBackgroundColor(resources.getColor(R.color.grey_1))
        }

        setCustomDataLabels()
        setPhoneDataLabels()


        return lView
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == AuthorizationActivity.REQUEST_AUTHORIZATION) {
            val auth = AuthUtils.activityResultAsAuthentication(requestCode, resultCode, data)
            if (auth != null) {
                authManager!!.onAuthentication(auth)
                syncWithAv(auth.accessToken)
            }
        }
    }

    private fun syncWithAv(token: String?) {

        val prefs = PreferenceUtils.getAvPhonePrefs(activity)
        val display = this

        val syncAvTask = taskFactory!!.syncAvTask(prefs.serverHost, token!!)


        val params = SyncWithAvParams()

        params.deviceId = DeviceInfo.getUniqueId(activity)
        params.imei = DeviceInfo.getIMEI(activity)
        params.deviceName = DeviceInfo.deviceName
        params.iccid = DeviceInfo.getICCID(activity)
        params.mqttPassword = prefs.password
        params.customData = PreferenceUtils.getCustomDataLabels(activity)
        //     params.current = ((MainActivity)getActivity()).current;
        params.activity = activity as MainActivity

        syncAvTask.execute(params)
        syncAvTask.addProgressListener { result ->
            syncAvTask.showResult(result, display, activity)

            if (!result.isError) {
                syncListener!!.invoke(result)
            }
        }


    }

    fun setLinkToSystem(systemUid: String?, systemName: String?) {

        if (lView == null || activity == null) {
            // View is unavailable, bear it in mind for later
            this.systemUid = systemUid
            this.systemName = systemName
            return
        }

        val infoMessageView = lView!!.findViewById<TextView>(R.id.run_info_message)

        val infoMessage: String
        if (systemUid != null) {

            val avPhonePrefs = PreferenceUtils.getAvPhonePrefs(activity)
            val link = String.format("https://%s/monitor/systems/systemDetails?uid=%s", avPhonePrefs.serverHost,
                    systemUid)

            infoMessage = getString(R.string.run_info_message_link, link, systemName)
            infoMessageView.text = Html.fromHtml(infoMessage)

        } else {
            infoMessage = getString(R.string.run_info_message, DeviceInfo.getUniqueId(activity))
            infoMessageView.text = infoMessage
        }

    }

    override fun onResume() {
        super.onResume()

        val isServiceRunning = monitorServiceManager!!.isServiceRunning()
        val serviceSwitch = serviceSwitch
        serviceSwitch.isChecked = isServiceRunning

        val systemUid = (activity as MainActivity).systemUid
        val systemName = (activity as MainActivity).systemName

        this.setLinkToSystem(systemUid, systemName)

    }

    private fun startMonitoringService() {
        val avPrefs = PreferenceUtils.getAvPhonePrefs(activity)
        if (!avPrefs.checkCredentials()) {
            PreferenceUtils.showMissingPrefsDialog(activity)
            val serviceSwitch = serviceSwitch
            serviceSwitch.isChecked = false
        } else {
            this.monitorServiceManager!!.startSendData()
        }
        this.monitorServiceManager!!.monitoringService!!.startSendData()

    }

    private fun stopMonitoringService() {
        this.monitorServiceManager!!.stopSendData()
        this.monitorServiceManager!!.monitoringService!!.stopSendData()
    }


    private fun setPhoneDataLabels() {
        val listPhone = ArrayList<HashMap<String, String>>()

        var temp: HashMap<String, String>


        temp = HashMap()
        temp[Tools.NAME] = "RSSI"
        temp[Tools.VALUE] = ""
        listPhone.add(temp)

        temp = HashMap()
        temp[Tools.NAME] = "Operator"
        temp[Tools.VALUE] = ""
        listPhone.add(temp)

        temp = HashMap()
        temp[Tools.NAME] = "Bytes Sent"
        temp[Tools.VALUE] = "0 Mo"
        listPhone.add(temp)

        temp = HashMap()
        temp[Tools.NAME] = "Bytes Received"
        temp[Tools.VALUE] = "0 Mo"
        listPhone.add(temp)

        temp = HashMap()
        temp[Tools.NAME] = "Network Type"
        temp[Tools.VALUE] = ""
        listPhone.add(temp)

        temp = HashMap()
        temp[Tools.NAME] = "Latitude"
        temp[Tools.VALUE] = ""
        listPhone.add(temp)

        temp = HashMap()
        temp[Tools.NAME] = "Longitude"
        temp[Tools.VALUE] = ""
        listPhone.add(temp)
        val adapter = RunListViewAdapter(activity, listPhone)
        phoneListView.adapter = adapter
        phoneListView.invalidateViews()

    }

    protected fun setCustomDataLabels() {
        val listObject = ArrayList<HashMap<String, String>>()


        objectsManager = ObjectsManager.getInstance()
        val `object` = objectsManager!!.getObjectByName(objectName!!)
        var temp: HashMap<String, String>
        for (data in `object`!!.datas) {
            temp = HashMap()
            temp[Tools.NAME] = data.name
            if (data.isInteger!!) {
                temp[Tools.VALUE] = data.current!!.toString()
            } else {
                temp[Tools.VALUE] = data.defaults
            }
            listObject.add(temp)
        }
        val adapter = RunListViewAdapter(activity, listObject)
        objectListView.adapter = adapter
        objectListView.invalidateViews()
    }

    override fun onServiceStarted(service: MonitoringService) {
        lView!!.findViewById<View>(R.id.toggle_to_start).visibility = View.GONE
        lView!!.findViewById<View>(R.id.started_since).visibility = View.VISIBLE
        lView!!.findViewById<View>(R.id.service_log).visibility = View.VISIBLE
        lView!!.findViewById<View>(R.id.alarm_log).visibility = View.VISIBLE
        viewUpdater!!.onStart(service.startedSince, service.lastData, service.lastLog,
                service.lastRun)
    }

    override fun onServiceStopped(service: MonitoringService) {
        lView!!.findViewById<View>(R.id.toggle_to_start).visibility = View.VISIBLE
        lView!!.findViewById<View>(R.id.started_since).visibility = View.GONE
        lView!!.findViewById<View>(R.id.service_log).visibility = View.GONE
        lView!!.findViewById<View>(R.id.alarm_log).visibility = View.GONE
        viewUpdater!!.onStop()
    }

    override fun onCustomLabelsChanged() {
        // The activity can be null if the change is done while the fragment is not active.
        // This can wait for the activity to be resumed.
        if (activity != null) {
            setCustomDataLabels()
        }
    }

    companion object {
        private val TAG = "RunFragment"

        private val LOGTAG = RunFragment::class.java.name
    }
}
