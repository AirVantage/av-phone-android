package com.sierrawireless.avphone.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.TrafficStats
import android.os.Binder
import android.os.IBinder
import android.support.v4.content.LocalBroadcastManager
import android.telephony.*
import android.util.Log
import android.widget.Toast
import com.crashlytics.android.Crashlytics
import com.google.gson.Gson
import com.sierrawireless.avphone.MainActivity
import com.sierrawireless.avphone.ObjectsManager
import com.sierrawireless.avphone.R
import com.sierrawireless.avphone.task.SendDataParams
import com.sierrawireless.avphone.task.SendDataTask
import com.sierrawireless.avphone.tools.Tools
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.nio.charset.Charset
import java.util.*

class MonitoringService : Service() {

    // system services
    private var telephonyManager: TelephonyManager? = null
    private var activityManager: ActivityManager? = null
    private var connManager: ConnectivityManager? = null
    var set: Boolean? = false

    private var client: MqttPushClient? = null

    var startedSince: Long? = null

    var lastRun: Long? = null
        private set
    var lastLog: String? = null
        private set
    val lastData = NewData()
    /* the date of the last location reading */
    private var lastLocation: Long = 0

    // FIXME(pht) for testing, to compare with "last known location"
    private var networkLocation: Location? = null
    private var gpsLocation: Location? = null
    private var networkLocationListener: LocationListener? = null
    private var gpsLocationListener: LocationListener? = null
    private var objectsManager: ObjectsManager? = null

    private var phoneStateListener: PhoneStateListener? = null

    private var dbm: Int? = null


    private val lastKnownLocation: Location?
        @SuppressLint("MissingPermission")
        get() {
            val locManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

            val locationProvider = locManager.getBestProvider(Criteria(), true)
            Log.d(TAG, "Getting last known location from provider: " + locationProvider!!)

            val location: Location? = locManager.getLastKnownLocation(locationProvider)
            if (location != null) {
                Log.d(TAG, "Last known location : " + location.latitude + "," + location.longitude)
            } else {
                Log.d(TAG, "Read null location")
            }
            if (networkLocation != null) {
                Log.d(TAG, "Last Network Location : " + networkLocation!!.latitude + "," + networkLocation!!.longitude)
            } else {
                Log.d(TAG, "No known network location")
            }
            if (gpsLocation != null) {
                Log.d(TAG, "Last GPS Location : " + gpsLocation!!.latitude + "," + gpsLocation!!.longitude)
            } else {
                Log.d(TAG, "No known GPSlocation")
            }

            return location
        }

    // Service binding

    private val binder = ServiceBinder()

    // MQTT client callback

    private val mqttCallback = object : MqttCallback {

        internal inner class Message {
            var timestamp: Long = 0
            var command: Command? = null
        }

        internal inner class Command {
            var params: Map<String, String>? = null
        }

        @Throws(Exception::class)
        override fun messageArrived(topic: String, msg: MqttMessage) {
            Log.d(TAG, "MQTT msg received: " + String(msg.payload))

            // parse json payload
            val messages = Gson().fromJson(String(msg.payload, Charset.forName("UTF-8")), Array<Message>::class.java)

            // display a new notification
            @Suppress("DEPRECATION")
            val notification = Notification.Builder(this@MonitoringService.applicationContext) //
                    .setContentTitle(getText(R.string.notif_new_message)) //
                    .setContentText(messages[0].command!!.params!!["message"]) //
                    .setSmallIcon(R.drawable.ic_notif) //
                    .setAutoCancel(true) //
                    .build()

            val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mNotificationManager.notify(messages[0].timestamp.toInt(), notification)
        }

        override fun deliveryComplete(arg0: IMqttDeliveryToken) {
            //
        }

        override fun connectionLost(arg0: Throwable) {
            //
        }
    }

    override fun onCreate() {
        // Unique Identification Number for the Notification.

        Log.d(TAG, "onCreate: " + this)
        objectsManager = ObjectsManager.getInstance()

        // Display a notification icon


        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        connManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        phoneStateListener = object : PhoneStateListener() {
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                super.onSignalStrengthsChanged(signalStrength)
                if (signalStrength == null)
                    return
                if (telephonyManager!!.networkType == TelephonyManager.NETWORK_TYPE_LTE) {
                    val parts = signalStrength.toString().split(" ")
                    dbm = parts[8].toInt() - 240
                    Log.d(TAG, "Dbm is " + dbm)
                }else{
                    if (signalStrength.gsmSignalStrength != 99) {
                        dbm = -113 + 2 * signalStrength.gsmSignalStrength
                    }
                }

            }

        }

