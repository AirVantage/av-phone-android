package com.sierrawireless.avphone.task;

import android.content.Context;
import android.os.AsyncTask;

import net.airvantage.model.AvError;
import net.airvantage.utils.AirVantageClient;

public class AsyncTaskFactory implements IAsyncTaskFactory {

    private Context context;

    public AsyncTaskFactory(Context context) {
        this.context = context;
    }
    
    public SyncWithAvTask syncAvTask(String serverHost, String token) {
        
        AirVantageClient avClient = new AirVantageClient(serverHost, token);

        IApplicationClient appClient = new ApplicationClient(avClient);
        ISystemClient systemClient = new SystemClient(avClient);
        IAlertRuleClient alertRuleClient = new AlertRuleClient(avClient);
        IUserClient userClient = new UserClient(avClient);

        return new ProgressSyncWithAvTask(appClient, systemClient, alertRuleClient, userClient, context);
    }

    public GetUserTask getUserTak(String serverHost, String token) {

        AirVantageClient avClient = new AirVantageClient(serverHost, token);

        IUserClient userClient = new UserClient(avClient);

        return new ProgressGetUserTask(userClient, context);
    }

    public DeleteSystemTask deleteSystemTak(String serverHost, String token) {

        AirVantageClient avClient = new AirVantageClient(serverHost, token);
        ISystemClient systemClient = new SystemClient(avClient);

        IUserClient userClient = new UserClient(avClient);

        return new ProgressDeleteSystemTask(systemClient, userClient, context);
    }


    public AsyncTask<String, Integer, AvError> logoutTask(String serverHost, String token) {
        
        AirVantageClient client = new AirVantageClient(serverHost, token);

        return new LogoutTask(client);
    }
    
}
