package com.sierrawireless.avphone

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import com.crashlytics.android.Crashlytics
import com.sierrawireless.avphone.activity.AuthorizationActivity
import com.sierrawireless.avphone.activity.MainActivity
import com.sierrawireless.avphone.activity.ObjectConfigureActivity
import com.sierrawireless.avphone.adapter.ObjectAdapter
import com.sierrawireless.avphone.auth.AuthUtils
import com.sierrawireless.avphone.task.IAsyncTaskFactory
import com.sierrawireless.avphone.task.SyncWithAvParams
import com.sierrawireless.avphone.task.UpdateParams
import com.sierrawireless.avphone.tools.DeviceInfo
import kotlinx.android.synthetic.main.fragment_configure.*
import net.airvantage.utils.PreferenceUtils
import java.util.*

open class ConfigureFragment : AvPhoneFragment() {
    override var errorMessageView: TextView? = null

    private var objectsManager: ObjectsManager? = null
    private var menu: ArrayList<String> = ArrayList()
    enum class Mode {
        DELETE,
        SYNC,
        UPDATE

    }
    internal var delete: Mode = Mode.SYNC

    private var lView: View? = null

    var current = 0


    private var taskFactory: IAsyncTaskFactory? = null

    fun setTaskFactory(taskFactory: IAsyncTaskFactory) {
        this.taskFactory = taskFactory
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {


        lView = inflater.inflate(R.layout.fragment_configure, container, false)



        return lView
    }

    private fun reloadMenu(){
        menu = ArrayList()

        for (obj in objectsManager!!.objects) {
            menu.add(obj.name!!)
        }


        val adapter = ObjectAdapter(activity, R.layout.menu_objects, menu)
        objectConfigure.adapter = adapter
    }
    override fun onStart() {
        instance = this
        super.onStart()
        objectsManager = ObjectsManager.getInstance()
        errorMessageView = configure_error_message


       reloadMenu()

        objectConfigure.onItemClickListener = AdapterView.OnItemClickListener { _, view, i, _ ->
            val deleteActionBtn: Button = view.findViewById(R.id.menuDeleteActionBtn)
            if (deleteActionBtn.visibility == View.VISIBLE) {
                val deleteBtn:ImageButton = view.findViewById(R.id.menuDeleteBtn)
                deleteBtn.visibility = View.VISIBLE
                deleteActionBtn.visibility = View.GONE
            } else {
                startObjectConfigure(i)
            }
        }

        doneConfigureBtn.setOnClickListener { (activity as MainActivity).goLastFragment() }

        addConfigureBtn.setOnClickListener { _ ->
            startObjectConfigure(-1)
        }

        resync.setOnClickListener { _ ->
            resyncAll()
        }
    }

    private fun startObjectConfigure(position: Int) {

        //Open a new intent with the selected Object
        val intent = Intent(view?.context, ObjectConfigureActivity::class.java)
        intent.putExtra(INDEX, position)

        startActivityForResult(intent, CONFIGURE)
    }

    override fun onStop() {
        super.onStop()
        instance = null
    }

    private fun checkCredentials(): Boolean {

        val prefs = PreferenceUtils.getAvPhonePrefs(activity)

        if (!prefs.checkCredentials()) {
            PreferenceUtils.showMissingPrefsDialog(activity)
            return false
        }

        return true

    }

    private fun resyncAll() {
        current = 0
        // first check credential
        if (checkCredentials()) {
            val auth = authManager!!.authentication
            if (!auth!!.isExpired) {
                updateAllSystem(auth.accessToken!!)
            } else {
                this.delete = Mode.UPDATE
                requestAuthentication()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data == null) {
            Crashlytics.log(Log.ERROR, TAG, "data is null ??? why ????")
            return
        }
        if (requestCode == AuthorizationActivity.REQUEST_AUTHORIZATION) {

            val auth = AuthUtils.activityResultAsAuthentication(requestCode, resultCode, data)
            if (auth != null) {
                authManager!!.onAuthentication(auth)

                when (delete) {
                    Mode.DELETE -> deleteSysten(auth.accessToken!!)
                    Mode.SYNC -> syncWithAv(auth.accessToken!!)
                    Mode.UPDATE -> updateAllSystem(auth.accessToken!!)
                }
            }
        } else if (requestCode == CONFIGURE) {
            if (resultCode == Activity.RESULT_OK) {
                this.delete = Mode.SYNC
                val position = data.getIntExtra(POS, -1)
                //set current and start synchronization

                objectsManager!!.setSavedPosition(position)

                if (checkCredentials()) {
                    val auth = authManager!!.authentication
                    if (!auth!!.isExpired) {
                        syncWithAv(auth.accessToken!!)
                    } else {
                        this.delete = Mode.SYNC
                        requestAuthentication()
                    }
                }
            }
            MainActivity.instance.loadMenu(false)
            reloadMenu()
            objectConfigure.invalidate()

        }
    }

    internal fun delete() {
        if (checkCredentials()) {
            val auth = authManager!!.authentication
            if (!auth!!.isExpired) {
                deleteSysten(auth.accessToken!!)
            } else {
                this.delete = Mode.DELETE
                requestAuthentication()
            }
        }
    }

    private fun updateAllSystem(token: String) {

        if (current >= objectsManager!!.objects.size) return

        objectsManager!!.setSavedPosition(current)

        val prefs = PreferenceUtils.getAvPhonePrefs(activity)

        val display = this

        val updateTask = taskFactory!!.updateTask(prefs.serverHost!!, token)

        val updateParams = UpdateParams()
        updateParams.deviceId = DeviceInfo.getUniqueId(activity)
        updateParams.imei = DeviceInfo.getIMEI(activity)
        updateParams.deviceName = DeviceInfo.deviceName
        updateParams.iccid = DeviceInfo.getICCID(activity)
        updateParams.mqttPassword = prefs.password
        updateParams.customData = PreferenceUtils.getCustomDataLabels(activity)

        updateTask.execute(updateParams)

        updateTask.addProgressListener { result ->

            updateTask.showResult(result, objectsManager!!.savedObjectName!!, display, activity)
            // Update next system
            current++
            updateAllSystem(token)


        }


    }

    private fun deleteSysten(token: String) {
        val prefs = PreferenceUtils.getAvPhonePrefs(activity)

        val display = this

        val deleteTask = taskFactory!!.deleteSystemTak(prefs.serverHost!!, token)
        deleteTask.execute()

        deleteTask.addProgressListener({ result ->
            if (delete == Mode.DELETE) {
                objectsManager!!.removeSavedObject()
            }

            deleteTask.showResult(result, display, activity)
            MainActivity.instance.loadMenu(false)
            reloadMenu()
            objectConfigure.invalidate()

        })


    }

    private fun syncWithAv(token: String) {

        val prefs = PreferenceUtils.getAvPhonePrefs(activity)

        val display = this

        val syncTask = taskFactory!!.syncAvTask(prefs.serverHost!!, token)

        val syncParams = SyncWithAvParams()
        syncParams.deviceId = DeviceInfo.getUniqueId(activity)
        syncParams.imei = DeviceInfo.getIMEI(activity)
        syncParams.deviceName = DeviceInfo.deviceName
        syncParams.iccid = DeviceInfo.getICCID(activity)
        syncParams.mqttPassword = prefs.password
        syncParams.customData = PreferenceUtils.getCustomDataLabels(activity)

        syncTask.execute(syncParams)

        syncTask.addProgressListener { result ->
            if (delete == Mode.DELETE) {
                objectsManager!!.removeSavedObject()
            }

            syncTask.showResult(result, display, activity)
            MainActivity.instance.loadMenu(false)

            if (!result.isError) {
                syncListener!!.invoke(result)
            }
        }

    }

    companion object {
        var INDEX = "index"
        var CONFIGURE = 0
        var POS = "position"
        @SuppressLint("StaticFieldLeak")
        var instance: ConfigureFragment? = null
        private val TAG = ConfigureFragment::class.simpleName
    }

}