        telephonyManager!!.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)


        startedSince = System.currentTimeMillis()

    }

    fun startSendData() {
        val notif = R.string.notif_title
        // Create an intent to start the activity when clicking the notification
        val resultIntent = Intent(this, MainActivity::class.java)
        val stackBuilder = TaskStackBuilder.create(this)
        stackBuilder.addParentStack(MainActivity::class.java)
        stackBuilder.addNextIntent(resultIntent)
        val resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        @Suppress("DEPRECATION")
        val notification = Notification.Builder(this.applicationContext) //
                .setContentTitle(getText(R.string.notif_title)) //
                .setContentText(getText(R.string.notif_desc)) //
                .setSmallIcon(R.drawable.ic_notif) //
                .setOngoing(true) //
                .setContentIntent(resultPendingIntent) //
                .build()

        startForeground(notif, notification)

    }

    fun stopSendData() {

        // Cancel the persistent notification.
        stopForeground(true)
    }

    @SuppressLint("HardwareIds", "MissingPermission")
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        lastRun = System.currentTimeMillis()
        objectsManager = ObjectsManager.getInstance()
        val `object` = objectsManager!!.currentObject


        try {
            val mustConnect = intent.getBooleanExtra(CONNECT, true)

            /* First we have to create the system if it doesn't exist */

            if (this.client == null) {

                //
                // Ensure intent is valid
                //
                val deviceId = Tools.buildSerialNumber(intent.getStringExtra(DEVICE_ID), `object`!!.name!!)
                val password = intent.getStringExtra(PASSWORD)
                val serverHost = intent.getStringExtra(SERVER_HOST)

                val intentValuesList = Arrays.asList(deviceId, password, serverHost)
                if (intentValuesList.contains(null)) {
                    // Stop service when unable to start MQTT client
                    stopSelfResult(startId)
                    return Service.START_STICKY
                }

                // Now, create client
                client = MqttPushClient(deviceId, password, serverHost, mqttCallback)
            }


            if (mustConnect) {
                val location = lastKnownLocation

                // retrieve data
                val data = NewData()

                if (dbm != null) {
                    data.rssi = dbm!!
                }else {

                    @SuppressLint("MissingPermission") val cellInfos = telephonyManager!!.allCellInfo
                    if (cellInfos != null && !cellInfos.isEmpty()) {
                        val cellInfo = cellInfos[0]
                        if (cellInfo is CellInfoGsm) {
                            data.rssi = cellInfo.cellSignalStrength.dbm
                            // } else if (cellInfo instanceof CellInfoWcdma) {
                            // RSSI ?
                            // data.setRssi(((CellInfoWcdma) cellInfo).getCellSignalStrength().getDbm());
                        } else if (cellInfo is CellInfoLte) {
                            data.rsrp = cellInfo.cellSignalStrength.dbm
                        }
                    }
                }

                if (telephonyManager!!.phoneType == TelephonyManager.PHONE_TYPE_GSM) {
                    @Suppress("DEPRECATION")
                    data.imei = telephonyManager!!.deviceId
                }

                data.operator = telephonyManager!!.networkOperatorName

                when (telephonyManager!!.networkType) {
                    TelephonyManager.NETWORK_TYPE_GPRS -> data.networkType = "GPRS"
                    TelephonyManager.NETWORK_TYPE_EDGE -> data.networkType = "EDGE"
                    TelephonyManager.NETWORK_TYPE_UMTS -> data.networkType = "UMTS"
                    TelephonyManager.NETWORK_TYPE_HSDPA -> data.networkType = "HSDPA"
                    TelephonyManager.NETWORK_TYPE_HSPAP -> data.networkType = "HSPA+"
                    TelephonyManager.NETWORK_TYPE_HSPA -> data.networkType = "HSPA"
                    TelephonyManager.NETWORK_TYPE_HSUPA -> data.networkType = "HSUPA"
                    TelephonyManager.NETWORK_TYPE_LTE -> data.networkType = "LTE"
                }// to be continued


                // location
                if (location != null && location.time != lastLocation) {
                    data.latitude = location.latitude
                    data.longitude = location.longitude
                    lastLocation = location.time
                }

                // bytes sent/received
                data.bytesReceived = TrafficStats.getMobileRxBytes()
                data.bytesSent = TrafficStats.getMobileTxBytes()

                //execute action on current object datas
                objectsManager!!.execOnCurrent()
                // Custom data
                data.setCustom()


                //customDataSource.next(new Date());

                // save new data values
                if (data.extras != null) {
                    lastData.putExtras(data.extras!!)
                }

                // dispatch new data event to update the activity UI
                LocalBroadcastManager.getInstance(this).sendBroadcast(data)

                val sendDataTask = SendDataTask()
                val params = SendDataParams()
                params.client = client
                params.data = data
                params.context = this
                params.alarm = false

                sendDataTask.execute(params)


                sendDataTask.addProgressListener { result -> lastLog = result.lastLog }

                setUpLocationListeners()
            }

        } catch (e: Exception) {
            Crashlytics.logException(e)
            Log.e(TAG, "error", e)
            lastLog = "ERROR: " + e.message
            LocalBroadcastManager.getInstance(this).sendBroadcast(LogMessage(lastLog!!, false))
        }


        return Service.START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.d("MonitoringService", "Stopping service")

        if (this.client != null) {
            try {
                this.client!!.disconnect()
            } catch (e: MqttException) {
                Crashlytics.logException(e)
                Log.e(TAG, "error", e)
            }

        }

        // Cancel the persistent notification.
        stopForeground(true)

        stopLocationListeners()
    }

    @SuppressLint("MissingPermission")
    private fun setUpLocationListeners() {
        val locManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val networkLocationProvider = locManager.getProvider(LocationManager.NETWORK_PROVIDER)
        if (networkLocationProvider != null) {
            networkLocationListener = object : LocationListenerAdapter() {
                override fun onLocationChanged(location: Location) {
                    Log.d(TAG, "Received Network location update " + location.latitude + ";" + location.longitude)
                    networkLocation = location
                }
            }

            locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, (60 * 1000).toLong(), 5f, networkLocationListener)
        }
        val gpsLocationProvider = locManager.getProvider(LocationManager.GPS_PROVIDER)
        if (gpsLocationProvider != null) {
            gpsLocationListener = object : LocationListenerAdapter() {
                override fun onLocationChanged(location: Location) {
                    Log.d(TAG, "Received GPS location update " + location.latitude + ";" + location.longitude)
                    gpsLocation = location
                }
            }
            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, (60 * 1000).toLong(), 5f, gpsLocationListener)
        }
    }

    private fun stopLocationListeners() {
        val locManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (networkLocationListener != null) {
            locManager.removeUpdates(networkLocationListener)
        }
        if (gpsLocationListener != null) {
            locManager.removeUpdates(gpsLocationListener)
        }
    }

    fun sendAlarmEvent() {

        if (this.client == null) {
            Toast.makeText(applicationContext, "Alarm client is not available,wait...", Toast.LENGTH_SHORT).show()
            return
        }


        val data = NewData()
        set = !(set!!)
        data.isAlarmActivated = set

        // save alarm state
        //   if (data.getExtras() != null) {
        //       lastData.putExtras(data.getExtras());
        //   }

        val sendDataTask = SendDataTask()
        val params = SendDataParams()
        params.client = client
        params.data = data
        params.context = this
        params.alarm = true
        params.value = set!!

        sendDataTask.execute(params)


        sendDataTask.addProgressListener { result -> lastLog = result.lastLog }

    }

    override fun onBind(arg0: Intent): IBinder? {
        return binder
    }

    inner class ServiceBinder : Binder() {

        val service: MonitoringService
            get() = this@MonitoringService
    }

    companion object {
        private const val TAG = "MonitoringService"


        // Intent extra keys
        const val DEVICE_ID = "device_id"
        const val SERVER_HOST = "server_host"
        const val PASSWORD = "password"
        const val CONNECT = "connect"
        const val OBJECT_NAME = "objname"
    }

}
