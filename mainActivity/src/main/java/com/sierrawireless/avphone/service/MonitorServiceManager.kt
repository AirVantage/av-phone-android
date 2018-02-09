package com.sierrawireless.avphone.service

import com.sierrawireless.avphone.listener.MonitorServiceListener
import com.sierrawireless.avphone.service.MonitoringService


interface MonitorServiceManager {
    //val isServiceRunning: Boolean
    var monitoringService: MonitoringService?
    fun isServiceStarted(name: String): Boolean
    fun oneServiceStarted(): Boolean
    fun stopMonitoringService()
    fun startMonitoringService(name: String)
    fun startSendData(name: String):Boolean
    fun stopSendData()
    fun sendAlarmEvent(on: Boolean):Boolean
    fun setMonitoringServiceListener(listener: MonitorServiceListener)
    fun isServiceRunning(name: String? = null): Boolean
    fun cancel()
    fun start()
}
