package com.sierrawireless.avphone.task

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.support.v4.content.ContextCompat
import android.text.Html
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.sierrawireless.avphone.activity.MainActivity


class ProgressUpdateTask internal constructor(applicationClient: IApplicationClient, systemClient: ISystemClient,
                                                  alertRuleClient: IAlertRuleClient, userClient: IUserClient, context: Context) : UpdateTask(applicationClient, systemClient, alertRuleClient, userClient, context) {

    @SuppressLint("StaticFieldLeak")

    private var dialog: AlertDialog? = null
    @SuppressLint("StaticFieldLeak")
    private var progressBar: ProgressBar? = null
    @SuppressLint("StaticFieldLeak")
    private var tvText: TextView? = null


    @Suppress("SameParameterValue")
    private fun setProgressDialog(context:Context, message:String, max: Int): AlertDialog {
        val llPadding = 90
        val ll = LinearLayout(context)

        val horizontalLayout = LinearLayout(context)
        val horizontalParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        horizontalLayout.orientation = LinearLayout.VERTICAL
        horizontalLayout.layoutParams = horizontalParams

        ll.orientation = LinearLayout.HORIZONTAL
        ll.setPadding(llPadding, llPadding, llPadding, llPadding)
        ll.gravity = Gravity.CENTER
        var llParam = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT)
        llParam.gravity = Gravity.CENTER
        ll.layoutParams = llParam

        progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal)
        progressBar!!.isIndeterminate = false
        progressBar!!.max = max
        progressBar!!.setPadding(0, 0, 0, 0)
        progressBar!!.layoutParams = llParam
        progressBar!!.progressDrawable = context.resources.getDrawable(
                com.sierrawireless.avphone.R.drawable.apptheme_progress_horizontal_holo_light, context.theme)
        progressBar!!.indeterminateDrawable = context.resources.getDrawable(
                com.sierrawireless.avphone.R.drawable.apptheme_progress_indeterminate_horizontal_holo_light, context.theme)

        llParam = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
        llParam.gravity = Gravity.CENTER
        llParam.height = 400
        tvText = TextView(context)
        //tvText!!.setTextColor(ContextCompat.getColor(context, com.sierrawireless.avphone.R.color.sierrared))
        tvText!!.text = message
       // tvText!!.setTextColor(Color.parseColor("#000000"))
        tvText!!.textSize = 20.toFloat()
        tvText!!.layoutParams = llParam


        horizontalLayout.addView(tvText)

        horizontalLayout.addView(progressBar)
        ll.addView(horizontalLayout)

        val builder = AlertDialog.Builder(context)
        builder.setCancelable(true)
        builder.setView(ll)

        val dialog = builder.create()
        val window = dialog.window
        if (window != null) {
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(dialog.window?.attributes)
            layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT
            layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            dialog.window?.attributes = layoutParams
        }
        return dialog
    }


    override fun onPreExecute() {

        Log.d("*******************", "Creareupdatetask")
        super.onPreExecute()

        val maxProgress = UpdateProgress.values().size
        dialog = setProgressDialog(context, "We are synchronizing your phone with Airvantage. Please Wait.", maxProgress)
        dialog!!.show()

//        dialog2 = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal)
//        dialog2!!.isIndeterminate = false
//        dialog2!!.max = maxProgress
//        dialog2!!.progressDrawable = context.resources.getDrawable(
//                com.sierrawireless.avphone.R.drawable.apptheme_progress_horizontal_holo_light, context.theme)
//        dialog2!!.indeterminateDrawable = context.resources.getDrawable(
//                com.sierrawireless.avphone.R.drawable.apptheme_progress_indeterminate_horizontal_holo_light, context.theme)
//
//        dialog2!!.visibility = View.VISIBLE
//        val params = RelativeLayout.LayoutParams(100, 100)
//        params.addRule(RelativeLayout.CENTER_IN_PARENT)
//        layout.addView(dialog2!!, params)



        // Color has to be set *after* the dialog is shown (see
        // http://blog.supenta.com/2014/07/02/how-to-style-alertdialogs-like-a-pro/)
        val titleDividerId = context.resources.getIdentifier("titleDivider", "id", "android")
        val titleDivider = dialog!!.findViewById<View>(titleDividerId)
        titleDivider?.setBackgroundColor(ContextCompat.getColor(context, com.sierrawireless.avphone.R.color.sierrared))

        // See http://stackoverflow.com/questions/15271500/how-to-change-alert-dialog-header-divider-color-android
        val alertTitleId = context.resources.getIdentifier("alertTitle", "id", "android")
        val windows = dialog!!.window
        if (windows != null) {
            val alertTitle = windows.decorView.findViewById<TextView>(alertTitleId)
            alertTitle.setTextColor(ContextCompat.getColor(context, com.sierrawireless.avphone.R.color.sierrared)) // change title text color
        }
    }

    public override fun onProgressUpdate(vararg progress: UpdateProgress) {


        progressBar!!.progress = progress[0].value
        val stepMessage = context.getString(progress[0].stringId)
        val htmlMessage = context.getString(com.sierrawireless.avphone.R.string.progress_syncing_message, stepMessage)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tvText!!.text = Html.fromHtml(htmlMessage, Html.FROM_HTML_MODE_LEGACY)
        }else{
            @Suppress("DEPRECATION")
            tvText!!.text = Html.fromHtml(htmlMessage)
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