package com.sierrawireless.avphone.task;

import android.app.ProgressDialog;
import android.content.Context;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.sierrawireless.avphone.R;

public class ProgressGetUserTask extends GetUserTask {

    private ProgressDialog dialog;

    ProgressGetUserTask( IUserClient userClient, Context context) {
        super(userClient, context);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        int maxProgress = SyncProgress.values().length;
        dialog = new ProgressDialog(getContext());
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setIndeterminate(false);
        dialog.setMax(maxProgress);
        dialog.setCancelable(false);
        dialog.setTitle(R.string.progress_syncing);

        dialog.setMessage(getContext().getString(R.string.progress_starting));
        dialog.setProgressDrawable(getContext().getResources().getDrawable(
                R.drawable.apptheme_progress_horizontal_holo_light, getContext().getTheme()));

        dialog.setIndeterminateDrawable(getContext().getResources().getDrawable(
                R.drawable.apptheme_progress_indeterminate_horizontal_holo_light, getContext().getTheme()));

        dialog.show();

        // Color has to be set *after* the dialog is shown (see
        // http://blog.supenta.com/2014/07/02/how-to-style-alertdialogs-like-a-pro/)
        int titleDividerId = getContext().getResources().getIdentifier("titleDivider", "id", "android");
        View titleDivider = dialog.findViewById(titleDividerId);
        if (titleDivider != null)
            titleDivider.setBackgroundColor(getContext().getResources().getColor(R.color.sierrared));

        // See http://stackoverflow.com/questions/15271500/how-to-change-alert-dialog-header-divider-color-android
        int alertTitleId = getContext().getResources().getIdentifier("alertTitle", "id", "android");
        Window windows = dialog.getWindow();
        if (windows != null) {
            TextView alertTitle = (TextView) windows.getDecorView().findViewById(alertTitleId);
            alertTitle.setTextColor(getContext().getResources().getColor(R.color.sierrared)); // change title text color
        }
    }

    @Override
    public void onProgressUpdate(GetUserProgress... progress) {
        dialog.setProgress(progress[0].value);
        String stepMessage = getContext().getString(progress[0].stringId);
        String htmlMessage = getContext().getString(R.string.progress_syncing_message, stepMessage);
        dialog.setMessage(Html.fromHtml(htmlMessage));
    }

    @Override
    protected void onPostExecute(GetUserResult result) {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
        super.onPostExecute(result);
    }

}
