package com.sierrawireless.avphone

import com.sierrawireless.avphone.service.MonitoringService


interface MonitorServiceManager {
    //val isServiceRunning: Boolean
    var monitoringService: MonitoringService?
    fun isServiceStarted(name: String): Boolean
    fun oneServiceStarted(): Boolean
    fun stopMonitoringService()
    fun startMonitoringService(name: String)
    fun startSendData()
    fun stopSendData()
    fun sendAlarmEvent()
    fun setMonitoringServiceListener(listener: MonitorServiceListener)
    fun isServiceRunning(): Boolean
}