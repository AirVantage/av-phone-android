package com.sierrawireless.avphone

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
import com.sierrawireless.avphone.auth.AuthUtils
import com.sierrawireless.avphone.auth.Authentication
import com.sierrawireless.avphone.message.IMessageDisplayer
import com.sierrawireless.avphone.task.GetUserParams
import com.sierrawireless.avphone.task.IAsyncTaskFactory
import com.sierrawireless.avphone.task.SyncWithAvListener
import com.sierrawireless.avphone.task.SyncWithAvParams
import kotlinx.android.synthetic.main.fragment_home.*
import net.airvantage.model.User
import net.airvantage.utils.PreferenceUtils

class HomeFragment : AvPhoneFragment(), IMessageDisplayer {

    private var lView: View? = null

    private var authForSync: Authentication? = null
    private var retrySync: Boolean = false



    private var taskFactory: IAsyncTaskFactory? = null

    private var user: User? = null

    private val infoMessageView: TextView
        get() = home_info_message

    override var errorMessageView: TextView
        get() = home_error_message
        set(textView) {

        }

    init {
        retrySync = false
        taskFactory = null
    }

    fun setTaskFactory(taskFactory: IAsyncTaskFactory) {
        this.taskFactory = taskFactory
        if (retrySync) {
            retrySync = false
            syncWithAv(authForSync)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        @Suppress("UNCHECKED_CAST")
        syncListener = context as SyncWithAvListener
    }

    @SuppressWarnings("deprecation")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        this.lView = inflater.inflate(R.layout.fragment_home, container, false)


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

        if (authManager!!.isLogged) {
            showLoggedInState()
        } else {
            showLoggedOutState()
        }


    }

    override fun onResume() {
        super.onResume()

        if (authManager!!.isLogged) {
            showLoggedInState()
        } else {
            showLoggedOutState()
        }
    }

    private fun showCurrentServer() {
        val phonePrefs = PreferenceUtils.getAvPhonePrefs(activity)



        val message: String
        message = when {
            phonePrefs.usesNA() -> getString(R.string.logged_on_na)
            phonePrefs.usesEU() -> getString(R.string.logged_on_eu)
            else -> getString(R.string.logged_on_custom, phonePrefs.serverHost)
        }

        infoMessageView.text = message
        infoMessageView.visibility = View.VISIBLE
        if (user != null) {
            home_login.text = String.format("%s %s", getString(R.string.welcome), user!!.name)
            home_login.visibility = View.VISIBLE
        }
    }

    private fun hideCurrentServer() {

        infoMessageView.visibility = View.GONE
        home_login.visibility = View.GONE
        infoMessageView.text = ""
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        val auth = AuthUtils.activityResultAsAuthentication(requestCode, resultCode, data)
        if (auth != null) {
            syncWithAv(auth)
        }
        MainActivity.instance.onAuthentication(auth!!)
        MainActivity.instance.readAuthenticationFromPreferences()
        MainActivity.instance.loadMenu()
    }


    private fun syncGetUser(auth: Authentication) {
        hideErrorMessage()

        val avPhonePrefs = PreferenceUtils.getAvPhonePrefs(activity)

        // Without task factory, try later
        if (taskFactory == null) {
            authForSync = auth
            retrySync = true
            return
        }


        val displayer = this
        val getUserTask = taskFactory!!.getUserTak(avPhonePrefs.serverHost!!, auth.accessToken!!)

        getUserTask.addProgressListener { result ->
            if (result.isError) {
                authManager!!.forgetAuthentication()
                showLoggedOutState()
                getUserTask.showResult(result, displayer, activity)
            } else {
                authManager!!.onAuthentication(auth)
                showLoggedInState()
                user = result.user
                user!!.server = avPhonePrefs.serverHost
                MainActivity.instance.setUser(user!!)

            }
        }

        val params = GetUserParams()

        getUserTask.execute(params)

    }

    private fun syncWithAv(auth: Authentication?) {

        hideErrorMessage()

        val avPhonePrefs = PreferenceUtils.getAvPhonePrefs(activity)

        // Without task factory, try later
        if (taskFactory == null) {
            authForSync = auth
            retrySync = true
            return
        }

        val displayer = this
        val syncAvTask = taskFactory!!.syncAvTask(avPhonePrefs.serverHost!!, auth!!.accessToken!!)

        syncAvTask.addProgressListener { result ->
            if (result.isError) {
                authManager!!.forgetAuthentication()
                showLoggedOutState()
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
        val avPhonePrefs = PreferenceUtils.getAvPhonePrefs(activity)
        if (user == null) {
            syncGetUser(authManager!!.authentication!!)
        } else {
            user!!.server = avPhonePrefs.serverHost
            MainActivity.instance.setUser(user!!)
        }
        showCurrentServer()
        showLogoutButton()


    }

    private fun showLoggedOutState() {
        hideLogoutButton()
        hideCurrentServer()
    }

    private fun logout() {

        val avPhonePrefs = PreferenceUtils.getAvPhonePrefs(activity)

        val accessToken = authManager!!.authentication!!.accessToken

        val logoutTask = taskFactory!!.logoutTask(avPhonePrefs.serverHost!!, accessToken!!)

        logoutTask.execute()

        try {
            logoutTask.get()
        } catch (e: Exception) {
            Log.w(LOGTAG, "Exception while logging out")
            Crashlytics.logException(e)
        } finally {
            authManager!!.forgetAuthentication()

            showLoggedOutState()
        }

    }

    private fun showLogoutButton() {
        logout_btn.visibility = View.VISIBLE
        login_btn.visibility = View.GONE
        home_login_message.visibility = View.GONE
    }

    private fun hideLogoutButton() {
        logout_btn.visibility = View.GONE
        login_btn.visibility = View.VISIBLE

        home_login_message.visibility = View.VISIBLE
    }

    companion object {

        private val LOGTAG = HomeFragment::class.java.name
    }
}
