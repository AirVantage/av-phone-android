package com.sierrawireless.avphone.task;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;

import com.sierrawireless.avphone.R;
import com.sierrawireless.avphone.message.IMessageDisplayer;


import net.airvantage.model.AirVantageException;
import net.airvantage.model.AvError;
import net.airvantage.model.User;

import java.util.ArrayList;
import java.util.List;

public class GetUserTask extends AvPhoneTask<GetUserParams, GetUserProgress, GetUserResult> {


    private List<GetUserListener> syncListeners = new ArrayList<>();

    private IUserClient userClient;

    @SuppressLint("StaticFieldLeak")
    private Context context;


    GetUserTask(IUserClient userClient, Context context) {

        this.userClient = userClient;
        this.context = context;
    }

    public void addProgressListener(GetUserListener listener) {
        this.syncListeners.add(listener);
    }

    @SuppressLint("DefaultLocale")
    protected GetUserResult doInBackground(GetUserParams... params) {

        try {

            publishProgress(GetUserProgress.CHECKING_RIGHTS);

            final List<String> missingRights = userClient.checkRights();
            if (!missingRights.isEmpty()) {
                return new GetUserResult(new AvError(AvError.MISSING_RIGHTS, missingRights));
            }

            publishProgress(GetUserProgress.GET_USER);
            final User user = userClient.getUser();

            publishProgress(GetUserProgress.DONE);
            return new GetUserResult(user);
        }
        catch (AirVantageException e) {
            publishProgress(GetUserProgress.DONE);
            return new GetUserResult(e.getError());
        }
    }

    @Override
    protected void onPostExecute(GetUserResult result) {
        super.onPostExecute(result);
        for (GetUserListener listener : syncListeners) {
            listener.onGetting(result);
        }
    }


    protected Context getContext() {
        return this.context;
    }

    public void showResult(GetUserResult result, IMessageDisplayer displayer, Activity context) {

        if (result.isError()) {
            AvError error = result.getError();
            displayTaskError(error, displayer, context, userClient);

        } else {
            displayer.showSuccess(R.string.sync_success);
        }
    }
}
