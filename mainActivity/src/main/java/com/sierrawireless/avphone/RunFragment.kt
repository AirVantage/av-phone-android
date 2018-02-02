package com.sierrawireless.avphone

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.sierrawireless.avphone.adapter.RunListViewAdapter
import com.sierrawireless.avphone.auth.AuthUtils
import com.sierrawireless.avphone.model.AvPhoneObjectData
import com.sierrawireless.avphone.service.LogMessage
import com.sierrawireless.avphone.service.MonitoringService
import com.sierrawireless.avphone.service.NewData
import com.sierrawireless.avphone.task.IAsyncTaskFactory
import com.sierrawireless.avphone.task.SyncWithAvParams
import com.sierrawireless.avphone.tools.Tools
import kotlinx.android.synthetic.main.fragment_run.*
import net.airvantage.utils.PreferenceUtils
import java.util.*
import kotlin.concurrent.schedule

open class RunFragment : AvPhoneFragment(), MonitorServiceListener, CustomLabelsListener {

    private var viewUpdater: DataViewUpdater? = null

    private var lView: View? = null

    private var monitorServiceManager: MonitorServiceManager? = null

    private var systemUid: String? = null
    private var systemName: String? = null

    private var taskFactory: IAsyncTaskFactory? = null
    private var objectName: String? = null
    private var objectsManager: ObjectsManager = ObjectsManager.getInstance()


    private var timer:TimerTask? = null

    // Alarm button
    private var onAlarmClick: View.OnClickListener = View.OnClickListener {
        monitorServiceManager!!.sendAlarmEvent()
    }

    override var errorMessageView: TextView
        get() = run_error_message
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

        lView = inflater.inflate(R.layout.fragment_run, container, false)

