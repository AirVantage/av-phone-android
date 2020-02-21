package com.sierrawireless.avphone.task


import android.content.Context
import android.os.Build

import android.text.Html
import android.util.Log

import android.view.View

import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.sierrawireless.avphone.R

import com.sierrawireless.avphone.activity.MainActivity


class ProgressUpdateTask internal constructor(applicationClient: IApplicationClient, systemClient: ISystemClient,
                                                  alertRuleClient: IAlertRuleClient, userClient: IUserClient, context: Context) : UpdateTask(applicationClient, systemClient, alertRuleClient, userClient, context) {


    private var dialog: AlertDialog? = null
    private var topProgressBar: ProgressBar? = null
    private var textProgressBar:TextView? = null;

    override fun onPreExecute() {

        Log.d("*******************", "Creareupdatetask")
        super.onPreExecute()

        val maxProgress = UpdateProgress.values().size

        val builder: AlertDialog.Builder = AlertDialog.Builder(MainActivity.instance, R.style.Theme_AppCompat_DayNight_Dialog_Alert)
        builder.setCancelable(false) // if you want user to wait for some process to finish,
        builder.setView(com.sierrawireless.avphone.R.layout.layout_loading_dialog)
        dialog = builder.create()
        dialog!!.show()




        // Color has to be set *after* the dialog is shown (see
        // http://blog.supenta.com/2014/07/02/how-to-style-alertdialogs-like-a-pro/)
        val titleDividerId = context.resources.getIdentifier("titleDivider", "id", "android")
        val titleDivider = dialog!!.findViewById<View>(titleDividerId)
        titleDivider?.setBackgroundColor(ContextCompat.getColor(context, com.sierrawireless.avphone.R.color.sierrared))

        // See http://stackoverflow.com/questions/15271500/how-to-change-alert-dialog-header-divider-color-android
        val windows = dialog!!.window
        if (windows != null) {

            topProgressBar = windows.decorView.findViewById<ProgressBar> ( R.id.top_progressBar )
            textProgressBar  = windows.decorView.findViewById<TextView> ( R.id.loading_msg )
            topProgressBar!!.isIndeterminate = false
            topProgressBar!!.max = maxProgress
            dialog!!.setCancelable(false)
            //dialog!!.setTitle(R.string.progress_syncing)

            textProgressBar!!.text = context.getString(R.string.progress_starting)

            val alertTitle = windows.decorView.findViewById<TextView>(R.id.alert_title)
            alertTitle.text = context.getString(R.string.progress_syncing)
            alertTitle.setTextColor(ContextCompat.getColor(context, R.color.sierrared)) // change title text color

        }
    }

    public override fun onProgressUpdate(vararg progress: UpdateProgress) {

        topProgressBar!!.progress = progress[0].value

        val stepMessage = context.getString(progress[0].stringId)
        val htmlMessage = context.getString(com.sierrawireless.avphone.R.string.progress_syncing_message, stepMessage)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            dialog!!.setMessage(Html.fromHtml(htmlMessage, Html.FROM_HTML_MODE_LEGACY))
        }else{
            @Suppress("DEPRECATION")
            dialog!!.setMessage(Html.fromHtml(htmlMessage, Html.FROM_HTML_MODE_LEGACY))
        }
    }

    override fun onPostExecute(result: UpdateResult) {
        if (!MainActivity.instance.isFinishing && !MainActivity.instance.isDestroyed) {
            try {
                if (dialog != null && dialog!!.isShowing) {
                    dialog!!.dismiss()
                }
            } catch (e: IllegalArgumentException) {
            } catch (e: Exception) {
            } finally {
                dialog = null
            }
        }
        super.onPostExecute(result)
    }

}