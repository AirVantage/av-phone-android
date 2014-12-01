package com.sierrawireless.avphone;

import com.sierrawireless.avphone.service.MonitoringService;

public interface MonitorServiceListener {

    public void onServiceStarted(MonitoringService service);

    public void onServiceStopped(MonitoringService service);
}
