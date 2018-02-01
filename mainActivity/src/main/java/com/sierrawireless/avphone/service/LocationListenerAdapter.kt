package com.sierrawireless.avphone.service

import android.location.Location
import android.location.LocationListener
import android.os.Bundle

open class LocationListenerAdapter : LocationListener {

    override fun onLocationChanged(location: Location) {}

    override fun onProviderDisabled(provider: String) {}

    override fun onProviderEnabled(provider: String) {}

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}

}
