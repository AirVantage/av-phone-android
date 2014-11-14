package com.sierrawireless.avphone.task;

import net.airvantage.model.AvError;
import android.app.ProgressDialog;
import android.content.Context;
import android.text.Html;

import com.sierrawireless.avphone.R;

public class ProgressSyncWithAvTask extends SyncWithAvTask {

    private Context context;
    private ProgressDialog dialog;

    public ProgressSyncWithAvTask(IApplicationClient applicationClient, ISystemClient systemClient,
            IAlertRuleClient alertRuleClient, Context context) {
        super(applicationClient, systemClient, alertRuleClient);
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        int maxProgress = SyncProgress.values().length;
        dialog = new ProgressDialog(this.context);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setIndeterminate(false);
        dialog.setMax(maxProgress);
        dialog.setCancelable(false);
        dialog.setTitle(R.string.progress_syncing);
        dialog.setMessage(context.getString(R.string.progress_starting));
        dialog.show();
    }

    @Override
    public void onProgressUpdate(SyncProgress ...progress) {
        dialog.setProgress(progress[0].value);
        String stepMessage = context.getString(progress[0].stringId);
        String htmlMessage = context.getString(R.string.progress_syncing_message, stepMessage);
        dialog.setMessage(Html.fromHtml(htmlMessage));
    }

    @Override
    protected void onPostExecute(AvError result) {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
        super.onPostExecute(result);
    }

}
