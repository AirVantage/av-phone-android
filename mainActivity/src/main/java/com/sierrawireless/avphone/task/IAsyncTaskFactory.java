package com.sierrawireless.avphone.task;

import net.airvantage.model.AvError;
import android.os.AsyncTask;

public interface IAsyncTaskFactory {
    SyncWithAvTask syncAvTask(String serverHost, String token);
    GetUserTask getUserTak(String serverHost, String token);
    DeleteSystemTask deleteSystemTak(String serverHost, String token);

    AsyncTask<String, Integer, AvError> logoutTask(String serverHost, String token);
}
