@file:Suppress("DEPRECATION")

package com.sierrawireless.avphone.task

import android.app.ProgressDialog
import android.content.Context
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.TextView
import com.sierrawireless.avphone.R

class ProgressGetUserTask internal constructor(userClient: IUserClient, context: Context) : GetUserTask(userClient, context) {

    private var dialog: ProgressDialog? = null

    override fun onPreExecute() {
        super.onPreExecute()

        val maxProgress = SyncProgress.values().size
        dialog = ProgressDialog(context)
        dialog!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        dialog!!.isIndeterminate = false
        dialog!!.max = maxProgress
        dialog!!.setCancelable(false)
        dialog!!.setTitle(R.string.progress_syncing)

        dialog!!.setMessage(context.getString(R.string.progress_starting))
        dialog!!.setProgressDrawable(context.resources.getDrawable(
                R.drawable.apptheme_progress_horizontal_holo_light, context.theme))

        dialog!!.setIndeterminateDrawable(context.resources.getDrawable(
                R.drawable.apptheme_progress_indeterminate_horizontal_holo_light, context.theme))

        dialog!!.show()

        // Color has to be set *after* the dialog is shown (see
        // http://blog.supenta.com/2014/07/02/how-to-style-alertdialogs-like-a-pro/)
        val titleDividerId = context.resources.getIdentifier("titleDivider", "id", "android")
        val titleDivider = dialog!!.findViewById<View>(titleDividerId)
        titleDivider?.setBackgroundColor(ContextCompat.getColor(context, R.color.sierrared))

        // See http://stackoverflow.com/questions/15271500/how-to-change-alert-dialog-header-divider-color-android
        val alertTitleId = context.resources.getIdentifier("alertTitle", "id", "android")
        val windows = dialog!!.window
        if (windows != null) {
            val alertTitle = windows.decorView.findViewById<TextView>(alertTitleId)
            alertTitle.setTextColor(ContextCompat.getColor(context, R.color.sierrared)) // change title text color
        }
    }


    override fun onPostExecute(result: GetUserResult) {
        if (dialog!!.isShowing) {
            dialog!!.dismiss()
        }
        super.onPostExecute(result)
    }

}
