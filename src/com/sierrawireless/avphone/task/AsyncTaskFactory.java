package com.sierrawireless.avphone.task;

import net.airvantage.model.AvError;
import net.airvantage.utils.AirVantageClient;
import android.content.Context;
import android.os.AsyncTask;

public class AsyncTaskFactory implements IAsyncTaskFactory {

    private Context context;

    public AsyncTaskFactory(Context context) {
        this.context = context;
    }
    
    public SyncWithAvTask syncAvTask(String serverHost, String token) {
        
        AirVantageClient client = new AirVantageClient(serverHost, token);

        IApplicationClient appClient = new ApplicationClient(client);
        ISystemClient systemClient = new SystemClient(client);
        IAlertRuleClient alertRuleClient = new AlertRuleClient(client);

        return new ProgressSyncWithAvTask(appClient, systemClient, alertRuleClient, context);
    }

    public AsyncTask<String, Integer, AvError> logoutTask(String serverHost, String token) {
        
        AirVantageClient client = new AirVantageClient(serverHost, token);

        return new LogoutTask(client);
    }
    
}
