package com.sierrawireless.avphone

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.TextView
import com.baoyz.swipemenulistview.SwipeMenuCreator
import com.baoyz.swipemenulistview.SwipeMenuItem
import com.sierrawireless.avphone.adapter.ObjectAdapter
import com.sierrawireless.avphone.auth.AuthUtils
import com.sierrawireless.avphone.task.IAsyncTaskFactory
import com.sierrawireless.avphone.task.SyncWithAvParams
import com.sierrawireless.avphone.tools.Tools
import kotlinx.android.synthetic.main.fragment_configure.*
import net.airvantage.utils.PreferenceUtils
import java.util.*

open class ConfigureFragment : AvPhoneFragment() {
    override var errorMessageView: TextView
        get() = configure_error_message
        set(value) {}

    private var objectsManager: ObjectsManager? = null
    private var menu: ArrayList<String> = ArrayList()
    private var delete: Boolean = false

    private var lView: View? = null


    private var taskFactory: IAsyncTaskFactory? = null

    fun setTaskFactory(taskFactory: IAsyncTaskFactory) {
        this.taskFactory = taskFactory
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {


        lView = inflater.inflate(R.layout.fragment_configure, container, false)



        return lView
    }

    override fun onStart() {
        super.onStart()
        objectsManager = ObjectsManager.getInstance()


        menu = ArrayList()

        for (obj in objectsManager!!.objects) {
            menu.add(obj.name!!)
        }



        val creator = SwipeMenuCreator { menu ->
            when (menu.viewType) {
                0 -> {
                    // create "delete" item
                    val deleteItem = SwipeMenuItem(
                            activity.baseContext)
                    // set item background
                    deleteItem.background = ColorDrawable(Color.rgb(0xF9,
                            0x3F, 0x25))
                    // set item width
                    deleteItem.width = Tools.dp2px(
                            activity.baseContext).toInt()
                    // set a icon
                    deleteItem.setIcon(android.R.drawable.ic_menu_delete)
                    // add to menu
                    menu.addMenuItem(deleteItem)
                }
            }
        }


        objectConfigure.setOnMenuItemClickListener { position, _, index ->
            when (index) {
                0 -> {
                    //delete
                    objectsManager!!.setSavedPosition(position)
                    delete()
                    delete = true
                }
            }
            false
        }

        objectConfigure.setMenuCreator(creator)

        val adapter = ObjectAdapter(activity, android.R.layout.simple_list_item_1, menu)

        objectConfigure.adapter = adapter

        objectConfigure.onItemClickListener = AdapterView.OnItemClickListener { _, view, i, _ ->
            //Open a new intent with the selected Object
            Log.d(TAG, "onItemClick: " + i + " " + menu[i])
            val intent = Intent(view.context, ObjectConfigureActivity::class.java)
            intent.putExtra(INDEX, i)

            startActivityForResult(intent, CONFIGURE)
        }


        doneConfigureBtn.setOnClickListener { (activity as MainActivity).goHomeFragment() }

        addConfigureBtn.setOnClickListener { view ->
            val intent = Intent(view.context, ObjectConfigureActivity::class.java)
            intent.putExtra(INDEX, -1)

            startActivityForResult(intent, CONFIGURE)
        }

    }

    private fun checkCredentials(): Boolean {

        val prefs = PreferenceUtils.getAvPhonePrefs(activity)

        if (!prefs.checkCredentials()) {
            PreferenceUtils.showMissingPrefsDialog(activity)
            return false
        }

        return true

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == AuthorizationActivity.REQUEST_AUTHORIZATION) {
            val auth = AuthUtils.activityResultAsAuthentication(requestCode, resultCode, data)
            if (auth != null) {
                authManager!!.onAuthentication(auth)
                if (delete) {
                    deleteSysten(auth.accessToken!!)
                } else {
                    syncWithAv(auth.accessToken!!)
                }
            }
        } else if (requestCode == CONFIGURE) {
            if (resultCode == Activity.RESULT_OK) {
                this.delete = false
                val position = data.getIntExtra(POS, -1)
                //set current and start synchronization

                objectsManager!!.setSavedPosition(position)

                if (checkCredentials()) {
                    val auth = authManager!!.authentication
                    if (!auth!!.isExpired) {
                        syncWithAv(auth.accessToken!!)
                    } else {
                        this.delete = false
                        requestAuthentication()
                    }
                }
            }
            MainActivity.instance.loadMenu()


        }
    }

    private fun delete() {
        if (checkCredentials()) {
            val auth = authManager!!.authentication
            if (!auth!!.isExpired) {
                deleteSysten(auth.accessToken!!)
            } else {
                this.delete = true
                requestAuthentication()
            }
        }
    }

    private fun deleteSysten(token: String) {
        val prefs = PreferenceUtils.getAvPhonePrefs(activity)

        val display = this

        val deleteTask = taskFactory!!.deleteSystemTak(prefs.serverHost, token)
        deleteTask.execute()

        deleteTask.addProgressListener({ result ->
            Log.d(TAG, "onSynced: ICI")
            if (delete) {
                objectsManager!!.removeSavedObject()
            }

            deleteTask.showResult(result, display, activity)
            MainActivity.instance.loadMenu()})

    }

    private fun syncWithAv(token: String) {

        val prefs = PreferenceUtils.getAvPhonePrefs(activity)

        val display = this

        val syncTask = taskFactory!!.syncAvTask(prefs.serverHost, token)

        val syncParams = SyncWithAvParams()
        syncParams.deviceId = DeviceInfo.getUniqueId(activity)
        syncParams.imei = DeviceInfo.getIMEI(activity)
        syncParams.deviceName = DeviceInfo.deviceName
        syncParams.iccid = DeviceInfo.getICCID(activity)
        syncParams.mqttPassword = prefs.password
        syncParams.customData = PreferenceUtils.getCustomDataLabels(activity)

        syncTask.execute(syncParams)

        syncTask.addProgressListener { result ->
            Log.d(TAG, "onSynced: ICI")
            if (delete) {
                objectsManager!!.removeSavedObject()
            }

            syncTask.showResult(result, display, activity)
            MainActivity.instance.loadMenu()

            if (!result.isError) {
                syncListener!!.invoke(result)
            }
        }

    }

    companion object {
        private const val TAG = "ConfigureFragment"
        var INDEX = "index"
        var CONFIGURE = 0
        var POS = "position"
    }

}
