package com.sierrawireless.avphone.task;

import net.airvantage.model.AvError;
import net.airvantage.model.User;

/**
 * Created by JDamiano on 29/01/2018.
 */

public class SendDataResult {
    private boolean error;
    private String lastLog;


    SendDataResult(String lastLog, boolean error) {
        this.error = error;
        this.lastLog = lastLog;
    }

    SendDataResult(String lastLog) {
        this.lastLog = lastLog;
        this.error = false;
    }

    public boolean isError() {
        return error;
    }


    public String getLastLog() {
        return lastLog;
    }


}
