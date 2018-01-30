package com.sierrawireless.avphone.task;


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
