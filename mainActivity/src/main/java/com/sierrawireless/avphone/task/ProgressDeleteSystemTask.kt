@file:Suppress("DEPRECATION")

package com.sierrawireless.avphone.task

import android.app.ProgressDialog
import android.content.Context
import android.os.Build
import android.support.v4.content.ContextCompat
import android.text.Html
import android.view.View
import android.widget.TextView
import com.sierrawireless.avphone.R
import com.sierrawireless.avphone.activity.MainActivity

class ProgressDeleteSystemTask internal constructor(systemClient: ISystemClient, userClient: IUserClient, alertRuleClient: IAlertRuleClient, context: Context) : DeleteSystemTask(systemClient, userClient, alertRuleClient, context) {


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


    public override fun onProgressUpdate(vararg progress: DeleteSystemProgress) {
        dialog!!.progress = progress[0].value
        val stepMessage = context.getString(progress[0].stringId)
        val htmlMessage = context.getString(R.string.progress_syncing_message, stepMessage)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            dialog!!.setMessage(Html.fromHtml(htmlMessage, Html.FROM_HTML_MODE_LEGACY))
        }else{
            @Suppress("DEPRECATION")
            dialog!!.setMessage(Html.fromHtml(htmlMessage))
        }

    }

    override fun onPostExecute(result: DeleteSystemResult) {
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
