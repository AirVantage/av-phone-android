
package com.sierrawireless.avphone.task

import android.content.Context

import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.sierrawireless.avphone.R
import com.sierrawireless.avphone.activity.MainActivity

class ProgressGetUserTask internal constructor(userClient: IUserClient, context: Context) : GetUserTask(userClient, context) {

    private var dialog: AlertDialog? = null
    private var topProgressBar: ProgressBar? = null
    private var textProgressBar:TextView? = null;

    override fun onPreExecute() {
        super.onPreExecute()

        val maxProgress = SyncProgress.values().size

        val builder: AlertDialog.Builder = AlertDialog.Builder(MainActivity.instance, R.style.Theme_AppCompat_DayNight_Dialog_Alert)
        builder.setCancelable(false) // if you want user to wait for some process to finish,
        builder.setView(com.sierrawireless.avphone.R.layout.layout_loading_dialog)
        dialog = builder.create()


        dialog!!.show()


        // Color has to be set *after* the dialog is shown (see
        // http://blog.supenta.com/2014/07/02/how-to-style-alertdialogs-like-a-pro/)
        val titleDividerId = context.resources.getIdentifier("titleDivider", "id", "android")
        val titleDivider = dialog!!.findViewById<View>(titleDividerId)
        titleDivider?.setBackgroundColor(ContextCompat.getColor(context, R.color.sierrared))

        // See http://stackoverflow.com/questions/15271500/how-to-change-alert-dialog-header-divider-color-android
        val windows = dialog!!.window
        if (windows != null) {
            topProgressBar = windows.decorView.findViewById<ProgressBar> ( R.id.top_progressBar )
            textProgressBar  = windows.decorView.findViewById<TextView> ( R.id.loading_msg )
            topProgressBar!!.isIndeterminate = false
            topProgressBar!!.max = maxProgress
            dialog!!.setCancelable(false)
            dialog!!.setTitle(R.string.progress_syncing)

            textProgressBar!!.text = context.getString(R.string.progress_starting)

            val alertTitle = windows.decorView.findViewById<TextView>(R.id.alert_title)
            alertTitle.text = context.getString(R.string.progress_syncing)
            alertTitle.setTextColor(ContextCompat.getColor(context, R.color.sierrared)) // change title text color
        }
    }


    override fun onPostExecute(result: GetUserResult) {
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
