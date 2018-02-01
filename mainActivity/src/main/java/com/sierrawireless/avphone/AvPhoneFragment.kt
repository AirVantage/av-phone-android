package com.sierrawireless.avphone

import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.text.Spanned
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.sierrawireless.avphone.auth.AuthenticationManager
import com.sierrawireless.avphone.message.IMessageDisplayer
import com.sierrawireless.avphone.task.SyncWithAvListener

abstract class AvPhoneFragment : Fragment(), IMessageDisplayer {

    var authManager: AuthenticationManager? = null

    var syncListener: SyncWithAvListener? = null

    abstract var errorMessageView: TextView


    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is AuthenticationManager) {
            authManager = context
        }

    }


    override fun showError(id: Int, vararg params: Any) {
        this.showErrorMessage(id, *params)
    }

    override fun showSuccess(id: Int, vararg params: Any) {
        this.hideErrorMessage()
        this.toast(id)
    }

    private fun showErrorMessage(id: Int, vararg params: Any) {
        showErrorMessage(activity.getString(id, *params))
    }

    private fun showErrorMessage(message: String) {
        val errorMessageView = errorMessageView
        errorMessageView.text = message
        errorMessageView.visibility = View.VISIBLE
    }

    override fun showErrorMessage(spanned: Spanned) {
        val errorMessageView = errorMessageView
        errorMessageView.text = spanned
        errorMessageView.visibility = View.VISIBLE
    }

    fun hideErrorMessage() {
        errorMessageView.visibility = View.GONE
    }

    private fun toast(id: Int) {
        toast(activity.getString(id))
    }

    private fun toast(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }

    protected fun requestAuthentication() {

        val intent = Intent(this.activity, AuthorizationActivity::class.java)
        this.startActivityForResult(intent, AuthorizationActivity.REQUEST_AUTHORIZATION)
    }

}