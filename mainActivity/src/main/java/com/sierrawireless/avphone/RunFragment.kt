package com.sierrawireless.avphone

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.sierrawireless.avphone.activity.AuthorizationActivity
import com.sierrawireless.avphone.activity.MainActivity
import com.sierrawireless.avphone.adapter.RunListViewAdapter
import com.sierrawireless.avphone.auth.AuthUtils
import com.sierrawireless.avphone.listener.CustomLabelsListener
import com.sierrawireless.avphone.listener.MonitorServiceListener
import com.sierrawireless.avphone.model.AvPhoneObjectData
import com.sierrawireless.avphone.service.LogMessage
import com.sierrawireless.avphone.service.MonitorServiceManager
import com.sierrawireless.avphone.service.MonitoringService
import com.sierrawireless.avphone.service.NewData
import com.sierrawireless.avphone.task.IAsyncTaskFactory
import com.sierrawireless.avphone.task.SyncWithAvParams
import com.sierrawireless.avphone.tools.DeviceInfo
import com.sierrawireless.avphone.tools.Tools
import kotlinx.android.synthetic.main.fragment_run.*
import net.airvantage.utils.PreferenceUtils
import org.jetbrains.anko.alert
import java.util.*
import kotlin.concurrent.schedule


@Suppress("DEPRECATION")
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
        if (this.monitorServiceManager!!.isServiceStarted(objectName!!)) {
            objectsManager = ObjectsManager.getInstance()
            val obj = objectsManager.currentObject!!
            obj.alarm = !obj.alarm
            if (!monitorServiceManager!!.sendAlarmEvent(obj.alarm, obj.alarmName)) {
                obj.alarm = !obj.alarm
            } else {
                objectsManager.saveOnPref()
                setAlarmButton()
            }
        }else{
            alert("A run already exist for " + MainActivity.instance.startObjectName, "Alert") {
                positiveButton("OK") {

                }
            }.show()
        }
    }

    private fun setAlarmButton() {
        val obj = objectsManager.currentObject!!
        alarm_btn.text = if (obj.alarm) {
            getString(R.string.cancel)
        }else{
            getString(R.string.reaise)
        }
    }

    override var errorMessageView: TextView?
        get() = run_error_message
        set(textView) {

        }

    fun setTaskFactory(taskFactory: IAsyncTaskFactory) {
        this.taskFactory = taskFactory
    }

    fun setObjectName(name: String) {
        this.objectName = name
    }

    @Suppress("OverridingDeprecatedMember")
    override fun onAttach(activity:Activity) {
        super.onAttach(activity)

        Log.d(TAG, "OnAttach Called")

        if (activity is MonitorServiceManager) {
            this.setMonitorServiceManager(activity as MonitorServiceManager)
        }

        if (activity is CustomLabelsManager) {
            this.setCustomLabelsManager(activity as CustomLabelsManager)
        }
    }

    @TargetApi(23)
    override fun onAttach(context: Context) {
        super.onAttach(context)

        Log.d(TAG, "OnAttach Called")

        if (context is MonitorServiceManager) {
            this.setMonitorServiceManager(context as MonitorServiceManager)
        }

        if (context is CustomLabelsManager) {
            this.setCustomLabelsManager(context as CustomLabelsManager)
        }
    }

    override fun onDetach() {
        super.onDetach()
        Log.d(TAG, "On detach called")
    }

    private fun setMonitorServiceManager(manager: MonitorServiceManager) {
        Log.d(TAG, "Set monitoring service")
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
        if (objectName != null) {
            objectsManager.changeCurrent(objectName!!)
        }else{
            Log.d(TAG, "retrieve canceled objectName")
            objectName = MainActivity.instance.fragmentsList!![MainActivity.instance.lastPosition].name
        }
        viewUpdater = DataViewUpdater(lView!!, activity as MainActivity)


        // register service listener
        LocalBroadcastManager.getInstance(activity).registerReceiver(viewUpdater!!,
                IntentFilter(NewData.NEW_DATA))
        LocalBroadcastManager.getInstance(activity).registerReceiver(viewUpdater!!,
                IntentFilter(LogMessage.LOG_EVENT))

        /* if service is running and it's myself or that is not running */
        if (monitorServiceManager != null &&
                ((monitorServiceManager!!.isServiceRunning() &&  monitorServiceManager!!.isServiceRunning(objectName!!)) ||
                !monitorServiceManager!!.isServiceRunning())){
            if (!this.monitorServiceManager!!.isServiceStarted(objectName!!)) {
                if (this.monitorServiceManager!!.oneServiceStarted()) {
                    //stop the service
                    this.monitorServiceManager!!.stopMonitoringService()
                }
                //registerNewDevice();
                this.monitorServiceManager!!.startMonitoringService(objectName!!)
            }
        }
        val isServiceRunning = if (monitorServiceManager != null) {
            monitorServiceManager!!.isServiceRunning(objectName!!)
        }else{
            false
        }
        service_switch.isChecked = isServiceRunning

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

        // Might had those before initialization
        if (systemUid != null && systemName != null) {
            setLinkToSystem(systemUid, systemName)
        }

        obj.text = objectName
        phone.setBackgroundColor(ContextCompat.getColor(MainActivity.instance.baseContext, R.color.grey_1))
        phoneListView.visibility = View.VISIBLE
        objectLstView.visibility = View.GONE

        phone.setOnClickListener {
            phoneListView.visibility = View.VISIBLE
            objectLstView.visibility = View.GONE
            phone.isSelected = true
            phone.isPressed = true
            phone.setBackgroundColor(ContextCompat.getColor(MainActivity.instance.baseContext, R.color.grey_1))
            obj.isSelected = false
            obj.isPressed = false
            obj.setBackgroundColor(ContextCompat.getColor(MainActivity.instance.baseContext, R.color.grey_4))
        }

        obj.setOnClickListener {
            phoneListView.visibility = View.GONE
            objectLstView.visibility = View.VISIBLE
            phone.isSelected = false
            phone.isPressed = false
            phone.setBackgroundColor(ContextCompat.getColor(MainActivity.instance.baseContext, R.color.grey_4))
            obj.isSelected = true
            obj.isPressed = true
            obj.setBackgroundColor(ContextCompat.getColor(MainActivity.instance.baseContext, R.color.grey_1))
        }

        setCustomDataLabels()
        setPhoneDataLabels()
    }

    override fun onResume() {
        Log.d(TAG, "On Resume Called")
        super.onResume()
        if (monitorServiceManager == null){
            // We have to restart monitorServiceManager
            Log.d(TAG, "MonitoringServiceManager is null")
            return
        }
        if (objectName == null) {
            Log.d(TAG, "objectName is null retreive it from objectManager")
            objectName = objectsManager.currentObject!!.name
        }
        val isServiceRunning = monitorServiceManager!!.isServiceRunning(objectName!!)
        service_switch.isChecked = isServiceRunning
        val systemUid = (activity as MainActivity).systemUid
        val systemName = (activity as MainActivity).systemName

        this.setLinkToSystem(systemUid, systemName)
        startTimer()
        monitorServiceManager?.start()
        setAlarmButton()
        setPhoneDataLabels()
    }

    override fun onPause() {
        Log.d(TAG, "on Pause Called")
        super.onPause()
        stopTimer()
        this.monitorServiceManager?.cancel()
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

    private fun setLinkToSystem(systemUid: String?, systemName: String?) {

        if (lView == null || activity == null) {
            // View is unavailable, bear it in mind for later
            this.systemUid = systemUid
            this.systemName = systemName
        }
        val avPhonePrefs = PreferenceUtils.getAvPhonePrefs(MainActivity.instance)
        objectsManager = ObjectsManager.getInstance()
        val obj = objectsManager.currentObject!!
        val link = String.format("https://%s/monitor/systems/systemDetails?uid=%s", avPhonePrefs.serverHost, obj.systemUid)

        airvantage.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
            startActivity(browserIntent)
        }

        // set button to open Airvantage

        return
    }

    private fun startMonitoringService() {
        val avPrefs = PreferenceUtils.getAvPhonePrefs(activity)
        if (!avPrefs.checkCredentials()) {
            PreferenceUtils.showMissingPrefsDialog(activity)
            service_switch.isChecked = false
        } else {
           if (this.monitorServiceManager!!.startSendData(objectName!!)) {
               this.monitorServiceManager!!.monitoringService!!.startSendData()
           }else{
               service_switch.isChecked = false
           }
        }

    }

    private fun stopMonitoringService() {
        this.monitorServiceManager!!.stopSendData()
        this.monitorServiceManager!!.monitoringService!!.stopSendData()
    }




    private fun setPhoneDataLabels() {
        Log.d(TAG, "setPhoneDataLabels called for " + objectName)
        val adapter = RunListViewAdapter(activity,  arrayListOf(
                hashMapOf(Tools.NAME to "RSSI",
                          Tools.VALUE to "0 dBm"),
                hashMapOf(Tools.NAME to "Operator",
                          Tools.VALUE to "Unknown"),
                hashMapOf(Tools.NAME to "Bytes Sent",
                        Tools.VALUE to "0 Mo"),
                hashMapOf(Tools.NAME to "Bytes received",
                        Tools.VALUE to "0 Mo"),
                hashMapOf(Tools.NAME to "Network Type",
                        Tools.VALUE to "Unknown"),
                hashMapOf(Tools.NAME to "Latitude",
                        Tools.VALUE to "Unknown"),
                hashMapOf(Tools.NAME to "Longitude",
                        Tools.VALUE to "Unknown")))
        phoneListView.adapter = adapter
        phoneListView.invalidateViews()

    }

    private fun setCustomDataLabels() {
        val listObject = ArrayList<HashMap<String, String>>()

        objectsManager = ObjectsManager.getInstance()
        val obj = objectsManager.getObjectByName(objectName!!)
        var temp: HashMap<String, String>
        for (data in obj!!.datas) {
            temp = HashMap()
            temp[Tools.NAME] = data.name
            if (data.mode != AvPhoneObjectData.Mode.None) {
                temp[Tools.VALUE] = data.current!!.toString()
            } else {
                temp[Tools.VALUE] = data.defaults
            }
            listObject.add(temp)
        }
        val adapter = RunListViewAdapter(activity, listObject)
        objectLstView?.adapter = adapter
        objectLstView?.invalidateViews()
    }

    override fun onServiceStarted(service: MonitoringService) {
        Log.d(TAG, "OnServiceStarted for " + objectName)
        toggle_to_start?.visibility = View.GONE
        started_since?.visibility = View.VISIBLE
        service_log?.visibility = View.VISIBLE
        alarm_log?.visibility = View.VISIBLE
        viewUpdater?.onStart(service.startedSince, service.lastData, service.lastLog, service.lastAlarmLog,
                service.lastRun)
    }

    override fun onServiceStopped(service: MonitoringService) {
        toggle_to_start?.visibility = View.VISIBLE
        started_since?.visibility = View.GONE
        service_log?.visibility = View.GONE
        alarm_log?.visibility = View.GONE
        viewUpdater?.onStop()
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
       // Log.i(TAG, "custom data timer started for " + objectName)

        timer = Timer().schedule(Tools.rand(1000, 5000)) {
            execMode()
        }
    }

    private fun stopTimer() {
       // Log.i(TAG, "custom data timer stopped for " + objectName)
        if (timer != null) {
            timer!!.cancel()
        }
    }

    private fun execMode() {
        val obj =  objectsManager.getObjectByName(objectName!!)!!

        // The return code is not used
        // As for the default we don't change the value
        // We don't need anything is for this the ""
        for (data in obj.datas) {
            @Suppress("UNUSED_EXPRESSION")
            when (data.mode){
                AvPhoneObjectData.Mode.UP -> if (Tools.rand(0, 2000) > 500) data.execMode()
                AvPhoneObjectData.Mode.DOWN -> if (Tools.rand(0, 2000) > 1500) data.execMode()
                AvPhoneObjectData.Mode.RANDOM -> if (Tools.rand(0, 2000) > 500) data.execMode()
                else -> ""
            }

        }
        objectsManager.saveOnPref()
        startTimer()
    }

    companion object {
        private val TAG = RunFragment::class.simpleName
    }
}

