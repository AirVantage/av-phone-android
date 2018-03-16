package com.sierrawireless.avphone.listener

import com.sierrawireless.avphone.service.MonitoringService

interface MonitorServiceListener {

    fun onServiceStarted(service: MonitoringService)

    fun onServiceStopped(service: MonitoringService)
}
