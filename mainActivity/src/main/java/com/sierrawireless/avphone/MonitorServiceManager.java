package com.sierrawireless.avphone;

import com.sierrawireless.avphone.service.MonitoringService;


public interface MonitorServiceManager {
    boolean isServiceRunning();
    boolean isServiceStarted();
    void stopMonitoringService();
    void startMonitoringService();
    void startSendData();
    void stopSendData();
    void sendAlarmEvent(boolean activated);
    void setMonitoringServiceListener(MonitorServiceListener listener);
    MonitoringService getMonitoringService();
}