        return lView
    }

    override fun onStart() {
        super.onStart()
        objectsManager = ObjectsManager.getInstance()
        objectsManager.changeCurrent(objectName!!)

        viewUpdater = DataViewUpdater(lView!!, activity as MainActivity)


        // register service listener
        LocalBroadcastManager.getInstance(activity).registerReceiver(viewUpdater,
                IntentFilter(NewData.NEW_DATA))
        LocalBroadcastManager.getInstance(activity).registerReceiver(viewUpdater,
                IntentFilter(LogMessage.LOG_EVENT))

        val isServiceRunning = monitorServiceManager!!.isServiceRunning()


        service_switch.isChecked = isServiceRunning

        if (!this.monitorServiceManager!!.isServiceStarted(objectName!!)) {
            if (this.monitorServiceManager!!.oneServiceStarted()) {
                //stop the service
                this.monitorServiceManager!!.stopMonitoringService()
            }
            //registerNewDevice();
            this.monitorServiceManager!!.startMonitoringService(objectName!!)
        }

        service_switch.setOnCheckedChangeListener { _, isChecked ->
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
        alarm_btn.setOnClickListener(onAlarmClick)

        // Make links clickable in info view.

        run_info_message.linksClickable = true
        run_info_message.movementMethod = LinkMovementMethod.getInstance()

        // Might had those before initialization
        if (systemUid != null && systemName != null) {
            setLinkToSystem(systemUid, systemName)
        }

        `object`.text = objectName
        phone.setBackgroundColor(ContextCompat.getColor(MainActivity.instance.baseContext, R.color.grey_1))
        phoneListView.visibility = View.VISIBLE
        objectLstView.visibility = View.GONE

        phone.setOnClickListener {
            phoneListView.visibility = View.VISIBLE
            objectLstView.visibility = View.GONE
            phone.isSelected = true
            phone.isPressed = true
            phone.setBackgroundColor(ContextCompat.getColor(MainActivity.instance.baseContext, R.color.grey_1))
            `object`.isSelected = false
            `object`.isPressed = false
            `object`.setBackgroundColor(ContextCompat.getColor(MainActivity.instance.baseContext, R.color.grey_4))
        }

        `object`.setOnClickListener {
            phoneListView.visibility = View.GONE
            objectLstView.visibility = View.VISIBLE
            phone.isSelected = false
            phone.isPressed = false
            phone.setBackgroundColor(ContextCompat.getColor(MainActivity.instance.baseContext, R.color.grey_4))
            `object`.isSelected = true
            `object`.isPressed = true
            `object`.setBackgroundColor(ContextCompat.getColor(MainActivity.instance.baseContext, R.color.grey_1))
        }

        setCustomDataLabels()
        setPhoneDataLabels()


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

        val syncAvTask = taskFactory!!.syncAvTask(prefs.serverHost!!, token!!)


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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                infoMessageView.text = Html.fromHtml(infoMessage, Html.FROM_HTML_MODE_LEGACY)
            }else{
                @Suppress("DEPRECATION")
                infoMessageView.text = Html.fromHtml(infoMessage)
            }

        } else {
            infoMessage = getString(R.string.run_info_message, DeviceInfo.getUniqueId(activity))
            infoMessageView.text = infoMessage
        }

    }

    override fun onResume() {
        super.onResume()

        val isServiceRunning = monitorServiceManager!!.isServiceRunning()
        service_switch.isChecked = isServiceRunning

        val systemUid = (activity as MainActivity).systemUid
        val systemName = (activity as MainActivity).systemName

        this.setLinkToSystem(systemUid, systemName)
        startTimer()

    }

    override fun onPause() {
        super.onPause()
        stopTimer()
    }

    private fun startMonitoringService() {
        val avPrefs = PreferenceUtils.getAvPhonePrefs(activity)
        if (!avPrefs.checkCredentials()) {
            PreferenceUtils.showMissingPrefsDialog(activity)
            service_switch.isChecked = false
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

        var temp: HashMap<String, String> = HashMap()


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

    private fun setCustomDataLabels() {
        val listObject = ArrayList<HashMap<String, String>>()


        objectsManager = ObjectsManager.getInstance()
        val `object` = objectsManager.getObjectByName(objectName!!)
        var temp: HashMap<String, String>
        for (data in `object`!!.datas) {
            temp = HashMap()
            temp[Tools.NAME] = data.name
            if (data.isInteger) {
                temp[Tools.VALUE] = data.current!!.toString()
            } else {
                temp[Tools.VALUE] = data.defaults
            }
            listObject.add(temp)
        }
        val adapter = RunListViewAdapter(activity, listObject)
        objectLstView.adapter = adapter
        objectLstView.invalidateViews()
    }

    override fun onServiceStarted(service: MonitoringService) {
        toggle_to_start.visibility = View.GONE
        started_since.visibility = View.VISIBLE
        service_log.visibility = View.VISIBLE
        alarm_log.visibility = View.VISIBLE
        viewUpdater!!.onStart(service.startedSince, service.lastData, service.lastLog,
                service.lastRun)
    }

    override fun onServiceStopped(service: MonitoringService) {
        toggle_to_start.visibility = View.VISIBLE
        started_since.visibility = View.GONE
        service_log.visibility = View.GONE
        alarm_log.visibility = View.GONE
        viewUpdater!!.onStop()
    }

    override fun onCustomLabelsChanged() {
        // The activity can be null if the change is done while the fragment is not active.
        // This can wait for the activity to be resumed.
        if (activity != null) {
            setCustomDataLabels()
        }
    }


    // Manage object data for action


    private fun startTimer() {

        Log.d(TAG, "custom data timer started for " + objectName)

        timer = Timer().schedule(Tools.rand(1000, 5000)) {
            execMode()
        }
    }

    private fun stopTimer() {
        Log.d(TAG, "custom data timer stopped for " + objectName)
        if (timer != null) {
            timer!!.cancel()
        }
    }

    private fun execMode() {
        val obj =  objectsManager.getObjectByName(objectName!!)!!

        for (data in obj.datas) {

            @Suppress("UNUSED_EXPRESSION")
            when (data.mode){
                AvPhoneObjectData.Mode.UP -> if (Tools.rand(0, 2000) > 500) data.execMode()
                AvPhoneObjectData.Mode.DOWN -> if (Tools.rand(0, 2000) > 1500) data.execMode()
                else -> ""
            }

        }
        objectsManager.saveOnPref()
        MainActivity.instance.runOnUiThread { setCustomDataLabels() }
        // MainActivity.instance.setCustomDataLabels(run)
        startTimer()

    }
    companion object {
        const val TAG = "RunFragment"
    }


}

