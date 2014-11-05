package com.sierrawireless.avphone.task;

import net.airvantage.model.AvError;
import android.os.AsyncTask;

public interface IAsyncTaskFactory {
    public SyncWithAvTask syncAvTask(String serverHost, String token);
    public AsyncTask<String, Integer, AvError> logoutTask(String serverHost, String token);
}
