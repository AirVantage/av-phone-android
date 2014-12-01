package com.sierrawireless.avphone;

import com.sierrawireless.avphone.service.MonitoringService;


public interface MonitorServiceManager {
    public boolean isServiceRunning();
    public void stopMonitoringService();
    public void startMonitoringService();
    public void sendAlarmEvent(boolean activated);
    public void setMonitoringServiceListener(MonitorServiceListener listener);
    public MonitoringService getMonitoringService();
}
