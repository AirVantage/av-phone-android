package com.sierrawireless.avphone;

import com.sierrawireless.avphone.service.MonitoringService;

public interface MonitorServiceListener {

    void onServiceStarted(MonitoringService service);

    void onServiceStopped(MonitoringService service);
}
