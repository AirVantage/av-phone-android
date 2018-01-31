package com.sierrawireless.avphone

import com.sierrawireless.avphone.service.MonitoringService

interface MonitorServiceListener {

    fun onServiceStarted(service: MonitoringService)

    fun onServiceStopped(service: MonitoringService)
}
