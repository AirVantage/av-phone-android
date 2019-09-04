package com.sierrawireless.avphone

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.crashlytics.android.Crashlytics
import com.sierrawireless.avphone.activity.MainActivity
import com.sierrawireless.avphone.auth.AuthUtils
import com.sierrawireless.avphone.auth.Authentication
import com.sierrawireless.avphone.message.IMessageDisplayer
import com.sierrawireless.avphone.task.IAsyncTaskFactory
import com.sierrawireless.avphone.task.SyncWithAvListener
import com.sierrawireless.avphone.task.SyncWithAvParams
import com.sierrawireless.avphone.tools.DeviceInfo
import kotlinx.android.synthetic.main.fragment_home.*
import net.airvantage.model.User
import net.airvantage.utils.PreferenceUtils

@Suppress("UNUSED_PARAMETER")
class HomeFragment : AvPhoneFragment(), IMessageDisplayer {
    private var lView: View? = null
    private var authForSync: Authentication? = null
    private var retrySync: Boolean = false
    private var taskFactory: IAsyncTaskFactory? = null
    private var user: User? = null
    private val infoMessageView: TextView?
        get() = home_info_message

    override var errorMessageView: TextView?
        get() = home_error_message
        set(textView) {
        }

    fun setTaskFactory(taskFactory: IAsyncTaskFactory) {
        this.taskFactory = taskFactory
        if (retrySync) {
            retrySync = false
            syncWithAv(authForSync)
        }
    }

    @Suppress("OverridingDeprecatedMember", "DEPRECATION", "UNCHECKED_CAST")
    override fun onAttach(activity: Activity) {
        super.onAttach(activity)


        syncListener = activity as SyncWithAvListener

    }

    @TargetApi(23)
    override fun onAttach(context: Context) {
        super.onAttach(context)

        @Suppress("UNCHECKED_CAST")
        syncListener = context as SyncWithAvListener
    }


    @SuppressWarnings("deprecation")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        lView = inflater.inflate(R.layout.fragment_home, container, false)

        return lView
    }

    override fun onStart() {
        super.onStart()
        val loginMessage = home_login_message
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            loginMessage.text = Html.fromHtml(getString(R.string.home_login_message_str), Html.FROM_HTML_MODE_LEGACY)
        }else{
            @Suppress("DEPRECATION")
            loginMessage.text = Html.fromHtml(getString(R.string.home_login_message_str))
        }

        login_btn.setOnClickListener { requestAuthentication() }
        logout_btn.setOnClickListener { logout() }

        showLoggedOutState()
    }

    override fun onResume() {
        super.onResume()

        showLoggedOutState()
    }

    private fun hideCurrentServer() {
        infoMessageView?.visibility = View.GONE
        home_login?.visibility = View.GONE
        infoMessageView?.text = ""
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data != null) {


            val auth = AuthUtils.activityResultAsAuthentication(requestCode, resultCode, data)
            if (auth != null) {
                syncWithAv(auth)
            }
            MainActivity.instance.onAuthentication(auth!!)
            MainActivity.instance.readAuthenticationFromPreferences()
            MainActivity.instance.loadMenu(true)
        }
    }
    
    private fun syncWithAv(auth: Authentication?) {
        hideErrorMessage()

        val avPhonePrefs = PreferenceUtils.getAvPhonePrefs(activity)

        // Without task factory, try later
        if (taskFactory == null || authManager == null) {
            authForSync = auth
            retrySync = true
            return
        }

        val displayer = this
        val syncAvTask = taskFactory!!.syncAvTask(avPhonePrefs.serverHost!!, auth!!.accessToken!!)

        syncAvTask.addProgressListener { result ->
            if (result.isError) {
                MainActivity.instance.logout()
                syncAvTask.showResult(result, displayer, activity)
            } else {
                authManager!!.onAuthentication(auth)
                showLoggedInState()
                user = result.user
                syncListener!!.invoke(result)
            }
        }

        val params = SyncWithAvParams()

        params.deviceId = DeviceInfo.getUniqueId(activity)
        params.imei = DeviceInfo.getIMEI(activity)
        params.deviceName = DeviceInfo.deviceName
        params.iccid = DeviceInfo.getICCID(activity)
        params.mqttPassword = avPhonePrefs.password
        params.customData = PreferenceUtils.getCustomDataLabels(activity)
        //     params.current = ((MainActivity)getActivity()).current;
        params.activity = activity as MainActivity

        syncAvTask.execute(params)
    }

    private fun showLoggedInState() {

    }

    private fun showLoggedOutState() {
        MainActivity.instance.runOnUiThread {
            hideLogoutButton()
            hideCurrentServer()
        }
    }

    private fun logout() {
        val avPhonePrefs = PreferenceUtils.getAvPhonePrefs(activity)

        if (authManager == null || authManager!!.authentication == null || taskFactory == null) {
            return
        }

        val accessToken = authManager!!.authentication!!.accessToken

        val logoutTask = taskFactory!!.logoutTask(avPhonePrefs.serverHost!!, accessToken!!)

        logoutTask.execute()
        try {
            logoutTask.get()
        } catch (e: Exception) {
            Crashlytics.log(Log.WARN, TAG, "Exception while logging out")
            Crashlytics.logException(e)
        } finally {
            authManager!!.forgetAuthentication()

            showLoggedOutState()
        }
    }

    private fun hideLogoutButton() {
        if (logout_btn != null) {
            logout_btn.visibility = View.GONE
        }
        if (login_btn != null) {
            login_btn.visibility = View.VISIBLE
        }

        if (home_login_message != null) {
            home_login_message.visibility = View.VISIBLE
        }
    }

    companion object {
        private val TAG = HomeFragment::class.simpleName
    }
}
