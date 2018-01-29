package com.sierrawireless.avphone.service;

import android.content.Intent;

public class LogMessage extends Intent {

    public static final String LOG_EVENT = "com.sierrawireless.avphone.event.log";

    // keys used for broadcasting log events
    private static final String LOG = "log";
    private static final String ALARM = "alarm";

    public LogMessage(String message, boolean alarm) {
        super(LOG_EVENT);

        this.putExtra(LOG, message);
        this.putExtra(ALARM, alarm);
    }

    public String getMessage() {
        return this.getStringExtra(LOG);
    }
    public boolean getAlarm() { return this.getBooleanExtra(ALARM, false);}

}
