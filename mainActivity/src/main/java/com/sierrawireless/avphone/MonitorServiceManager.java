package com.sierrawireless.avphone;

import com.sierrawireless.avphone.service.MonitoringService;


public interface MonitorServiceManager {
    boolean isServiceRunning();
    boolean isServiceStarted(String name);
    boolean oneServiceStarted();
    void stopMonitoringService();
    void startMonitoringService(String name);
    void startSendData();
    void stopSendData();
    void sendAlarmEvent();
    void setMonitoringServiceListener(MonitorServiceListener listener);
    MonitoringService getMonitoringService();
}
