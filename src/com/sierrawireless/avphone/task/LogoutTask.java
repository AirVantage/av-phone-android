package com.sierrawireless.avphone.task;

import java.io.IOException;

import net.airvantage.model.AirVantageException;
import net.airvantage.model.AvError;
import net.airvantage.utils.IAirVantageClient;
import android.os.AsyncTask;

public class LogoutTask extends AsyncTask<String, Integer, AvError> {

    private IAirVantageClient client;

    public LogoutTask(IAirVantageClient client) {
        this.client = client;
    }
    
    @Override
    protected AvError doInBackground(String... arg0) {
        AvError res = null;
        try {
            this.client.logout();
        } catch (IOException e) {
            res = new AvError("unexpected.error");
        } catch (AirVantageException e) {
            res = e.getError();
        }
        return res;
    }

}
