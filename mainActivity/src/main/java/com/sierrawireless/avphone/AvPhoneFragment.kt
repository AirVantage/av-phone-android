package com.sierrawireless.avphone

import android.annotation.TargetApi
import android.app.Activity
import androidx.fragment.app.Fragment
import android.content.Context
import android.content.Intent
import android.text.Spanned
import android.util.Log
import android.view.View
import android.widget.TextView
import com.sierrawireless.avphone.activity.AuthorizationActivity
import com.sierrawireless.avphone.auth.AuthenticationManager
import com.sierrawireless.avphone.message.IMessageDisplayer
import com.sierrawireless.avphone.task.SyncWithAvListener
import net.airvantage.utils.PreferenceUtils
import org.jetbrains.anko.toast

abstract class AvPhoneFragment : Fragment(), IMessageDisplayer {

    var authManager: AuthenticationManager? = null

    var syncListener: SyncWithAvListener? = null

    abstract var errorMessageView: TextView?

    @TargetApi(23)
    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is AuthenticationManager) {
            authManager = context
        }

    }

    override fun onResume() {
        super.onResume()
        Log.d("AVPhoneFragment", "**********************************RESUME********************")
        checkCredential()
    }


    override fun showError(id: Int, vararg params: Any) {
        this.showErrorMessage(id, *params)
    }

    override fun showSuccess(id: Int, vararg params: Any) {
        this.hideErrorMessage()
        this.lToast(id)
    }

    override fun showSuccess(name: String, vararg params: Any) {
        this.hideErrorMessage()
        this.lToast(name)
    }

    private fun showErrorMessage(id: Int, vararg params: Any) {
        showErrorMessage(activity?.getString(id, *params)!!)
    }

    private fun showErrorMessage(message: String) {
        val errorMessageView = errorMessageView
        errorMessageView?.text = message
        errorMessageView?.visibility = View.VISIBLE
    }

    override fun showErrorMessage(spanned: Spanned) {
        val errorMessageView = errorMessageView
        errorMessageView?.text = spanned
        errorMessageView?.visibility = View.VISIBLE
    }

    fun hideErrorMessage() {
        errorMessageView?.visibility = View.GONE
    }

    private fun lToast(id: Int) {
        context?.toast(activity?.getString(id)!!)
    }

    private fun lToast(name: String) {
        context?.toast(name)

    }

    protected fun requestAuthentication() {

        val intent = Intent(this.activity, AuthorizationActivity::class.java)
        this.startActivityForResult(intent, AuthorizationActivity.REQUEST_AUTHORIZATION)
    }

    fun checkCredentials(): Boolean {

        val prefs = PreferenceUtils.getAvPhonePrefs(activity as Activity)

        if (!prefs.checkCredentials()) {
            PreferenceUtils.showMissingPrefsDialog(activity as Activity)
            return false
        }

        return true

    }

    private fun checkCredential() {
        // first check credential
        if (checkCredentials()) {
            val auth = authManager!!.authentication

            if (auth != null && auth.isExpired) {
                requestAuthentication()
            }
        }
    }



}