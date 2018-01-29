package com.sierrawireless.avphone.task;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.sierrawireless.avphone.DeviceInfo;
import com.sierrawireless.avphone.MainActivity;
import com.sierrawireless.avphone.ObjectsManager;
import com.sierrawireless.avphone.R;
import com.sierrawireless.avphone.message.IMessageDisplayer;

import net.airvantage.model.AirVantageException;
import net.airvantage.model.AvError;
import net.airvantage.model.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DeleteSystemTask extends AvPhoneTask<Void, DeleteSystemProgress, DeleteSystemResult> {


    private ISystemClient systemClient;


    private List<DeleteSystemListener> syncListeners = new ArrayList<>();

    private IUserClient userClient;

    @SuppressLint("StaticFieldLeak")
    private Context context;


    DeleteSystemTask( ISystemClient systemClient, IUserClient userClient, Context context) {

        this.systemClient = systemClient;
        this.userClient = userClient;
        this.context = context;
    }

    public void addProgressListener(DeleteSystemListener listener) {
        this.syncListeners.add(listener);
    }

    @SuppressLint("DefaultLocale")
    protected DeleteSystemResult doInBackground(Void... params) {

        try {

            publishProgress(DeleteSystemProgress.CHECKING_RIGHTS);

            final List<String> missingRights = userClient.checkRights();
            if (!missingRights.isEmpty()) {
                return new DeleteSystemResult(new AvError(AvError.MISSING_RIGHTS, missingRights));
            }

            String systemType;
            final User user = userClient.getUser();

            ObjectsManager objectsManager = ObjectsManager.getInstance();

            systemType = objectsManager.getSavedObjectName();

            // For emulator and iOs compatibility sake, using generated serial.
            final String serialNumber =  DeviceInfo.generateSerial(user.uid);

            // Save Device serial in context
            if (context instanceof MainActivity) {
                final MainActivity mainActivity = (MainActivity) context;
                mainActivity.setSystemSerial(serialNumber);
            }


            publishProgress(DeleteSystemProgress.CHECKING_SYSTEM);
            net.airvantage.model.AvSystem system = this.systemClient.getSystem(serialNumber, systemType);
            if (system != null) {
                publishProgress(DeleteSystemProgress.DELETING_SYSTEM);
                systemClient.deleteSystem(system);
            }
            publishProgress(DeleteSystemProgress.DONE);
            return new DeleteSystemResult(user);



        } catch (AirVantageException e) {
            publishProgress(DeleteSystemProgress.DONE);
            return new DeleteSystemResult(e.getError());
        } catch (IOException e) {
            Crashlytics.logException(e);
            Log.e(MainActivity.class.getName(), "Error when trying to synchronize with server", e);
            publishProgress(DeleteSystemProgress.DONE);
            return new DeleteSystemResult(new AvError("unkown.error"));
        }

    }

    protected void onPostExecute(DeleteSystemResult result) {
        super.onPostExecute(result);
        for (DeleteSystemListener listener : syncListeners) {
            listener.onDeleting(result);
        }
    }

    public void showResult(DeleteSystemResult result, IMessageDisplayer displayer, Activity context) {

        if (result.isError()) {
            AvError error = result.getError();
            displayTaskError(error, displayer, context, userClient);

        } else {
            displayer.showSuccess(R.string.sync_success);
        }
    }

    protected Context getContext() {
        return this.context;
    }

}
